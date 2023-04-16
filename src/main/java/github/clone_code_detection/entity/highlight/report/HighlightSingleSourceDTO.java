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
import java.util.function.Function;

@Builder
@Data

@NoArgsConstructor
@AllArgsConstructor
public class HighlightSingleSourceDTO {
    @JsonProperty("id")
    private UUID id;

    @JsonProperty("source")
    private FileDocument source;

    @JsonProperty("matches")
    private Collection<HighlightSingleTargetMatchDTO> matches;

    public static HighlightSingleSourceDTO from(HighlightSingleDocument document,
                                                Function<HighlightSingleTargetMatchDocument, Collection<HighlightWordMatchDTO>> resolver) {
        HighlightSingleSourceDTO singleSourceDTO = ModelMapperUtil.getMapper()
                                                                  .map(document, HighlightSingleSourceDTO.class);
        singleSourceDTO.matches = document.getMatches()
                                          .stream()
                                          .map(target -> HighlightSingleTargetMatchDTO.from(target, resolver))
                                          .toList();
        return singleSourceDTO;
    }
}
