package ai.labs.eddi.operator.crd.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Network exposure configuration — Route, Ingress, or auto-detect.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExposureSpec {

    private ExposureType type = ExposureType.AUTO;
    private String host = "";

    private TlsSpec tls = new TlsSpec();

    private Map<String, String> annotations = new HashMap<>();
    private String ingressClassName = "";

    public ExposureType getType() {
        return type;
    }

    public void setType(ExposureType type) {
        this.type = type;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public TlsSpec getTls() {
        return tls;
    }

    public void setTls(TlsSpec tls) {
        this.tls = tls;
    }

    public Map<String, String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Map<String, String> annotations) {
        this.annotations = annotations;
    }

    public String getIngressClassName() {
        return ingressClassName;
    }

    public void setIngressClassName(String ingressClassName) {
        this.ingressClassName = ingressClassName;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TlsSpec {
        private boolean enabled = true;
        private String secretRef = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSecretRef() {
            return secretRef;
        }

        public void setSecretRef(String secretRef) {
            this.secretRef = secretRef;
        }
    }
}
