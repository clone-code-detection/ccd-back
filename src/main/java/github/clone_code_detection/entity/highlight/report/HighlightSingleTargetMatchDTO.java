package github.clone_code_detection.entity.highlight.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSingleTargetMatchDocument;
import github.clone_code_detection.util.ModelMapperUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

@Builder
@Data

@NoArgsConstructor
@AllArgsConstructor
public class HighlightSingleTargetMatchDTO {
    @JsonProperty
    private UUID id;

    @JsonProperty("target")
    private FileDocument target;

    @JsonProperty("matches")
    @Builder.Default
    private Collection<HighlightWordMatchDTO> matches = new ArrayList<>();


    public static HighlightSingleTargetMatchDTO from(HighlightSingleTargetMatchDocument document, Collection<HighlightWordMatchDTO> matches) {
        HighlightSingleTargetMatchDTO targetMatchDTO = ModelMapperUtil.getMapper()
                                                                      .map(document,
                                                                              HighlightSingleTargetMatchDTO.class);
        targetMatchDTO.matches = matches;
        return targetMatchDTO;
    }
}
