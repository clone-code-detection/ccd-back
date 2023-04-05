package github.clone_code_detection.entity.highlight.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.document.HighlightMatchDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSingleDocument;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Builder
@Data
public class HighlightSingleMatchDTO {
    private UUID id;

    @JsonProperty("source")
    private String source;

    @JsonProperty("target")
    private String target;

    @JsonProperty("target_matches")
    @Builder.Default
    private List<Integer[]> targetMatches = new ArrayList<>();

    public static HighlightSingleMatchDTO fromHighlightSingleMatchDTO(HighlightSingleDocument document) {
        FileDocument targetFile = document.getTarget();
        FileDocument sourceFile = document.getSource();
        return HighlightSingleMatchDTO.builder()
                                      .source(sourceFile.getContentAsString())
                                      .target(targetFile.getContentAsString())
                                      .targetMatches(extractMatches(document.getMatches()))
                                      .build();
    }

    private static List<Integer[]> extractMatches(Collection<HighlightMatchDocument> matches) {
        return matches.stream()
                      .map(highlightMatch -> new Integer[]{highlightMatch.getStart(), highlightMatch.getEnd()})
                      .toList();
    }
}
