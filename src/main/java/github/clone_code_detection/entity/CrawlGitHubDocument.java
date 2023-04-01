package github.clone_code_detection.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class CrawlGitHubDocument {
    private Map<String, Object> meta;

    public CrawlGitHubDocument() {
        meta = new HashMap<>();
    }

    public String toString() {
        try {
            return new ObjectMapper().writer()
                                     .withDefaultPrettyPrinter()
                                     .writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
