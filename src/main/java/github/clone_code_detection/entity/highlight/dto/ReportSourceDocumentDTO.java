package github.clone_code_detection.entity.highlight.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.document.ReportSourceDocument;
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
public class ReportSourceDocumentDTO {
    @JsonProperty("id")
    private UUID id;

    @JsonProperty("source")
    private FileDocument source;

    @JsonProperty("matches")
    private Collection<ReportTargetDocumentDTO> matches;

    public static ReportSourceDocumentDTO from(ReportSourceDocument document) {
        ReportSourceDocumentDTO sourceDTO = ModelMapperUtil.getMapper().map(document, ReportSourceDocumentDTO.class);
        sourceDTO.matches = document.getMatches()
                                    .stream()
                                    .map(ReportTargetDocumentDTO::from)
                                    .toList();
        return sourceDTO;
    }
}
