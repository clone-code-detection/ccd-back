package github.clone_code_detection.entity.highlight.request;

import github.clone_code_detection.entity.fs.FileDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HighlightSessionRequest {
    private String fileName;
    private Collection<FileDocument> sources;
}
