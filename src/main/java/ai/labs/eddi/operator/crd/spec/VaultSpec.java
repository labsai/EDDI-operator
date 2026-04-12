package ai.labs.eddi.operator.crd.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Vault / secrets configuration.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VaultSpec {

    private String masterKeySecretRef = "";

    public String getMasterKeySecretRef() {
        return masterKeySecretRef;
    }

    public void setMasterKeySecretRef(String masterKeySecretRef) {
        this.masterKeySecretRef = masterKeySecretRef;
    }
}
