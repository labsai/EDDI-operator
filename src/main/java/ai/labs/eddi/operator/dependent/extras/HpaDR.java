package ai.labs.eddi.operator.dependent.extras;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.autoscaling.v2.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.autoscaling.v2.HorizontalPodAutoscalerBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

/**
 * Manages the HorizontalPodAutoscaler for the EDDI Deployment.
 * Activated when spec.autoscaling.enabled=true.
 */
@KubernetesDependent
public class HpaDR extends CRUDKubernetesDependentResource<HorizontalPodAutoscaler, EddiResource> {

    public HpaDR() {
        super(HorizontalPodAutoscaler.class);
    }

    @Override
    protected HorizontalPodAutoscaler desired(EddiResource eddi, Context<EddiResource> context) {
        var spec = eddi.getSpec();
        var as = spec.getAutoscaling();
        var name = Labels.resourceName(eddi, "hpa");
        var targetName = Labels.resourceName(eddi, "server");

        return new HorizontalPodAutoscalerBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "hpa"))
                .endMetadata()
                .withNewSpec()
                    .withNewScaleTargetRef()
                        .withApiVersion("apps/v1")
                        .withKind("Deployment")
                        .withName(targetName)
                    .endScaleTargetRef()
                    .withMinReplicas(as.getMinReplicas())
                    .withMaxReplicas(as.getMaxReplicas())
                    .addNewMetric()
                        .withType("Resource")
                        .withNewResource()
                            .withName("cpu")
                            .withNewTarget()
                                .withType("Utilization")
                                .withAverageUtilization(as.getTargetCPU())
                            .endTarget()
                        .endResource()
                    .endMetric()
                    .addNewMetric()
                        .withType("Resource")
                        .withNewResource()
                            .withName("memory")
                            .withNewTarget()
                                .withType("Utilization")
                                .withAverageUtilization(as.getTargetMemory())
                            .endTarget()
                        .endResource()
                    .endMetric()
                .endSpec()
                .build();
    }
}
