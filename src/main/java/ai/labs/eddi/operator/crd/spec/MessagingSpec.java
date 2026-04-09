package ai.labs.eddi.operator.crd.spec;

/**
 * Messaging configuration — in-memory or NATS JetStream.
 */
public class MessagingSpec {

    private String type = "in-memory"; // "in-memory" | "nats"

    private ManagedMessagingSpec managed = new ManagedMessagingSpec();

    private ExternalMessagingSpec external = new ExternalMessagingSpec();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ManagedMessagingSpec getManaged() {
        return managed;
    }

    public void setManaged(ManagedMessagingSpec managed) {
        this.managed = managed;
    }

    public ExternalMessagingSpec getExternal() {
        return external;
    }

    public void setExternal(ExternalMessagingSpec external) {
        this.external = external;
    }

    public static class ManagedMessagingSpec {
        private boolean enabled = true;
        private ComponentImageSpec image = new ComponentImageSpec("nats", "2.10-alpine");
        private ManagedDatabaseSpec.StorageSpec storage = new ManagedDatabaseSpec.StorageSpec("5Gi", "");
        private ResourcesSpec resources = new ResourcesSpec("50m", "64Mi", "500m", "256Mi");

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

        public ManagedDatabaseSpec.StorageSpec getStorage() {
            return storage;
        }

        public void setStorage(ManagedDatabaseSpec.StorageSpec storage) {
            this.storage = storage;
        }

        public ResourcesSpec getResources() {
            return resources;
        }

        public void setResources(ResourcesSpec resources) {
            this.resources = resources;
        }
    }

    public static class ExternalMessagingSpec {
        private String url = "";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
