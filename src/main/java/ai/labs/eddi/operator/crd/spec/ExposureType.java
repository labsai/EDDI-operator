package ai.labs.eddi.operator.crd.spec;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Network exposure modes.
 * AUTO detects OpenShift (Route) vs vanilla K8s (Ingress) at runtime.
 */
public enum ExposureType {

    AUTO("auto"),
    ROUTE("route"),
    INGRESS("ingress"),
    NONE("none");

    private final String value;

    ExposureType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
