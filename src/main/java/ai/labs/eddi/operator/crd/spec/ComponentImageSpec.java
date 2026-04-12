package ai.labs.eddi.operator.crd.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Reusable image configuration for infrastructure components
 * (MongoDB, PostgreSQL, NATS, Keycloak).
 * Simpler than ImageSpec — no pullPolicy/pullSecrets
 * since those are inherited from the parent EDDI image config.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComponentImageSpec {

    private String repository;
    private String tag;

    public ComponentImageSpec() {
        this("", "");
    }

    public ComponentImageSpec(String repository, String tag) {
        this.repository = repository;
        this.tag = tag;
    }

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
}
