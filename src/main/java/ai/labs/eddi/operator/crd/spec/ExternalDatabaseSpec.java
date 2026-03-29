package ai.labs.eddi.operator.crd.spec;

/**
 * Configuration for external (pre-existing) database connections.
 */
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
