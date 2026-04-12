package ai.labs.eddi.operator.crd.spec;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Container image configuration for EDDI or sub-components.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageSpec {

    private String repository = "labsai/eddi";
    private String tag = "";
    private String pullPolicy = "IfNotPresent";
    private List<String> pullSecrets = new ArrayList<>();

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getPullPolicy() {
        return pullPolicy;
    }

    public void setPullPolicy(String pullPolicy) {
        this.pullPolicy = pullPolicy;
    }

    public List<String> getPullSecrets() {
        return pullSecrets;
    }

    public void setPullSecrets(List<String> pullSecrets) {
        this.pullSecrets = pullSecrets;
    }
}
