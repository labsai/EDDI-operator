package ai.labs.eddi.operator.crd.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Messaging configuration — in-memory or NATS JetStream.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessagingSpec {

    private MessagingType type = MessagingType.IN_MEMORY;

    private ManagedMessagingSpec managed = new ManagedMessagingSpec();

    private ExternalMessagingSpec external = new ExternalMessagingSpec();

    public MessagingType getType() {
        return type;
    }

    public void setType(MessagingType type) {
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

    @JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonIgnoreProperties(ignoreUnknown = true)
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
