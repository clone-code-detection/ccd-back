package github.clone_code_detection.entity.highlight.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSingleDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSingleTargetMatchDocument;
import github.clone_code_detection.util.ModelMapperUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.UUID;

@Builder
@Data

@NoArgsConstructor
@AllArgsConstructor
public class HighlightSingleSourceDTO {
    @JsonProperty("id")
    private UUID id;

    @JsonProperty("source")
    private FileDocument source;

    @JsonProperty("match_ids")
    private Collection<UUID> matchIds;

    public static HighlightSingleSourceDTO from(HighlightSingleDocument document) {
        HighlightSingleSourceDTO singleSourceDTO = ModelMapperUtil.getMapper()
                                                                  .map(document, HighlightSingleSourceDTO.class);
        singleSourceDTO.matchIds = document.getMatches()
                                           .stream()
                                           .map(HighlightSingleTargetMatchDocument::getId)
                                           .toList();
        return singleSourceDTO;
    }
}
