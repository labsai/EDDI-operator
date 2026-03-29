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
 * Headless Service for the managed PostgreSQL StatefulSet.
 */
@KubernetesDependent
public class PostgresServiceDR extends CRUDKubernetesDependentResource<Service, EddiResource> {

    public PostgresServiceDR() {
        super(Service.class);
    }

    @Override
    protected Service desired(EddiResource eddi, Context<EddiResource> context) {
        return new ServiceBuilder()
                .withNewMetadata()
                    .withName(Labels.resourceName(eddi, "postgres"))
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "postgres"))
                .endMetadata()
                .withNewSpec()
                    .withType("ClusterIP")
                    .withClusterIP("None")
                    .withSelector(Labels.selector(eddi, "postgres"))
                    .withPorts(
                            new ServicePortBuilder()
                                    .withName("postgres")
                                    .withPort(5432)
                                    .withTargetPort(new IntOrString(5432))
                                    .build()
                    )
                .endSpec()
                .build();
    }
}
