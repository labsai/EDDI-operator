package ai.labs.eddi.operator.dependent.auth;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

/**
 * ClusterIP Service for the managed Keycloak Deployment.
 */
@KubernetesDependent
public class KeycloakServiceDR extends CRUDKubernetesDependentResource<Service, EddiResource> {

    public KeycloakServiceDR() {
        super(Service.class);
    }

    @Override
    protected Service desired(EddiResource eddi, Context<EddiResource> context) {
        return new ServiceBuilder()
                .withNewMetadata()
                    .withName(Labels.resourceName(eddi, "keycloak"))
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "keycloak"))
                .endMetadata()
                .withNewSpec()
                    .withType("ClusterIP")
                    .withSelector(Labels.selector(eddi, "keycloak"))
                    .withPorts(
                            new ServicePortBuilder()
                                    .withName("http")
                                    .withPort(8080)
                                    .withTargetPort(new IntOrString(8080))
                                    .build()
                    )
                .endSpec()
                .build();
    }
}
