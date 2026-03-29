package ai.labs.eddi.operator.unit;

import ai.labs.eddi.operator.util.Hashing;
import ai.labs.eddi.operator.util.Labels;
import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.EddiSpec;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Labels and Hashing utilities.
 */
class UtilTests {

    private EddiResource createEddi(String name) {
        var eddi = new EddiResource();
        var meta = new ObjectMeta();
        meta.setName(name);
        meta.setNamespace("test-ns");
        eddi.setMetadata(meta);
        eddi.setSpec(new EddiSpec());
        return eddi;
    }

    @Test
    void shouldGenerateStandardLabels() {
        var eddi = createEddi("my-eddi");
        var labels = Labels.standard(eddi, "server");

        assertThat(labels).containsEntry("app.kubernetes.io/name", "eddi");
        assertThat(labels).containsEntry("app.kubernetes.io/instance", "my-eddi");
        assertThat(labels).containsEntry("app.kubernetes.io/component", "server");
        assertThat(labels).containsEntry("app.kubernetes.io/managed-by", "eddi-operator");
    }

    @Test
    void shouldGenerateSelectorLabels() {
        var eddi = createEddi("my-eddi");
        var labels = Labels.selector(eddi, "mongodb");

        assertThat(labels).hasSize(3);
        assertThat(labels).containsEntry("app.kubernetes.io/name", "eddi");
        assertThat(labels).containsEntry("app.kubernetes.io/instance", "my-eddi");
        assertThat(labels).containsEntry("app.kubernetes.io/component", "mongodb");
    }

    @Test
    void shouldGenerateResourceName() {
        var eddi = createEddi("my-eddi");
        assertThat(Labels.resourceName(eddi, "server")).isEqualTo("my-eddi-server");
        assertThat(Labels.resourceName(eddi, "mongodb")).isEqualTo("my-eddi-mongodb");
    }

    @Test
    void shouldHashConfigData() {
        var data = Map.of("KEY1", "value1", "KEY2", "value2");
        var hash = Hashing.hash(data);

        assertThat(hash).isNotBlank();
        assertThat(hash).hasSize(16); // 8 bytes = 16 hex chars
    }

    @Test
    void shouldProduceDeterministicHash() {
        var data = Map.of("B", "2", "A", "1");
        var hash1 = Hashing.hash(data);
        var hash2 = Hashing.hash(data);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void shouldProduceDifferentHashForDifferentData() {
        var hash1 = Hashing.hash(Map.of("KEY", "value1"));
        var hash2 = Hashing.hash(Map.of("KEY", "value2"));

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void shouldHandleEmptyData() {
        assertThat(Hashing.hash(Map.of())).isEqualTo("empty");
        assertThat(Hashing.hash(null)).isEqualTo("empty");
    }
}
