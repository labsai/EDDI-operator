package ai.labs.eddi.operator.crd.spec;

/**
 * Manager UI configuration.
 */
public class ManagerSpec {

    private boolean enabled = false;
    private ImageSpec image = new ImageSpec();
    private ResourcesSpec resources = new ResourcesSpec("50m", "64Mi", "250m", "256Mi");

    public ManagerSpec() {
        this.image.setRepository("labsai/eddi-config-ui");
        this.image.setTag("latest");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ImageSpec getImage() {
        return image;
    }

    public void setImage(ImageSpec image) {
        this.image = image;
    }

    public ResourcesSpec getResources() {
        return resources;
    }

    public void setResources(ResourcesSpec resources) {
        this.resources = resources;
    }
}
