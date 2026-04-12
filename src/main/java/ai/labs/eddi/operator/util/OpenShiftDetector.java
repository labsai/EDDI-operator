package ai.labs.eddi.operator.util;

import io.javaoperatorsdk.operator.api.reconciler.Context;

import org.jboss.logging.Logger;

/**
 * Shared OpenShift detection utility.
 * Checks for the presence of the Route CRD to determine if the cluster is OpenShift.
 * Caches the result for the lifecycle of the operator (reset on restart).
 */
public final class OpenShiftDetector {

    private static final Logger LOG = Logger.getLogger(OpenShiftDetector.class);

    private static volatile Boolean isOpenShift = null;

    private OpenShiftDetector() {
        // utility class
    }

    /**
     * Returns true if the cluster has the Route CRD installed (i.e., is OpenShift).
     * Result is cached after first detection.
     */
    public static boolean isOpenShift(Context<?> context) {
        if (isOpenShift != null) {
            return isOpenShift;
        }
        try {
            var routeCrd = context.getClient().apiextensions().v1()
                    .customResourceDefinitions()
                    .withName("routes.route.openshift.io")
                    .get();
            isOpenShift = routeCrd != null;
        } catch (Exception e) {
            LOG.debugf(e, "OpenShift Route CRD not detected, assuming vanilla Kubernetes");
            isOpenShift = false;
        }
        return isOpenShift;
    }

    /**
     * Resets the cached detection result. Useful for testing.
     */
    public static void reset() {
        isOpenShift = null;
    }
}
