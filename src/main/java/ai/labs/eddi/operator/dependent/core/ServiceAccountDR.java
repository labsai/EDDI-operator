package ai.labs.eddi.operator.dependent.core;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

/**
 * Manages the ServiceAccount for the EDDI server pods.
 */
@KubernetesDependent
public class ServiceAccountDR extends CRUDKubernetesDependentResource<ServiceAccount, EddiResource> {

    public ServiceAccountDR() {
        super(ServiceAccount.class);
    }

    @Override
    protected ServiceAccount desired(EddiResource eddi, Context<EddiResource> context) {
        return new ServiceAccountBuilder()
                .withNewMetadata()
                    .withName(Labels.resourceName(eddi, "server"))
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "server"))
                .endMetadata()
                .build();
    }
}
