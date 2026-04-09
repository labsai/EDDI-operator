package ai.labs.eddi.operator.crd.spec;

/**
 * Authentication configuration — currently supports Keycloak.
 */
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

    public static class ManagedAuthSpec {
        private boolean enabled = false;
        private ComponentImageSpec image = new ComponentImageSpec("quay.io/keycloak/keycloak", "26.0");
        private String adminSecretRef = "";
        /**
         * Keycloak mode: "dev" uses start-dev with ephemeral storage,
         * "production" uses start with TLS and requires an admin secret.
         */
        private String mode = "dev";
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

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public ResourcesSpec getResources() {
            return resources;
        }

        public void setResources(ResourcesSpec resources) {
            this.resources = resources;
        }
    }

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
