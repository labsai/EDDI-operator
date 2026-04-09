package ai.labs.eddi.operator.util;

import ai.labs.eddi.operator.crd.EddiSpec;
import ai.labs.eddi.operator.crd.spec.ResourcesSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.SecurityContext;
import io.fabric8.kubernetes.api.model.SecurityContextBuilder;

import java.util.List;
import java.util.Map;

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

    /**
     * Converts a ResourcesSpec to Kubernetes ResourceRequirements.
     * Centralizes the pattern duplicated across all DRs.
     */
    public static ResourceRequirements buildResources(ResourcesSpec spec) {
        return new ResourceRequirementsBuilder()
                .withRequests(Map.of(
                        "cpu", new Quantity(spec.getRequests().getCpu()),
                        "memory", new Quantity(spec.getRequests().getMemory())
                ))
                .withLimits(Map.of(
                        "cpu", new Quantity(spec.getLimits().getCpu()),
                        "memory", new Quantity(spec.getLimits().getMemory())
                ))
                .build();
    }

    /**
     * Resolves a container image reference from repository + tag.
     *
     * @param repository the image repository
     * @param tag        the image tag (used as-is, must not be blank)
     * @return full image reference, e.g. "mongo:7.0"
     */
    public static String resolveImage(String repository, String tag) {
        if (tag == null || tag.isBlank()) {
            return repository;
        }
        // If repository already contains ':', it's a full reference
        if (repository.contains(":")) {
            return repository;
        }
        return repository + ":" + tag;
    }

    /**
     * Returns a Pod Security Standards 'restricted' profile SecurityContext.
     * Required for Red Hat certification and enterprise PSA enforcement.
     * <ul>
     *   <li>runAsNonRoot = true</li>
     *   <li>allowPrivilegeEscalation = false</li>
     *   <li>capabilities.drop = [ALL]</li>
     *   <li>seccompProfile = RuntimeDefault</li>
     * </ul>
     */
    public static SecurityContext restrictedSecurityContext() {
        return new SecurityContextBuilder()
                .withRunAsNonRoot(true)
                .withAllowPrivilegeEscalation(false)
                .withNewCapabilities()
                    .withDrop(List.of("ALL"))
                .endCapabilities()
                .withNewSeccompProfile()
                    .withType("RuntimeDefault")
                .endSeccompProfile()
                .build();
    }
}
