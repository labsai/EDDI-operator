package ai.labs.eddi.operator.crd.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Pod Disruption Budget configuration.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PdbSpec {

    private boolean enabled = false;
    private int minAvailable = 1;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMinAvailable() {
        return minAvailable;
    }

    public void setMinAvailable(int minAvailable) {
        this.minAvailable = minAvailable;
    }
}
