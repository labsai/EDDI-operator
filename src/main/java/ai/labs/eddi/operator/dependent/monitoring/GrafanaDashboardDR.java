package ai.labs.eddi.operator.dependent.monitoring;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Manages a ConfigMap containing the Grafana dashboard JSON.
 * The Grafana Operator picks this up via the grafana_dashboard label.
 * Activated when spec.monitoring.grafanaDashboard.enabled=true.
 */
@KubernetesDependent
public class GrafanaDashboardDR extends CRUDKubernetesDependentResource<ConfigMap, EddiResource> {

    private static final String DASHBOARD_RESOURCE = "dashboards/eddi-overview.json";

    public GrafanaDashboardDR() {
        super(ConfigMap.class);
    }

    @Override
    protected ConfigMap desired(EddiResource eddi, Context<EddiResource> context) {
        var name = Labels.resourceName(eddi, "grafana-dashboard");

        var labels = Labels.standard(eddi, "grafana-dashboard");
        labels.put("grafana_dashboard", "1"); // Grafana sidecar auto-discovery

        var dashboardJson = loadDashboardJson();

        return new ConfigMapBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(labels)
                .endMetadata()
                .withData(Map.of("eddi-overview.json", dashboardJson))
                .build();
    }

    private String loadDashboardJson() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(DASHBOARD_RESOURCE)) {
            if (is == null) {
                return "{}"; // Fallback if dashboard JSON is not yet created
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "{}";
        }
    }
}
