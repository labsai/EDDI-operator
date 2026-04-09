package ai.labs.eddi.operator.dependent.auth;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.security.SecureRandom;
import java.util.Map;

/**
 * Auto-generates Keycloak admin credentials when using managed Keycloak.
 * Preserves existing credentials on subsequent reconciliations.
 * Only activated when spec.auth.enabled=true and spec.auth.managed.enabled=true
 * AND spec.auth.managed.adminSecretRef is empty.
 */
@KubernetesDependent
public class KeycloakSecretDR extends CRUDKubernetesDependentResource<Secret, EddiResource> {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*";
    private static final int PASSWORD_LENGTH = 24;

    public KeycloakSecretDR() {
        super(Secret.class);
    }

    @Override
    protected Secret desired(EddiResource eddi, Context<EddiResource> context) {
        var secretName = Labels.resourceName(eddi, "keycloak-admin");

        // Use direct client lookup to avoid secondary resource ambiguity
        var existing = context.getClient().secrets()
                .inNamespace(eddi.getMetadata().getNamespace())
                .withName(secretName)
                .get();

        if (existing != null && existing.getData() != null
                && existing.getData().containsKey("username")
                && existing.getData().containsKey("password")) {
            // Preserve existing credentials
            return new SecretBuilder()
                    .withNewMetadata()
                        .withName(secretName)
                        .withNamespace(eddi.getMetadata().getNamespace())
                        .withLabels(Labels.standard(eddi, "keycloak"))
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
                    .withLabels(Labels.standard(eddi, "keycloak"))
                .endMetadata()
                .withType("Opaque")
                .withStringData(Map.of(
                        "username", "admin",
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
