package ai.labs.eddi.operator.util;

import ai.labs.eddi.operator.crd.EddiResource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Standard Kubernetes labels builder following the app.kubernetes.io/ convention.
 * All child resources managed by the operator use these labels for consistent identification.
 */
public final class Labels {

    private Labels() {
        // utility class
    }

    /**
     * Standard labels for all managed resources.
     *
     * @param eddi      the parent Eddi CR
     * @param component the component name (e.g., "server", "mongodb", "nats", "manager")
     * @return label map
     */
    public static Map<String, String> standard(EddiResource eddi, String component) {
        var labels = new LinkedHashMap<String, String>();
        labels.put("app.kubernetes.io/name", "eddi");
        labels.put("app.kubernetes.io/instance", eddi.getMetadata().getName());
        labels.put("app.kubernetes.io/component", component);
        labels.put("app.kubernetes.io/version", eddi.getSpec().getVersion());
        labels.put("app.kubernetes.io/managed-by", "eddi-operator");
        labels.put("app.kubernetes.io/part-of", "eddi");
        return labels;
    }

    /**
     * Selector labels (subset of standard) for matching pods to services/deployments.
     *
     * @param eddi      the parent Eddi CR
     * @param component the component name
     * @return selector label map
     */
    public static Map<String, String> selector(EddiResource eddi, String component) {
        var labels = new LinkedHashMap<String, String>();
        labels.put("app.kubernetes.io/name", "eddi");
        labels.put("app.kubernetes.io/instance", eddi.getMetadata().getName());
        labels.put("app.kubernetes.io/component", component);
        return labels;
    }

    /**
     * Generates a resource name prefixed with the CR name to support multi-tenancy.
     *
     * @param eddi   the parent Eddi CR
     * @param suffix the resource suffix (e.g., "server", "mongodb", "config")
     * @return prefixed resource name
     */
    public static String resourceName(EddiResource eddi, String suffix) {
        var name = eddi.getMetadata().getName() + "-" + suffix;
        // Kubernetes names are limited to 253 characters
        if (name.length() > 253) {
            name = name.substring(0, 253);
        }
        return name;
    }

    /**
     * Sanitizes a string for safe use in shell commands.
     * Only allows DNS-safe characters (lowercase alphanumeric, hyphens, dots).
     * Rejects anything containing shell metacharacters.
     *
     * @param input the string to sanitize (typically a Kubernetes resource name)
     * @return the validated input
     * @throws IllegalArgumentException if the input contains unsafe characters
     */
    public static String sanitizeForShell(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Shell input must not be null or blank");
        }
        if (!SHELL_SAFE_PATTERN.matcher(input).matches()) {
            throw new IllegalArgumentException(
                    "Unsafe characters in shell input: '" + input
                            + "'. Only [a-z0-9.-] are allowed.");
        }
        return input;
    }

    private static final java.util.regex.Pattern SHELL_SAFE_PATTERN =
            java.util.regex.Pattern.compile("^[a-z0-9][a-z0-9.\\-]*$");
}
