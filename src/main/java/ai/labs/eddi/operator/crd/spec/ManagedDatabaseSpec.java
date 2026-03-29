package ai.labs.eddi.operator.crd.spec;

/**
 * Configuration for operator-managed database instances.
 */
public class ManagedDatabaseSpec {

    private boolean enabled = true;

    private StorageSpec storage = new StorageSpec("20Gi", "");

    private ResourcesSpec resources = new ResourcesSpec(
            "250m", "512Mi",
            "1", "1Gi"
    );

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public StorageSpec getStorage() {
        return storage;
    }

    public void setStorage(StorageSpec storage) {
        this.storage = storage;
    }

    public ResourcesSpec getResources() {
        return resources;
    }

    public void setResources(ResourcesSpec resources) {
        this.resources = resources;
    }

    /**
     * Storage configuration for PVC.
     */
    public static class StorageSpec {
        private String size;
        private String storageClassName;

        public StorageSpec() {
            this("20Gi", "");
        }

        public StorageSpec(String size, String storageClassName) {
            this.size = size;
            this.storageClassName = storageClassName;
        }

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public String getStorageClassName() {
            return storageClassName;
        }

        public void setStorageClassName(String storageClassName) {
            this.storageClassName = storageClassName;
        }
    }
}
