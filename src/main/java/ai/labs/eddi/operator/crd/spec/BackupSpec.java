package ai.labs.eddi.operator.crd.spec;

/**
 * Backup and restore configuration.
 */
public class BackupSpec {

    private boolean enabled = false;
    private String schedule = "0 2 * * *"; // Daily at 02:00
    private int retentionDays = 7;
    private BackupStorageSpec storage = new BackupStorageSpec();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public BackupStorageSpec getStorage() {
        return storage;
    }

    public void setStorage(BackupStorageSpec storage) {
        this.storage = storage;
    }

    public static class BackupStorageSpec {
        private String type = "pvc"; // "pvc" | "s3"
        private PvcBackupSpec pvc = new PvcBackupSpec();
        private S3BackupSpec s3 = new S3BackupSpec();

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public PvcBackupSpec getPvc() {
            return pvc;
        }

        public void setPvc(PvcBackupSpec pvc) {
            this.pvc = pvc;
        }

        public S3BackupSpec getS3() {
            return s3;
        }

        public void setS3(S3BackupSpec s3) {
            this.s3 = s3;
        }
    }

    public static class PvcBackupSpec {
        private String size = "50Gi";
        private String storageClassName = "";

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

    public static class S3BackupSpec {
        private String bucket = "";
        private String region = "";
        private String endpoint = "";
        private String secretRef = "";

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getSecretRef() {
            return secretRef;
        }

        public void setSecretRef(String secretRef) {
            this.secretRef = secretRef;
        }
    }
}
