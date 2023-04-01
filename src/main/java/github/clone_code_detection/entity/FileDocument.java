package github.clone_code_detection.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class FileDocument {
    @JsonProperty("content")
    private String content;
    @JsonProperty("file_name")
    private String fileName;
}
