package ai.labs.eddi.operator.crd.spec;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Supported messaging backends.
 * Emits JSON Schema enum constraint in the generated CRD for server-side validation.
 */
public enum MessagingType {

    IN_MEMORY("in-memory"),
    NATS("nats");

    private final String value;

    MessagingType(String value) {
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
