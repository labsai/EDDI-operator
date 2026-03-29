package ai.labs.eddi.operator.crd.spec;

/**
 * Pod Disruption Budget configuration.
 */
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
