package github.clone_code_detection.entity.index;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.clone_code_detection.entity.fs.FileDocument;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;

import java.util.Collection;
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

    public static IndexInstruction getDefaultInstruction() {
        return IndexInstruction.builder()
                .build();
    }

    @SneakyThrows
    public String toString() {
        return new ObjectMapper().writer()
                .withDefaultPrettyPrinter()
                .writeValueAsString(this);
    }
}
