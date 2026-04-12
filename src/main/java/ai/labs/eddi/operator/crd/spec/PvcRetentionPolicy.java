package ai.labs.eddi.operator.crd.spec;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * PVC cleanup policy on CR deletion.
 * RETAIN leaves PVCs intact (safe default), DELETE removes orphaned StatefulSet PVCs.
 */
public enum PvcRetentionPolicy {

    RETAIN("Retain"),
    DELETE("Delete");

    private final String value;

    PvcRetentionPolicy(String value) {
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
