package github.clone_code_detection.entity.highlight_intra.document.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.highlight_intra.document.dao.AuthorReport;
import github.clone_code_detection.entity.highlight_intra.document.dao.IntraProjectReport;
import github.clone_code_detection.util.ModelMapperUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntraProjectReportOverviewDTO {
    private UUID id;
    private String name;
    private long created;
    private String status;

    @JsonProperty("note")
    private String note;
    @JsonProperty("total_projects")
    private long totalProjects;
    @JsonProperty("total_files")
    private int totalFiles;
    @JsonProperty("matched_files")
    private int matchedFiles;

    public static IntraProjectReportOverviewDTO from(IntraProjectReport document) {
        ModelMapper mapper = ModelMapperUtil.getMapper();
        IntraProjectReportOverviewDTO mapped = mapper.map(document, IntraProjectReportOverviewDTO.class);
        mapped.totalProjects = 0;
        mapped.created = document.getCreatedAt().toEpochSecond();
        for (AuthorReport authorReport : document.getAuthorReports()) {
            mapped.totalProjects += authorReport.getTotalFiles();
        }
        return mapped;
    }
}
