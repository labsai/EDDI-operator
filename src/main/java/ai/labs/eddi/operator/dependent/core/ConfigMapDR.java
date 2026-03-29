package ai.labs.eddi.operator.dependent.core;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.EddiSpec;
import ai.labs.eddi.operator.util.Defaults;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates the EDDI application configuration ConfigMap from the CR spec.
 * Contains all environment variables that EDDI needs to connect to its backends.
 */
@KubernetesDependent
public class ConfigMapDR extends CRUDKubernetesDependentResource<ConfigMap, EddiResource> {

    public ConfigMapDR() {
        super(ConfigMap.class);
    }

    @Override
    protected ConfigMap desired(EddiResource eddi, Context<EddiResource> context) {
        var spec = eddi.getSpec();
        var name = eddi.getMetadata().getName();
        var data = buildConfigData(spec, name);

        return new ConfigMapBuilder()
                .withNewMetadata()
                    .withName(Labels.resourceName(eddi, "config"))
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "config"))
                .endMetadata()
                .withData(data)
                .build();
    }

    /**
     * Builds the environment configuration data map from the EddiSpec.
     */
    public static Map<String, String> buildConfigData(EddiSpec spec, String crName) {
        var data = new LinkedHashMap<String, String>();

        // Datastore configuration
        data.put("EDDI_DATASTORE_TYPE", spec.getDatastore().getType());

        if ("mongodb".equals(spec.getDatastore().getType())) {
            if (spec.getDatastore().getManaged().isEnabled()) {
                data.put("MONGODB_CONNECTION_STRING", Defaults.managedMongoConnectionString(crName));
            } else {
                var connStr = spec.getDatastore().getExternal().getConnectionString();
                if (connStr != null && !connStr.isBlank()) {
                    data.put("MONGODB_CONNECTION_STRING", connStr);
                }
            }
        } else if ("postgres".equals(spec.getDatastore().getType())) {
            data.put("QUARKUS_PROFILE", "postgres");
            if (spec.getDatastore().getManaged().isEnabled()) {
                data.put("QUARKUS_DATASOURCE_JDBC_URL", Defaults.managedPostgresJdbcUrl(crName));
            }
            // External postgres reads from secretRef
        }

        // Messaging configuration
        data.put("EDDI_MESSAGING_TYPE", spec.getMessaging().getType());
        if ("nats".equals(spec.getMessaging().getType())) {
            if (spec.getMessaging().getManaged().isEnabled()) {
                data.put("NATS_URL", Defaults.managedNatsUrl(crName));
            } else {
                var url = spec.getMessaging().getExternal().getUrl();
                if (url != null && !url.isBlank()) {
                    data.put("NATS_URL", url);
                }
            }
        }

        // Auth configuration
        if (spec.getAuth().isEnabled()) {
            data.put("QUARKUS_OIDC_ENABLED", "true");
            if (!spec.getAuth().getManaged().isEnabled()) {
                var authUrl = spec.getAuth().getExternal().getAuthServerUrl();
                if (authUrl != null && !authUrl.isBlank()) {
                    data.put("QUARKUS_OIDC_AUTH_SERVER_URL", authUrl);
                }
                data.put("QUARKUS_OIDC_CLIENT_ID", spec.getAuth().getExternal().getClientId());
            }
        } else {
            data.put("QUARKUS_OIDC_ENABLED", "false");
        }

        // CORS
        if (spec.getCors() != null && !spec.getCors().isBlank()) {
            data.put("QUARKUS_HTTP_CORS_ORIGINS", spec.getCors());
        }

        return data;
    }
}
