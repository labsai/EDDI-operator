package ai.labs.eddi.operator.crd.spec;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Keycloak deployment mode.
 * DEV uses start-dev (ephemeral storage, HTTP), PRODUCTION uses start (TLS, requires admin secret).
 */
public enum KeycloakMode {

    DEV("dev"),
    PRODUCTION("production");

    private final String value;

    KeycloakMode(String value) {
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
