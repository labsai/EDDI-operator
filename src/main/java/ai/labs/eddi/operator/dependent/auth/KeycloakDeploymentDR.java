package ai.labs.eddi.operator.dependent.auth;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages a Keycloak Deployment for authentication.
 * Server-only — realm configuration is left to the user.
 * Activated when spec.auth.enabled=true and spec.auth.managed.enabled=true.
 */
@KubernetesDependent
public class KeycloakDeploymentDR extends CRUDKubernetesDependentResource<Deployment, EddiResource> {

    public KeycloakDeploymentDR() {
        super(Deployment.class);
    }

    @Override
    protected Deployment desired(EddiResource eddi, Context<EddiResource> context) {
        var spec = eddi.getSpec();
        var authManaged = spec.getAuth().getManaged();
        var name = Labels.resourceName(eddi, "keycloak");

        var env = new ArrayList<EnvVar>();
        env.add(new EnvVarBuilder().withName("KEYCLOAK_ADMIN").withValue("admin").build());

        // Admin password from secret if provided
        var adminSecretRef = authManaged.getAdminSecretRef();
        if (adminSecretRef != null && !adminSecretRef.isBlank()) {
            env.add(new EnvVarBuilder()
                    .withName("KEYCLOAK_ADMIN_PASSWORD")
                    .withNewValueFrom()
                        .withNewSecretKeyRef()
                            .withName(adminSecretRef)
                            .withKey("password")
                        .endSecretKeyRef()
                    .endValueFrom()
                    .build());
        } else {
            env.add(new EnvVarBuilder()
                    .withName("KEYCLOAK_ADMIN_PASSWORD")
                    .withValue("admin")
                    .build());
        }

        var resources = new ResourceRequirementsBuilder()
                .withRequests(Map.of(
                        "cpu", new Quantity(authManaged.getResources().getRequests().getCpu()),
                        "memory", new Quantity(authManaged.getResources().getRequests().getMemory())
                ))
                .withLimits(Map.of(
                        "cpu", new Quantity(authManaged.getResources().getLimits().getCpu()),
                        "memory", new Quantity(authManaged.getResources().getLimits().getMemory())
                ))
                .build();

        return new DeploymentBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "keycloak"))
                .endMetadata()
                .withNewSpec()
                    .withReplicas(1)
                    .withNewSelector()
                        .withMatchLabels(Labels.selector(eddi, "keycloak"))
                    .endSelector()
                    .withNewTemplate()
                        .withNewMetadata()
                            .withLabels(Labels.standard(eddi, "keycloak"))
                        .endMetadata()
                        .withNewSpec()
                            .addNewContainer()
                                .withName("keycloak")
                                .withImage("quay.io/keycloak/keycloak:24.0")
                                .withArgs(List.of(
                                        "production".equals(authManaged.getMode()) ? "start" : "start-dev"
                                ))
                                .withPorts(List.of(
                                        new ContainerPortBuilder()
                                                .withName("http")
                                                .withContainerPort(8080)
                                                .build()
                                ))
                                .withEnv(env)
                                .withResources(resources)
                                .withNewReadinessProbe()
                                    .withNewHttpGet()
                                        .withPath("/health/ready")
                                        .withPort(new IntOrString(8080))
                                    .endHttpGet()
                                    .withInitialDelaySeconds(30)
                                    .withPeriodSeconds(10)
                                .endReadinessProbe()
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();
    }
}
