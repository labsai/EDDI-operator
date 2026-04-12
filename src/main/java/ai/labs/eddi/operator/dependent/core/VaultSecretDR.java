package ai.labs.eddi.operator.dependent.core;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import org.jboss.logging.Logger;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

/**
 * Manages the Vault master key Secret.
 * Only activated when spec.vault.masterKeySecretRef is empty (auto-generate mode).
 * When the user provides their own secret ref, VaultSecretActivationCondition skips this DR.
 *
 * <p><strong>Resilience:</strong> If the API lookup for an existing secret fails
 * (transient 503, network partition), this DR throws rather than silently generating
 * a new key — which would cause data loss when the application restarts with
 * a different encryption key.</p>
 */
@KubernetesDependent
public class VaultSecretDR extends CRUDKubernetesDependentResource<Secret, EddiResource> {

    private static final Logger LOG = Logger.getLogger(VaultSecretDR.class);
    private static final int KEY_LENGTH_BYTES = 32; // 256-bit

    public VaultSecretDR() {
        super(Secret.class);
    }

    @Override
    protected Secret desired(EddiResource eddi, Context<EddiResource> context) {
        var secretName = Labels.resourceName(eddi, "vault-key");

        // Check if secret already exists to avoid regenerating the key.
        // A transient API failure here MUST NOT cause silent key regeneration.
        Secret existing;
        try {
            existing = context.getClient().secrets()
                    .inNamespace(eddi.getMetadata().getNamespace())
                    .withName(secretName)
                    .get();
        } catch (Exception e) {
            LOG.warnf(e, "Vault secret lookup failed for '%s'. " +
                    "Aborting to prevent accidental key regeneration.", secretName);
            throw new IllegalStateException(
                    "Cannot verify existence of vault secret '" + secretName
                            + "'. Retrying on next reconciliation.", e);
        }

        var secretBuilder = new SecretBuilder()
                .withNewMetadata()
                    .withName(secretName)
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "vault"))
                .endMetadata()
                .withType("Opaque");

        if (existing != null && existing.getData() != null
                && existing.getData().containsKey("master-key")) {
            // Preserve existing key
            secretBuilder.withData(Map.of("master-key", existing.getData().get("master-key")));
        } else {
            // Generate new key
            secretBuilder.withStringData(Map.of("master-key", generateRandomKey()));
        }

        return secretBuilder.build();
    }

    /**
     * Generates a cryptographically secure random key, Base64-encoded.
     */
    public static String generateRandomKey() {
        var random = new SecureRandom();
        var keyBytes = new byte[KEY_LENGTH_BYTES];
        random.nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }
}
