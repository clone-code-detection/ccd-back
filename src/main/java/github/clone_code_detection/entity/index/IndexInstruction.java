package github.clone_code_detection.entity.index;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Data
public class IndexInstruction {
    @NotEmpty
    @JsonProperty("target_languages")
    private Collection<String> targetLanguages;

    @NotEmpty
    @JsonProperty("meta")
    private Map<String, Object> meta;

    public IndexInstruction() {
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
