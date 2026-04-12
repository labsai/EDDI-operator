package ai.labs.eddi.operator.crd.spec;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Observability configuration — ServiceMonitor, GrafanaDashboard, PrometheusRule.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MonitoringSpec {

    private ServiceMonitorSpec serviceMonitor = new ServiceMonitorSpec();
    private GrafanaDashboardSpec grafanaDashboard = new GrafanaDashboardSpec();
    private AlertsSpec alerts = new AlertsSpec();

    public ServiceMonitorSpec getServiceMonitor() {
        return serviceMonitor;
    }

    public void setServiceMonitor(ServiceMonitorSpec serviceMonitor) {
        this.serviceMonitor = serviceMonitor;
    }

    public GrafanaDashboardSpec getGrafanaDashboard() {
        return grafanaDashboard;
    }

    public void setGrafanaDashboard(GrafanaDashboardSpec grafanaDashboard) {
        this.grafanaDashboard = grafanaDashboard;
    }

    public AlertsSpec getAlerts() {
        return alerts;
    }

    public void setAlerts(AlertsSpec alerts) {
        this.alerts = alerts;
    }

    public static class ServiceMonitorSpec {
        private boolean enabled = false;
        private String interval = "30s";
        private Map<String, String> labels = new HashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getInterval() {
            return interval;
        }

        public void setInterval(String interval) {
            this.interval = interval;
        }

        public Map<String, String> getLabels() {
            return labels;
        }

        public void setLabels(Map<String, String> labels) {
            this.labels = labels;
        }
    }

    public static class GrafanaDashboardSpec {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class AlertsSpec {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
