package ai.labs.eddi.operator.unit;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.EddiSpec;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

import java.lang.reflect.Method;

/**
 * Shared test utilities for operator unit tests.
 * Provides reflection-based access to protected {@code desired()} methods
 * and common factory methods for EddiResource instances.
 */
final class TestSupport {

    static final String CR_NAME = "my-eddi";
    static final String NAMESPACE = "test-ns";

    private TestSupport() {
        // utility class
    }

    /**
     * Creates a minimal EddiResource with standard test metadata.
     */
    static EddiResource createEddi() {
        var eddi = new EddiResource();
        var meta = new ObjectMeta();
        meta.setName(CR_NAME);
        meta.setNamespace(NAMESPACE);
        eddi.setMetadata(meta);
        eddi.setSpec(new EddiSpec());
        return eddi;
    }

    /**
     * Calls the protected {@code desired(EddiResource, Context)} method on a
     * CRUDKubernetesDependentResource via reflection.
     * <p>
     * This is safe for DRs whose {@code desired()} does NOT read from Context
     * (i.e., only uses the EddiResource parameter). The context is passed as null.
     * </p>
     *
     * @param dr   the dependent resource instance
     * @param eddi the EddiResource to pass to desired()
     * @param <T>  the Kubernetes resource type returned by desired()
     * @return the desired Kubernetes resource
     * @throws IllegalStateException if reflection fails (indicates a test authoring bug)
     */
    @SuppressWarnings("unchecked")
    static <T extends HasMetadata> T callDesired(
            CRUDKubernetesDependentResource<T, EddiResource> dr,
            EddiResource eddi) {
        try {
            Method method = dr.getClass().getDeclaredMethod("desired",
                    EddiResource.class, Context.class);
            method.setAccessible(true);
            return (T) method.invoke(dr, eddi, null);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to invoke desired() on " + dr.getClass().getSimpleName()
                            + ". If this DR reads from Context, it cannot be tested this way.", e);
        }
    }
}
