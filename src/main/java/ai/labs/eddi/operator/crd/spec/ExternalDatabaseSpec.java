package ai.labs.eddi.operator.crd.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Configuration for external (pre-existing) database connections.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalDatabaseSpec {

    private String connectionString = "";
    private String secretRef = "";

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public String getSecretRef() {
        return secretRef;
    }

    public void setSecretRef(String secretRef) {
        this.secretRef = secretRef;
    }
}
