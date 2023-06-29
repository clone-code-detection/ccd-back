package github.clone_code_detection.entity.highlight.request;

import github.clone_code_detection.entity.fs.FileDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimilarityDetectRequest {
    private String reportName;
    private Collection<FileDocument> sources;
    private String author;
    private String origin;
    private String link;
    private Map<String, String> meta;
}
