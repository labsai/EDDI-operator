package ai.labs.eddi.operator.unit;

import ai.labs.eddi.operator.dependent.core.VaultSecretDR;
import ai.labs.eddi.operator.dependent.datastore.PostgresSecretDR;
import ai.labs.eddi.operator.dependent.auth.KeycloakSecretDR;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for secret generation resilience and cryptographic quality.
 * Validates that generated secrets are unique, properly formatted,
 * and meet minimum security requirements.
 */
class SecretResilienceTest {

    // ────────────────────────────────────────────────────
    //  VaultSecretDR — key generation
    // ────────────────────────────────────────────────────

    @Test
    void vaultKeyShouldBeValidBase64() {
        var key = VaultSecretDR.generateRandomKey();
        // Should not throw
        var decoded = Base64.getDecoder().decode(key);
        assertThat(decoded).isNotEmpty();
    }

    @Test
    void vaultKeyShouldBe256Bits() {
        var key = VaultSecretDR.generateRandomKey();
        var decoded = Base64.getDecoder().decode(key);
        assertThat(decoded).hasSize(32); // 256 bits = 32 bytes
    }

    @Test
    void vaultKeysShouldBeUnique() {
        var k1 = VaultSecretDR.generateRandomKey();
        var k2 = VaultSecretDR.generateRandomKey();
        assertThat(k1).isNotEqualTo(k2);
    }

    @RepeatedTest(10)
    void vaultKeyShouldAlwaysBe256Bits() {
        var key = VaultSecretDR.generateRandomKey();
        var decoded = Base64.getDecoder().decode(key);
        assertThat(decoded).hasSize(32);
    }

    // ────────────────────────────────────────────────────
    //  PostgresSecretDR — credential generation
    // ────────────────────────────────────────────────────

    @Test
    void postgresPasswordShouldBeCorrectLength() {
        var password = PostgresSecretDR.generatePassword();
        assertThat(password).hasSize(24);
    }

    @Test
    void postgresPasswordShouldUseValidCharset() {
        var password = PostgresSecretDR.generatePassword();
        assertThat(password).matches("[A-Za-z0-9]+");
    }

    @Test
    void postgresPasswordsShouldBeUnique() {
        var p1 = PostgresSecretDR.generatePassword();
        var p2 = PostgresSecretDR.generatePassword();
        assertThat(p1).isNotEqualTo(p2);
    }

    @Test
    void postgresPasswordsShouldHaveHighEntropy() {
        // Generate 100 passwords, all should be unique
        Set<String> passwords = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            passwords.add(PostgresSecretDR.generatePassword());
        }
        assertThat(passwords).hasSize(100);
    }

    // ────────────────────────────────────────────────────
    //  KeycloakSecretDR — credential generation
    // ────────────────────────────────────────────────────

    @Test
    void keycloakPasswordShouldBeCorrectLength() {
        var password = KeycloakSecretDR.generatePassword();
        assertThat(password).hasSize(24);
    }

    @Test
    void keycloakPasswordShouldUseExtendedCharset() {
        // Keycloak password charset includes special chars
        var password = KeycloakSecretDR.generatePassword();
        assertThat(password).matches("[A-Za-z0-9!@#$%&*]+");
    }

    @Test
    void keycloakPasswordsShouldBeUnique() {
        var p1 = KeycloakSecretDR.generatePassword();
        var p2 = KeycloakSecretDR.generatePassword();
        assertThat(p1).isNotEqualTo(p2);
    }

    @Test
    void keycloakPasswordsShouldHaveHighEntropy() {
        Set<String> passwords = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            passwords.add(KeycloakSecretDR.generatePassword());
        }
        assertThat(passwords).hasSize(100);
    }
}
