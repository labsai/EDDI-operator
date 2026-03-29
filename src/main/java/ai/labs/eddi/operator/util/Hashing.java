package ai.labs.eddi.operator.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Computes a deterministic hash of ConfigMap/Secret data.
 * This hash is added as a pod annotation to force rollouts when config changes.
 */
public final class Hashing {

    private Hashing() {
        // utility class
    }

    /**
     * Computes a SHA-256 hash of the given data map (sorted by key for determinism).
     *
     * @param data the ConfigMap or Secret data entries
     * @return hex-encoded hash string (first 16 chars)
     */
    public static String hash(Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return "empty";
        }

        try {
            var digest = MessageDigest.getInstance("SHA-256");
            // Sort keys for deterministic ordering
            var sorted = new TreeMap<>(data);
            for (var entry : sorted.entrySet()) {
                digest.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
                digest.update("=".getBytes(StandardCharsets.UTF_8));
                if (entry.getValue() != null) {
                    digest.update(entry.getValue().getBytes(StandardCharsets.UTF_8));
                }
                digest.update("\n".getBytes(StandardCharsets.UTF_8));
            }

            var hashBytes = digest.digest();
            var sb = new StringBuilder();
            for (int i = 0; i < Math.min(8, hashBytes.length); i++) {
                sb.append(String.format("%02x", hashBytes[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
