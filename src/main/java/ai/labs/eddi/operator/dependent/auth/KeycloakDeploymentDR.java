package ai.labs.eddi.operator.dependent.auth;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Defaults;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.ArrayList;
import java.util.List;

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

        // Resolve image from spec
        var imgSpec = authManaged.getImage();
        var image = Defaults.resolveImage(imgSpec.getRepository(), imgSpec.getTag());

        // Resolve admin credential secret name
        var adminSecretRef = authManaged.getAdminSecretRef();
        var credentialSecretName = (adminSecretRef != null && !adminSecretRef.isBlank())
                ? adminSecretRef
                : Labels.resourceName(eddi, "keycloak-admin");

        var env = new ArrayList<EnvVar>();
        // Admin username from secret
        env.add(new EnvVarBuilder()
                .withName("KEYCLOAK_ADMIN")
                .withNewValueFrom()
                    .withNewSecretKeyRef()
                        .withName(credentialSecretName)
                        .withKey("username")
                    .endSecretKeyRef()
                .endValueFrom()
                .build());
        // Admin password from secret
        env.add(new EnvVarBuilder()
                .withName("KEYCLOAK_ADMIN_PASSWORD")
                .withNewValueFrom()
                    .withNewSecretKeyRef()
                        .withName(credentialSecretName)
                        .withKey("password")
                    .endSecretKeyRef()
                .endValueFrom()
                .build());

        var resources = Defaults.buildResources(authManaged.getResources());

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
                                .withImage(image)
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
                                .withSecurityContext(Defaults.restrictedSecurityContext())
                                .withNewReadinessProbe()
                                    .withNewHttpGet()
                                        .withPath("/health/ready")
                                        .withPort(new IntOrString(8080))
                                    .endHttpGet()
                                    .withInitialDelaySeconds(30)
                                    .withPeriodSeconds(10)
                                .endReadinessProbe()
                                .withNewLivenessProbe()
                                    .withNewHttpGet()
                                        .withPath("/health/live")
                                        .withPort(new IntOrString(8080))
                                    .endHttpGet()
                                    .withInitialDelaySeconds(60)
                                    .withPeriodSeconds(15)
                                    .withFailureThreshold(3)
                                .endLivenessProbe()
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();
    }
}
