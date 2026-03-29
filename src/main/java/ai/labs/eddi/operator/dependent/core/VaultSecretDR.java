package ai.labs.eddi.operator.dependent.core;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

/**
 * Manages the Vault master key Secret.
 * Only activated when spec.vault.masterKeySecretRef is empty (auto-generate mode).
 * When the user provides their own secret ref, VaultSecretActivationCondition skips this DR.
 */
@KubernetesDependent
public class VaultSecretDR extends CRUDKubernetesDependentResource<Secret, EddiResource> {

    private static final int KEY_LENGTH_BYTES = 32; // 256-bit

    public VaultSecretDR() {
        super(Secret.class);
    }

    @Override
    protected Secret desired(EddiResource eddi, Context<EddiResource> context) {
        var secretName = Labels.resourceName(eddi, "vault-key");

        // Check if secret already exists to avoid regenerating the key
        var existing = context.getClient().secrets()
                .inNamespace(eddi.getMetadata().getNamespace())
                .withName(secretName)
                .get();

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
    static String generateRandomKey() {
        var random = new SecureRandom();
        var keyBytes = new byte[KEY_LENGTH_BYTES];
        random.nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }
}
