package github.clone_code_detection.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CrawlGitHubDocument {
    private Collection<String> target;
    private Map<String, Object> meta;

    public CrawlGitHubDocument() {
        target = new ArrayList<>();
        meta = new HashMap<>();
    }

    public Collection<String> getTarget() {
        return target;
    }

    public void setTarget(Collection<String> target) {
        this.target = target;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta;
    }

    public String toString() {
        try {
            return new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
