package ai.labs.eddi.operator.crd.spec;

/**
 * Vault / secrets configuration.
 */
public class VaultSpec {

    private String masterKeySecretRef = "";

    public String getMasterKeySecretRef() {
        return masterKeySecretRef;
    }

    public void setMasterKeySecretRef(String masterKeySecretRef) {
        this.masterKeySecretRef = masterKeySecretRef;
    }
}
