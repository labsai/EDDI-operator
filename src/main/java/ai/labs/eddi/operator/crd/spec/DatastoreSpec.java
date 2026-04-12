package ai.labs.eddi.operator.crd.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Datastore configuration — supports MongoDB or PostgreSQL,
 * each in managed (operator-deployed) or external (pre-existing) mode.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatastoreSpec {

    private DatastoreType type = DatastoreType.MONGODB;
    private ManagedDatabaseSpec managed = new ManagedDatabaseSpec();
    private ExternalDatabaseSpec external = new ExternalDatabaseSpec();

    public DatastoreType getType() {
        return type;
    }

    public void setType(DatastoreType type) {
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
