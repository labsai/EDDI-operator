package ai.labs.eddi.operator.crd.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Authentication configuration — currently supports Keycloak.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthSpec {

    private boolean enabled = false;
    private String provider = "keycloak";
    private ManagedAuthSpec managed = new ManagedAuthSpec();
    private ExternalAuthSpec external = new ExternalAuthSpec();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public ManagedAuthSpec getManaged() {
        return managed;
    }

    public void setManaged(ManagedAuthSpec managed) {
        this.managed = managed;
    }

    public ExternalAuthSpec getExternal() {
        return external;
    }

    public void setExternal(ExternalAuthSpec external) {
        this.external = external;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ManagedAuthSpec {
        private boolean enabled = false;
        private ComponentImageSpec image = new ComponentImageSpec("quay.io/keycloak/keycloak", "26.0");
        private String adminSecretRef = "";
        /**
         * Keycloak mode: DEV uses start-dev with ephemeral storage,
         * PRODUCTION uses start with TLS and requires an admin secret.
         */
        private KeycloakMode mode = KeycloakMode.DEV;
        private ResourcesSpec resources = new ResourcesSpec("250m", "512Mi", "1", "1Gi");

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public ComponentImageSpec getImage() {
            return image;
        }

        public void setImage(ComponentImageSpec image) {
            this.image = image;
        }

        public String getAdminSecretRef() {
            return adminSecretRef;
        }

        public void setAdminSecretRef(String adminSecretRef) {
            this.adminSecretRef = adminSecretRef;
        }

        public KeycloakMode getMode() {
            return mode;
        }

        public void setMode(KeycloakMode mode) {
            this.mode = mode;
        }

        public ResourcesSpec getResources() {
            return resources;
        }

        public void setResources(ResourcesSpec resources) {
            this.resources = resources;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExternalAuthSpec {
        private String authServerUrl = "";
        private String clientId = "eddi-backend";

        public String getAuthServerUrl() {
            return authServerUrl;
        }

        public void setAuthServerUrl(String authServerUrl) {
            this.authServerUrl = authServerUrl;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }
    }
}
