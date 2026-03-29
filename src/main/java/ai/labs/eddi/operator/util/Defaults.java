package ai.labs.eddi.operator.util;

import ai.labs.eddi.operator.crd.EddiSpec;

/**
 * Provides sensible defaults for unset fields in the EddiSpec.
 */
public final class Defaults {

    private Defaults() {
        // utility class
    }

    /**
     * Resolves the EDDI container image tag.
     * If spec.image.tag is empty, defaults to spec.version.
     */
    public static String resolveEddiImageTag(EddiSpec spec) {
        var tag = spec.getImage().getTag();
        return (tag == null || tag.isBlank()) ? spec.getVersion() : tag;
    }

    /**
     * Returns the full EDDI container image reference.
     */
    public static String resolveEddiImage(EddiSpec spec) {
        return spec.getImage().getRepository() + ":" + resolveEddiImageTag(spec);
    }

    /**
     * Determines whether managed MongoDB should be deployed.
     */
    public static boolean isManagedMongodb(EddiSpec spec) {
        return "mongodb".equals(spec.getDatastore().getType())
                && spec.getDatastore().getManaged().isEnabled();
    }

    /**
     * Determines whether managed PostgreSQL should be deployed.
     */
    public static boolean isManagedPostgres(EddiSpec spec) {
        return "postgres".equals(spec.getDatastore().getType())
                && spec.getDatastore().getManaged().isEnabled();
    }

    /**
     * Determines whether managed NATS should be deployed.
     */
    public static boolean isManagedNats(EddiSpec spec) {
        return "nats".equals(spec.getMessaging().getType())
                && spec.getMessaging().getManaged().isEnabled();
    }

    /**
     * Determines whether managed Keycloak should be deployed.
     */
    public static boolean isManagedAuth(EddiSpec spec) {
        return spec.getAuth().isEnabled()
                && spec.getAuth().getManaged().isEnabled();
    }

    /**
     * Returns the MongoDB connection string for the managed instance.
     */
    public static String managedMongoConnectionString(String name) {
        return "mongodb://" + name + "-mongodb:27017/eddi";
    }

    /**
     * Returns the NATS URL for the managed instance.
     */
    public static String managedNatsUrl(String name) {
        return "nats://" + name + "-nats:4222";
    }

    /**
     * Returns the PostgreSQL JDBC URL for the managed instance.
     */
    public static String managedPostgresJdbcUrl(String name) {
        return "jdbc:postgresql://" + name + "-postgres:5432/eddi";
    }
}
