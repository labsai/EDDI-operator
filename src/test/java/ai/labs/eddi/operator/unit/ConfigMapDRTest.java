package ai.labs.eddi.operator.unit;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.EddiSpec;
import ai.labs.eddi.operator.dependent.core.ConfigMapDR;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ConfigMapDR — verifies that buildConfigData produces
 * the correct environment variables for different EddiSpec configurations.
 */
class ConfigMapDRTest {

    @Test
    void shouldGenerateMongodbConfigForManagedMongo() {
        var spec = new EddiSpec();
        spec.getDatastore().setType("mongodb");
        spec.getDatastore().getManaged().setEnabled(true);

        Map<String, String> data = ConfigMapDR.buildConfigData(spec, "my-eddi");

        assertThat(data).containsEntry("EDDI_DATASTORE_TYPE", "mongodb");
        assertThat(data).containsEntry("MONGODB_CONNECTION_STRING", "mongodb://my-eddi-mongodb:27017/eddi");
        assertThat(data).doesNotContainKey("QUARKUS_PROFILE");
    }

    @Test
    void shouldGenerateExternalMongoConfig() {
        var spec = new EddiSpec();
        spec.getDatastore().setType("mongodb");
        spec.getDatastore().getManaged().setEnabled(false);
        spec.getDatastore().getExternal().setConnectionString("mongodb://atlas.example.com:27017/eddi");

        Map<String, String> data = ConfigMapDR.buildConfigData(spec, "my-eddi");

        assertThat(data).containsEntry("EDDI_DATASTORE_TYPE", "mongodb");
        assertThat(data).containsEntry("MONGODB_CONNECTION_STRING", "mongodb://atlas.example.com:27017/eddi");
    }

    @Test
    void shouldGeneratePostgresConfigForManagedPostgres() {
        var spec = new EddiSpec();
        spec.getDatastore().setType("postgres");
        spec.getDatastore().getManaged().setEnabled(true);

        Map<String, String> data = ConfigMapDR.buildConfigData(spec, "my-eddi");

        assertThat(data).containsEntry("EDDI_DATASTORE_TYPE", "postgres");
        assertThat(data).containsEntry("QUARKUS_PROFILE", "postgres");
        assertThat(data).containsEntry("QUARKUS_DATASOURCE_JDBC_URL", "jdbc:postgresql://my-eddi-postgres:5432/eddi");
    }

    @Test
    void shouldGenerateNatsConfigForManagedNats() {
        var spec = new EddiSpec();
        spec.getMessaging().setType("nats");
        spec.getMessaging().getManaged().setEnabled(true);

        Map<String, String> data = ConfigMapDR.buildConfigData(spec, "my-eddi");

        assertThat(data).containsEntry("EDDI_MESSAGING_TYPE", "nats");
        assertThat(data).containsEntry("NATS_URL", "nats://my-eddi-nats:4222");
    }

    @Test
    void shouldGenerateInMemoryMessagingConfig() {
        var spec = new EddiSpec();
        // Default is in-memory

        Map<String, String> data = ConfigMapDR.buildConfigData(spec, "my-eddi");

        assertThat(data).containsEntry("EDDI_MESSAGING_TYPE", "in-memory");
        assertThat(data).doesNotContainKey("NATS_URL");
    }

    @Test
    void shouldEnableOidcWhenAuthEnabled() {
        var spec = new EddiSpec();
        spec.getAuth().setEnabled(true);
        spec.getAuth().getExternal().setAuthServerUrl("https://keycloak.example.com/realms/eddi");
        spec.getAuth().getExternal().setClientId("eddi-backend");

        Map<String, String> data = ConfigMapDR.buildConfigData(spec, "my-eddi");

        assertThat(data).containsEntry("QUARKUS_OIDC_ENABLED", "true");
        assertThat(data).containsEntry("QUARKUS_OIDC_AUTH_SERVER_URL", "https://keycloak.example.com/realms/eddi");
        assertThat(data).containsEntry("QUARKUS_OIDC_CLIENT_ID", "eddi-backend");
    }

    @Test
    void shouldDisableOidcByDefault() {
        var spec = new EddiSpec();

        Map<String, String> data = ConfigMapDR.buildConfigData(spec, "my-eddi");

        assertThat(data).containsEntry("QUARKUS_OIDC_ENABLED", "false");
    }

    @Test
    void shouldIncludeCorsOrigins() {
        var spec = new EddiSpec();
        spec.setCors("https://app.example.com,https://admin.example.com");

        Map<String, String> data = ConfigMapDR.buildConfigData(spec, "my-eddi");

        assertThat(data).containsEntry("QUARKUS_HTTP_CORS_ORIGINS", "https://app.example.com,https://admin.example.com");
    }
}
