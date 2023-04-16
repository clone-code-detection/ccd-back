package github.clone_code_detection.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SourceCodeDocument {
    private String content;
    private String extension;
}
