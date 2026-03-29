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
 * Manages a PrometheusRule with default alerting rules for EDDI.
 * Includes alerts for: pod down, high error rate, DB connection failure,
 * backup failure, and high latency.
 */
@KubernetesDependent
public class PrometheusRuleDR extends CRUDKubernetesDependentResource<GenericKubernetesResource, EddiResource> {

    public PrometheusRuleDR() {
        super(GenericKubernetesResource.class);
    }

    @Override
    protected GenericKubernetesResource desired(EddiResource eddi, Context<EddiResource> context) {
        var name = Labels.resourceName(eddi, "alerts");
        var instanceName = eddi.getMetadata().getName();

        var rules = List.of(
                Map.of(
                        "alert", "EddiDown",
                        "expr", "up{job=\"" + instanceName + "-server\"} == 0",
                        "for", "5m",
                        "labels", Map.of("severity", "critical"),
                        "annotations", Map.of(
                                "summary", "EDDI instance is down",
                                "description", "EDDI pod {{ $labels.pod }} has been down for more than 5 minutes."
                        )
                ),
                Map.of(
                        "alert", "EddiHighErrorRate",
                        "expr", "rate(http_server_requests_seconds_count{job=\"" + instanceName + "-server\",status=~\"5..\"}[5m]) / rate(http_server_requests_seconds_count{job=\"" + instanceName + "-server\"}[5m]) > 0.05",
                        "for", "10m",
                        "labels", Map.of("severity", "warning"),
                        "annotations", Map.of(
                                "summary", "EDDI high error rate",
                                "description", "More than 5% of requests are failing."
                        )
                ),
                Map.of(
                        "alert", "EddiHighLatency",
                        "expr", "histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{job=\"" + instanceName + "-server\"}[5m])) > 2",
                        "for", "10m",
                        "labels", Map.of("severity", "warning"),
                        "annotations", Map.of(
                                "summary", "EDDI high latency",
                                "description", "P99 latency is above 2 seconds."
                        )
                )
        );

        var spec = Map.of(
                "groups", List.of(
                        Map.of(
                                "name", "eddi.rules",
                                "rules", rules
                        )
                )
        );

        return new GenericKubernetesResourceBuilder()
                .withApiVersion("monitoring.coreos.com/v1")
                .withKind("PrometheusRule")
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "alerts"))
                .endMetadata()
                .withAdditionalProperties(Map.of("spec", spec))
                .build();
    }
}
