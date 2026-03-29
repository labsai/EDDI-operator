package ai.labs.eddi.operator.dependent.core;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

/**
 * Manages the ClusterIP Service for the EDDI server.
 */
@KubernetesDependent
public class EddiServiceDR extends CRUDKubernetesDependentResource<Service, EddiResource> {

    public EddiServiceDR() {
        super(Service.class);
    }

    @Override
    protected Service desired(EddiResource eddi, Context<EddiResource> context) {
        return new ServiceBuilder()
                .withNewMetadata()
                    .withName(Labels.resourceName(eddi, "server"))
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "server"))
                .endMetadata()
                .withNewSpec()
                    .withType("ClusterIP")
                    .withSelector(Labels.selector(eddi, "server"))
                    .withPorts(
                            new ServicePortBuilder()
                                    .withName("http")
                                    .withPort(8080)
                                    .withTargetPort(new io.fabric8.kubernetes.api.model.IntOrString(8080))
                                    .withProtocol("TCP")
                                    .build()
                    )
                .endSpec()
                .build();
    }
}
