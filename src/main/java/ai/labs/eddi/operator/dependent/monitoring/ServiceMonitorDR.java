package ai.labs.eddi.operator.dependent.monitoring;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages a Prometheus ServiceMonitor that scrapes EDDI's /q/metrics endpoint.
 * Activated when spec.monitoring.serviceMonitor.enabled=true.
 */
@KubernetesDependent
public class ServiceMonitorDR extends CRUDKubernetesDependentResource<GenericKubernetesResource, EddiResource> {

    public ServiceMonitorDR() {
        super(GenericKubernetesResource.class);
    }

    @Override
    protected GenericKubernetesResource desired(EddiResource eddi, Context<EddiResource> context) {
        var name = Labels.resourceName(eddi, "monitor");
        var monitorSpec = eddi.getSpec().getMonitoring().getServiceMonitor();

        var selectorLabels = Labels.selector(eddi, "server");

        // Merge extra labels with standard labels for ServiceMonitor itself
        var labels = new LinkedHashMap<>(Labels.standard(eddi, "monitoring"));
        labels.putAll(monitorSpec.getLabels());

        var spec = new LinkedHashMap<String, Object>();
        spec.put("selector", Map.of("matchLabels", selectorLabels));
        spec.put("endpoints", List.of(
                Map.of(
                        "port", "http",
                        "path", "/q/metrics",
                        "interval", monitorSpec.getInterval()
                )
        ));

        return new GenericKubernetesResourceBuilder()
                .withApiVersion("monitoring.coreos.com/v1")
                .withKind("ServiceMonitor")
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(labels)
                .endMetadata()
                .withAdditionalProperties(Map.of("spec", spec))
                .build();
    }
}
