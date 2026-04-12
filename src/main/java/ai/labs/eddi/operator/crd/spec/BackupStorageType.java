package ai.labs.eddi.operator.crd.spec;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Backup storage targets.
 * PVC stores to a PersistentVolumeClaim, S3 streams to object storage.
 */
public enum BackupStorageType {

    PVC("pvc"),
    S3("s3");

    private final String value;

    BackupStorageType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
