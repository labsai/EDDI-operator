package ai.labs.eddi.operator.crd.spec;

/**
 * Datastore configuration — supports MongoDB or PostgreSQL,
 * each in managed (operator-deployed) or external (pre-existing) mode.
 */
public class DatastoreSpec {

    private String type = "mongodb"; // "mongodb" | "postgres"
    private ManagedDatabaseSpec managed = new ManagedDatabaseSpec();
    private ExternalDatabaseSpec external = new ExternalDatabaseSpec();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ManagedDatabaseSpec getManaged() {
        return managed;
    }

    public void setManaged(ManagedDatabaseSpec managed) {
        this.managed = managed;
    }

    public ExternalDatabaseSpec getExternal() {
        return external;
    }

    public void setExternal(ExternalDatabaseSpec external) {
        this.external = external;
    }
}
