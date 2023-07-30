package github.clone_code_detection.entity.highlight.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.document.ReportTargetDocument;
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
public class ReportTargetDocumentDTO {
    @JsonProperty
    private UUID id;

    @JsonProperty("target")
    private FileDocument target;


    public static ReportTargetDocumentDTO from(ReportTargetDocument document) {
        return ModelMapperUtil.getMapper()
                .map(document, ReportTargetDocumentDTO.class);
    }
}
