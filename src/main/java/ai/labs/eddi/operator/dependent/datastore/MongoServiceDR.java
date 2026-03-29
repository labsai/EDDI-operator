package ai.labs.eddi.operator.dependent.datastore;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

/**
 * Headless Service for the managed MongoDB StatefulSet.
 */
@KubernetesDependent
public class MongoServiceDR extends CRUDKubernetesDependentResource<Service, EddiResource> {

    public MongoServiceDR() {
        super(Service.class);
    }

    @Override
    protected Service desired(EddiResource eddi, Context<EddiResource> context) {
        return new ServiceBuilder()
                .withNewMetadata()
                    .withName(Labels.resourceName(eddi, "mongodb"))
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "mongodb"))
                .endMetadata()
                .withNewSpec()
                    .withType("ClusterIP")
                    .withClusterIP("None") // Headless for StatefulSet
                    .withSelector(Labels.selector(eddi, "mongodb"))
                    .withPorts(
                            new ServicePortBuilder()
                                    .withName("mongodb")
                                    .withPort(27017)
                                    .withTargetPort(new IntOrString(27017))
                                    .build()
                    )
                .endSpec()
                .build();
    }
}
