package ai.labs.eddi.operator.unit;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.EddiSpec;
import ai.labs.eddi.operator.crd.spec.DatastoreType;
import ai.labs.eddi.operator.crd.spec.MessagingType;
import ai.labs.eddi.operator.crd.spec.PvcRetentionPolicy;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the type-safe enum migration.
 * Verifies that all enum types serialize correctly via toString()/getValue()
 * and that default values are as expected.
 */
class EnumMigrationTest {

    private EddiSpec spec;

    @BeforeEach
    void setUp() {
        spec = new EddiSpec();
    }

    // ────────────────────────────────────────────────────
    //  DatastoreType
    // ────────────────────────────────────────────────────

    @Test
    void datastoreDefaultShouldBeMongodb() {
        assertThat(spec.getDatastore().getType()).isEqualTo(DatastoreType.MONGODB);
    }

    @Test
    void datastoreTypeValueShouldBeCorrect() {
        assertThat(DatastoreType.MONGODB.getValue()).isEqualTo("mongodb");
        assertThat(DatastoreType.POSTGRES.getValue()).isEqualTo("postgres");
    }

    @Test
    void datastoreTypeToStringShouldMatchValue() {
        assertThat(DatastoreType.MONGODB.toString()).isEqualTo("mongodb");
        assertThat(DatastoreType.POSTGRES.toString()).isEqualTo("postgres");
    }

    // ────────────────────────────────────────────────────
    //  MessagingType
    // ────────────────────────────────────────────────────

    @Test
    void messagingDefaultShouldBeInMemory() {
        assertThat(spec.getMessaging().getType()).isEqualTo(MessagingType.IN_MEMORY);
    }

    @Test
    void messagingTypeValueShouldBeCorrect() {
        assertThat(MessagingType.IN_MEMORY.getValue()).isEqualTo("in-memory");
        assertThat(MessagingType.NATS.getValue()).isEqualTo("nats");
    }

    // ────────────────────────────────────────────────────
    //  PvcRetentionPolicy
    // ────────────────────────────────────────────────────

    @Test
    void pvcRetentionDefaultShouldBeRetain() {
        assertThat(spec.getPvcRetentionPolicy()).isEqualTo(PvcRetentionPolicy.RETAIN);
    }

    @Test
    void pvcRetentionValueShouldBeCorrect() {
        assertThat(PvcRetentionPolicy.RETAIN.getValue()).isEqualTo("Retain");
        assertThat(PvcRetentionPolicy.DELETE.getValue()).isEqualTo("Delete");
    }

    // ────────────────────────────────────────────────────
    //  Enum identity comparison
    // ────────────────────────────────────────────────────

    @Test
    void enumComparisonShouldWorkWithIdentity() {
        spec.getDatastore().setType(DatastoreType.POSTGRES);
        assertThat(DatastoreType.POSTGRES == spec.getDatastore().getType()).isTrue();
        assertThat(DatastoreType.MONGODB == spec.getDatastore().getType()).isFalse();
    }

    @Test
    void enumComparisonShouldWorkAfterRoundtrip() {
        spec.getMessaging().setType(MessagingType.NATS);
        var retrieved = spec.getMessaging().getType();
        assertThat(retrieved).isEqualTo(MessagingType.NATS);
        assertThat(MessagingType.NATS == retrieved).isTrue();
    }
}
