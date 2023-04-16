package github.clone_code_detection.entity.index;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.clone_code_detection.entity.fs.FileDocument;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Builder
@Data
@AllArgsConstructor
public class IndexInstruction {
    @NotEmpty Collection<FileDocument> files;

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

    public static IndexInstruction getDefaultInstruction() {
        return IndexInstruction.builder()
                               .build();
    }
}
