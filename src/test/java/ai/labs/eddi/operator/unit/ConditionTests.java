package ai.labs.eddi.operator.unit;

import ai.labs.eddi.operator.crd.EddiSpec;
import ai.labs.eddi.operator.crd.spec.DatastoreType;
import ai.labs.eddi.operator.crd.spec.MessagingType;
import ai.labs.eddi.operator.util.Defaults;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for activation conditions and the Defaults utility.
 */
class ConditionTests {

    @Test
    void shouldDetectManagedMongodb() {
        var spec = new EddiSpec();
        spec.getDatastore().setType(DatastoreType.MONGODB);
        spec.getDatastore().getManaged().setEnabled(true);

        assertThat(Defaults.isManagedMongodb(spec)).isTrue();
        assertThat(Defaults.isManagedPostgres(spec)).isFalse();
    }

    @Test
    void shouldDetectManagedPostgres() {
        var spec = new EddiSpec();
        spec.getDatastore().setType(DatastoreType.POSTGRES);
        spec.getDatastore().getManaged().setEnabled(true);

        assertThat(Defaults.isManagedPostgres(spec)).isTrue();
        assertThat(Defaults.isManagedMongodb(spec)).isFalse();
    }

    @Test
    void shouldNotDetectManagedWhenExternal() {
        var spec = new EddiSpec();
        spec.getDatastore().setType(DatastoreType.MONGODB);
        spec.getDatastore().getManaged().setEnabled(false);

        assertThat(Defaults.isManagedMongodb(spec)).isFalse();
    }

    @Test
    void shouldDetectManagedNats() {
        var spec = new EddiSpec();
        spec.getMessaging().setType(MessagingType.NATS);
        spec.getMessaging().getManaged().setEnabled(true);

        assertThat(Defaults.isManagedNats(spec)).isTrue();
    }

    @Test
    void shouldNotDetectManagedNatsForInMemory() {
        var spec = new EddiSpec();
        // Default is in-memory

        assertThat(Defaults.isManagedNats(spec)).isFalse();
    }

    @Test
    void shouldDetectManagedAuth() {
        var spec = new EddiSpec();
        spec.getAuth().setEnabled(true);
        spec.getAuth().getManaged().setEnabled(true);

        assertThat(Defaults.isManagedAuth(spec)).isTrue();
    }

    @Test
    void shouldNotDetectManagedAuthWhenDisabled() {
        var spec = new EddiSpec();
        // Default: auth.enabled=false

        assertThat(Defaults.isManagedAuth(spec)).isFalse();
    }

    @Test
    void shouldResolveImageWithExplicitTag() {
        var spec = new EddiSpec();
        spec.getImage().setRepository("custom/eddi");
        spec.getImage().setTag("7.0.0");

        assertThat(Defaults.resolveEddiImage(spec)).isEqualTo("custom/eddi:7.0.0");
    }

    @Test
    void shouldResolveImageWithVersionFallback() {
        var spec = new EddiSpec();
        spec.setVersion("6.0.0");
        spec.getImage().setTag(""); // Empty means use version

        assertThat(Defaults.resolveEddiImage(spec)).isEqualTo("labsai/eddi:6.0.0");
    }

    @Test
    void shouldGenerateCorrectConnectionStrings() {
        assertThat(Defaults.managedMongoConnectionString("my-eddi"))
                .isEqualTo("mongodb://my-eddi-mongodb:27017/eddi");
        assertThat(Defaults.managedNatsUrl("my-eddi"))
                .isEqualTo("nats://my-eddi-nats:4222");
        assertThat(Defaults.managedPostgresJdbcUrl("my-eddi"))
                .isEqualTo("jdbc:postgresql://my-eddi-postgres:5432/eddi");
    }
}
