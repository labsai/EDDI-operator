package ai.labs.eddi.operator.unit;

import ai.labs.eddi.operator.crd.EddiSpec;
import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.spec.ComponentImageSpec;
import ai.labs.eddi.operator.conditions.KeycloakSecretActivationCondition;
import ai.labs.eddi.operator.dependent.auth.KeycloakSecretDR;
import ai.labs.eddi.operator.reconciler.EddiReconciler;
import ai.labs.eddi.operator.util.Defaults;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for new code from the code-review remediation:
 * - Spec validation
 * - resolveImage utility
 * - KeycloakSecretActivationCondition
 * - KeycloakSecretDR password generation
 */
class ValidationAndSecurityTests {

    // ────────────────────────────────────────────────────
    //  EddiReconciler.validateSpec()
    // ────────────────────────────────────────────────────

    @Test
    void shouldAcceptValidMinimalSpec() {
        var spec = new EddiSpec();
        assertThat(EddiReconciler.validateSpec(spec)).isNull();
    }

    @Test
    void shouldAcceptValidPostgresSpec() {
        var spec = new EddiSpec();
        spec.getDatastore().setType("postgres");
        assertThat(EddiReconciler.validateSpec(spec)).isNull();
    }

    @Test
    void shouldRejectInvalidDatastoreType() {
        var spec = new EddiSpec();
        spec.getDatastore().setType("mysql");
        assertThat(EddiReconciler.validateSpec(spec))
                .contains("mysql")
                .contains("datastore.type");
    }

    @Test
    void shouldRejectInvalidMessagingType() {
        var spec = new EddiSpec();
        spec.getMessaging().setType("kafka");
        assertThat(EddiReconciler.validateSpec(spec))
                .contains("kafka")
                .contains("messaging.type");
    }

    @Test
    void shouldRejectInvalidExposureType() {
        var spec = new EddiSpec();
        spec.getExposure().setType("nodeport");
        assertThat(EddiReconciler.validateSpec(spec))
                .contains("nodeport")
                .contains("exposure.type");
    }

    @Test
    void shouldRejectZeroReplicas() {
        var spec = new EddiSpec();
        spec.setReplicas(0);
        assertThat(EddiReconciler.validateSpec(spec))
                .contains("replicas");
    }

    @Test
    void shouldRejectNegativeReplicas() {
        var spec = new EddiSpec();
        spec.setReplicas(-1);
        assertThat(EddiReconciler.validateSpec(spec))
                .contains("replicas");
    }

    @ParameterizedTest
    @CsvSource({
            "mongodb, in-memory, auto",
            "mongodb, nats, auto",
            "postgres, in-memory, ingress",
            "postgres, nats, route",
            "mongodb, in-memory, none"
    })
    void shouldAcceptAllValidEnumCombinations(String ds, String msg, String exp) {
        var spec = new EddiSpec();
        spec.getDatastore().setType(ds);
        spec.getMessaging().setType(msg);
        spec.getExposure().setType(exp);
        assertThat(EddiReconciler.validateSpec(spec)).isNull();
    }

    // ────────────────────────────────────────────────────
    //  Defaults.resolveImage()
    // ────────────────────────────────────────────────────

    @Test
    void shouldResolveImageWithRepoAndTag() {
        assertThat(Defaults.resolveImage("mongo", "7.0")).isEqualTo("mongo:7.0");
    }

    @Test
    void shouldResolveImageWithFullRepo() {
        assertThat(Defaults.resolveImage("quay.io/keycloak/keycloak", "26.0"))
                .isEqualTo("quay.io/keycloak/keycloak:26.0");
    }

    @Test
    void shouldReturnRepoWhenTagIsNull() {
        assertThat(Defaults.resolveImage("mongo", null)).isEqualTo("mongo");
    }

    @Test
    void shouldReturnRepoWhenTagIsBlank() {
        assertThat(Defaults.resolveImage("mongo", "")).isEqualTo("mongo");
    }

    @Test
    void shouldNotDoubleTagWhenRepoContainsColon() {
        // If someone passes "mongo:7.0" as repo, don't append tag
        assertThat(Defaults.resolveImage("mongo:7.0", "8.0")).isEqualTo("mongo:7.0");
    }

    // ────────────────────────────────────────────────────
    //  KeycloakSecretActivationCondition
    // ────────────────────────────────────────────────────

    @Test
    void shouldNotActivateWhenAuthDisabled() {
        var cond = new KeycloakSecretActivationCondition();
        var eddi = createEddi();
        eddi.getSpec().getAuth().setEnabled(false);

        assertThat(cond.isMet(null, eddi, null)).isFalse();
    }

    @Test
    void shouldNotActivateWhenManagedAuthDisabled() {
        var cond = new KeycloakSecretActivationCondition();
        var eddi = createEddi();
        eddi.getSpec().getAuth().setEnabled(true);
        eddi.getSpec().getAuth().getManaged().setEnabled(false);

        assertThat(cond.isMet(null, eddi, null)).isFalse();
    }

    @Test
    void shouldActivateWhenManagedAuthAndNoExternalSecret() {
        var cond = new KeycloakSecretActivationCondition();
        var eddi = createEddi();
        eddi.getSpec().getAuth().setEnabled(true);
        eddi.getSpec().getAuth().getManaged().setEnabled(true);
        eddi.getSpec().getAuth().getManaged().setAdminSecretRef("");

        assertThat(cond.isMet(null, eddi, null)).isTrue();
    }

    @Test
    void shouldNotActivateWhenExternalSecretProvided() {
        var cond = new KeycloakSecretActivationCondition();
        var eddi = createEddi();
        eddi.getSpec().getAuth().setEnabled(true);
        eddi.getSpec().getAuth().getManaged().setEnabled(true);
        eddi.getSpec().getAuth().getManaged().setAdminSecretRef("my-kc-secret");

        assertThat(cond.isMet(null, eddi, null)).isFalse();
    }

    // ────────────────────────────────────────────────────
    //  KeycloakSecretDR.generatePassword()
    // ────────────────────────────────────────────────────

    @Test
    void shouldGeneratePasswordOfCorrectLength() {
        var password = KeycloakSecretDR.generatePassword();
        assertThat(password).hasSize(24);
    }

    @Test
    void shouldGenerateUniquePasswords() {
        var p1 = KeycloakSecretDR.generatePassword();
        var p2 = KeycloakSecretDR.generatePassword();
        assertThat(p1).isNotEqualTo(p2);
    }

    @Test
    void shouldGeneratePasswordWithValidCharacters() {
        var password = KeycloakSecretDR.generatePassword();
        assertThat(password).matches("[A-Za-z0-9!@#$%&*]+");
    }

    // ────────────────────────────────────────────────────
    //  ComponentImageSpec defaults
    // ────────────────────────────────────────────────────

    @Test
    void shouldDefaultToEmptyImageSpec() {
        var img = new ComponentImageSpec();
        assertThat(img.getRepository()).isEmpty();
        assertThat(img.getTag()).isEmpty();
    }

    @Test
    void shouldCreateImageSpecWithValues() {
        var img = new ComponentImageSpec("mongo", "7.0");
        assertThat(img.getRepository()).isEqualTo("mongo");
        assertThat(img.getTag()).isEqualTo("7.0");
    }

    // ────────────────────────────────────────────────────
    //  Helpers
    // ────────────────────────────────────────────────────

    private EddiResource createEddi() {
        var eddi = new EddiResource();
        var meta = new ObjectMeta();
        meta.setName("test-eddi");
        meta.setNamespace("test-ns");
        eddi.setMetadata(meta);
        eddi.setSpec(new EddiSpec());
        return eddi;
    }
}
