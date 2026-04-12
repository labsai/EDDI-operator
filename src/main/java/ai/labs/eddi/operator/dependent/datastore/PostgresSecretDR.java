package ai.labs.eddi.operator.dependent.datastore;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import org.jboss.logging.Logger;

import java.security.SecureRandom;
import java.util.Map;

/**
 * Auto-generates PostgreSQL credentials when using managed PostgreSQL.
 * Preserves existing credentials on subsequent reconciliations.
 *
 * <p><strong>Resilience:</strong> If the API lookup for an existing secret fails,
 * this DR throws rather than silently regenerating credentials — which would lock
 * the operator out of the managed database.</p>
 */
@KubernetesDependent
public class PostgresSecretDR extends CRUDKubernetesDependentResource<Secret, EddiResource> {

    private static final Logger LOG = Logger.getLogger(PostgresSecretDR.class);
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int PASSWORD_LENGTH = 24;

    public PostgresSecretDR() {
        super(Secret.class);
    }

    @Override
    protected Secret desired(EddiResource eddi, Context<EddiResource> context) {
        var secretName = Labels.resourceName(eddi, "postgres-credentials");

        // Use direct client lookup to avoid secondary resource ambiguity.
        // A transient API failure here MUST NOT cause silent credential regeneration.
        Secret existing;
        try {
            existing = context.getClient().secrets()
                    .inNamespace(eddi.getMetadata().getNamespace())
                    .withName(secretName)
                    .get();
        } catch (Exception e) {
            LOG.warnf(e, "PostgreSQL secret lookup failed for '%s'. " +
                    "Aborting to prevent accidental credential regeneration.", secretName);
            throw new IllegalStateException(
                    "Cannot verify existence of postgres secret '" + secretName
                            + "'. Retrying on next reconciliation.", e);
        }

        if (existing != null && existing.getData() != null
                && existing.getData().containsKey("username")
                && existing.getData().containsKey("password")) {
            // Preserve existing credentials
            return new SecretBuilder()
                    .withNewMetadata()
                        .withName(secretName)
                        .withNamespace(eddi.getMetadata().getNamespace())
                        .withLabels(Labels.standard(eddi, "postgres"))
                    .endMetadata()
                    .withType("Opaque")
                    .withData(existing.getData())
                    .build();
        }

        // Generate new credentials
        return new SecretBuilder()
                .withNewMetadata()
                    .withName(secretName)
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "postgres"))
                .endMetadata()
                .withType("Opaque")
                .withStringData(Map.of(
                        "username", "eddi",
                        "password", generatePassword()
                ))
                .build();
    }

    public static String generatePassword() {
        var random = new SecureRandom();
        var sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
