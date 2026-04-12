package ai.labs.eddi.operator.crd.spec;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Supported datastore backends.
 * Emits JSON Schema enum constraint in the generated CRD for server-side validation.
 */
public enum DatastoreType {

    MONGODB("mongodb"),
    POSTGRES("postgres");

    private final String value;

    DatastoreType(String value) {
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
