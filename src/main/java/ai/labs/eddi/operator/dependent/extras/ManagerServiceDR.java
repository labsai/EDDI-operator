package ai.labs.eddi.operator.dependent.extras;

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
 * ClusterIP Service for the EDDI Manager UI.
 */
@KubernetesDependent
public class ManagerServiceDR extends CRUDKubernetesDependentResource<Service, EddiResource> {

    public ManagerServiceDR() {
        super(Service.class);
    }

    @Override
    protected Service desired(EddiResource eddi, Context<EddiResource> context) {
        return new ServiceBuilder()
                .withNewMetadata()
                    .withName(Labels.resourceName(eddi, "manager"))
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "manager"))
                .endMetadata()
                .withNewSpec()
                    .withType("ClusterIP")
                    .withSelector(Labels.selector(eddi, "manager"))
                    .withPorts(
                            new ServicePortBuilder()
                                    .withName("http")
                                    .withPort(3000)
                                    .withTargetPort(new IntOrString(3000))
                                    .build()
                    )
                .endSpec()
                .build();
    }
}
