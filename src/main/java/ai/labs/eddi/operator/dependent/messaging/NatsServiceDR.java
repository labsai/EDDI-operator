package ai.labs.eddi.operator.dependent.messaging;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.List;

/**
 * Headless Service for the managed NATS StatefulSet.
 */
@KubernetesDependent
public class NatsServiceDR extends CRUDKubernetesDependentResource<Service, EddiResource> {

    public NatsServiceDR() {
        super(Service.class);
    }

    @Override
    protected Service desired(EddiResource eddi, Context<EddiResource> context) {
        return new ServiceBuilder()
                .withNewMetadata()
                    .withName(Labels.resourceName(eddi, "nats"))
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "nats"))
                .endMetadata()
                .withNewSpec()
                    .withType("ClusterIP")
                    .withClusterIP("None")
                    .withSelector(Labels.selector(eddi, "nats"))
                    .withPorts(List.of(
                            new ServicePortBuilder()
                                    .withName("client")
                                    .withPort(4222)
                                    .withTargetPort(new IntOrString(4222))
                                    .build(),
                            new ServicePortBuilder()
                                    .withName("monitor")
                                    .withPort(8222)
                                    .withTargetPort(new IntOrString(8222))
                                    .build()
                    ))
                .endSpec()
                .build();
    }
}
