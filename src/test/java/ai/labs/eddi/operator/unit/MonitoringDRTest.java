package ai.labs.eddi.operator.unit;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.dependent.monitoring.GrafanaDashboardDR;
import ai.labs.eddi.operator.dependent.monitoring.PrometheusRuleDR;
import ai.labs.eddi.operator.dependent.monitoring.ServiceMonitorDR;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static ai.labs.eddi.operator.unit.TestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for monitoring dependent resources:
 * ServiceMonitorDR, GrafanaDashboardDR, PrometheusRuleDR.
 */
class MonitoringDRTest {

    private EddiResource eddi;

    @BeforeEach
    void setUp() {
        eddi = createEddi();
    }

    // ────────────────────────────────────────────────────
    //  ServiceMonitorDR
    // ────────────────────────────────────────────────────

    @Nested
    class ServiceMonitor {

        @Test
        void shouldHaveCorrectApiVersionAndKind() {
            var sm = callDesired(new ServiceMonitorDR(), eddi);
            assertThat(sm.getApiVersion()).isEqualTo("monitoring.coreos.com/v1");
            assertThat(sm.getKind()).isEqualTo("ServiceMonitor");
        }

        @Test
        void shouldHaveCorrectName() {
            var sm = callDesired(new ServiceMonitorDR(), eddi);
            assertThat(sm.getMetadata().getName()).isEqualTo(CR_NAME + "-monitor");
        }

        @Test
        @SuppressWarnings("unchecked")
        void shouldScrapeMetricsEndpoint() {
            var sm = callDesired(new ServiceMonitorDR(), eddi);
            var spec = (Map<String, Object>) sm.getAdditionalProperties().get("spec");
            var endpoints = (List<Map<String, Object>>) spec.get("endpoints");
            assertThat(endpoints).hasSize(1);
            assertThat(endpoints.get(0).get("path")).isEqualTo("/q/metrics");
            assertThat(endpoints.get(0).get("port")).isEqualTo("http");
        }

        @Test
        @SuppressWarnings("unchecked")
        void shouldSelectServerPods() {
            var sm = callDesired(new ServiceMonitorDR(), eddi);
            var spec = (Map<String, Object>) sm.getAdditionalProperties().get("spec");
            var selector = (Map<String, Object>) spec.get("selector");
            var matchLabels = (Map<String, String>) selector.get("matchLabels");
            assertThat(matchLabels)
                    .containsEntry("app.kubernetes.io/component", "server")
                    .containsEntry("app.kubernetes.io/instance", CR_NAME);
        }
    }

    // ────────────────────────────────────────────────────
    //  GrafanaDashboardDR
    // ────────────────────────────────────────────────────

    @Nested
    class GrafanaDashboard {

        @Test
        void shouldHaveCorrectName() {
            var cm = callDesired(new GrafanaDashboardDR(), eddi);
            assertThat(cm.getMetadata().getName()).isEqualTo(CR_NAME + "-grafana-dashboard");
        }

        @Test
        void shouldHaveGrafanaSidecarLabel() {
            var cm = callDesired(new GrafanaDashboardDR(), eddi);
            assertThat(cm.getMetadata().getLabels())
                    .containsEntry("grafana_dashboard", "1");
        }

        @Test
        void shouldContainDashboardJsonKey() {
            var cm = callDesired(new GrafanaDashboardDR(), eddi);
            assertThat(cm.getData()).containsKey("eddi-overview.json");
        }

        @Test
        void shouldHaveStandardLabels() {
            var cm = callDesired(new GrafanaDashboardDR(), eddi);
            assertThat(cm.getMetadata().getLabels())
                    .containsEntry("app.kubernetes.io/component", "grafana-dashboard")
                    .containsEntry("app.kubernetes.io/managed-by", "eddi-operator");
        }
    }

    // ────────────────────────────────────────────────────
    //  PrometheusRuleDR
    // ────────────────────────────────────────────────────

    @Nested
    class PrometheusRule {

        @Test
        void shouldHaveCorrectApiVersionAndKind() {
            var rule = callDesired(new PrometheusRuleDR(), eddi);
            assertThat(rule.getApiVersion()).isEqualTo("monitoring.coreos.com/v1");
            assertThat(rule.getKind()).isEqualTo("PrometheusRule");
        }

        @Test
        void shouldHaveCorrectName() {
            var rule = callDesired(new PrometheusRuleDR(), eddi);
            assertThat(rule.getMetadata().getName()).isEqualTo(CR_NAME + "-alerts");
        }

        @Test
        @SuppressWarnings("unchecked")
        void shouldContainThreeAlertRules() {
            var rule = callDesired(new PrometheusRuleDR(), eddi);
            var spec = (Map<String, Object>) rule.getAdditionalProperties().get("spec");
            var groups = (List<Map<String, Object>>) spec.get("groups");
            assertThat(groups).hasSize(1);
            assertThat(groups.get(0).get("name")).isEqualTo("eddi.rules");
            var rules = (List<Map<String, Object>>) groups.get(0).get("rules");
            assertThat(rules).hasSize(3);
        }

        @Test
        @SuppressWarnings("unchecked")
        void shouldReferenceCorrectJobNameInPromQL() {
            var rule = callDesired(new PrometheusRuleDR(), eddi);
            var spec = (Map<String, Object>) rule.getAdditionalProperties().get("spec");
            var groups = (List<Map<String, Object>>) spec.get("groups");
            var rules = (List<Map<String, Object>>) groups.get(0).get("rules");

            // EddiDown rule should reference "my-eddi-server"
            var eddiDown = rules.stream()
                    .filter(r -> "EddiDown".equals(r.get("alert")))
                    .findFirst().orElseThrow();
            assertThat((String) eddiDown.get("expr")).contains(CR_NAME + "-server");
        }

        @Test
        @SuppressWarnings("unchecked")
        void shouldHaveCorrectAlertNames() {
            var rule = callDesired(new PrometheusRuleDR(), eddi);
            var spec = (Map<String, Object>) rule.getAdditionalProperties().get("spec");
            var groups = (List<Map<String, Object>>) spec.get("groups");
            var rules = (List<Map<String, Object>>) groups.get(0).get("rules");

            var alertNames = rules.stream().map(r -> (String) r.get("alert")).toList();
            assertThat(alertNames).containsExactly("EddiDown", "EddiHighErrorRate", "EddiHighLatency");
        }
    }
}
