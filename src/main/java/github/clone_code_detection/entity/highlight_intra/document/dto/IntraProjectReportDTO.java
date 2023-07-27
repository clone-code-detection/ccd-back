package github.clone_code_detection.entity.highlight_intra.document.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.highlight.dto.SimilarityReportDetailDTO;
import github.clone_code_detection.entity.highlight_intra.document.dao.AuthorReport;
import github.clone_code_detection.entity.highlight_intra.document.dao.IntraProjectReport;
import github.clone_code_detection.util.ModelMapperUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

import java.sql.Time;
import java.util.Collection;
import java.util.UUID;

@Builder
@Data

@NoArgsConstructor
@AllArgsConstructor
public class IntraProjectReportDTO {
    @JsonProperty
    private UUID id;

    @JsonProperty
    private Time created;

    @JsonProperty
    private String name;

    @JsonProperty("projects")
    private Collection<IntraProjectAuthorDTO> authors;

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IntraProjectAuthorDTO {
        @JsonProperty("author")
        private String author;

        @JsonProperty(value = "total_files")
        int totalFiles;

        @JsonProperty(value = "most_likely_match")
        String otherAuthor;

        @JsonProperty(value = "total_other_matches")
        Integer totalMatches;

        @JsonProperty("matches")
        private Collection<SimilarityReportDetailDTO.Document> reports;

        public static IntraProjectAuthorDTO from(AuthorReport report) {
            ModelMapper mapper = ModelMapperUtil.getMapper();
            IntraProjectAuthorDTO intraProjectAuthorDTO = mapper.map(report, IntraProjectAuthorDTO.class);
            intraProjectAuthorDTO.reports = report.getSources().stream().map(SimilarityReportDetailDTO.Document::from).toList();
            return intraProjectAuthorDTO;
        }
    }

    public static IntraProjectReportDTO from(IntraProjectReport document) {
        ModelMapper mapper = ModelMapperUtil.getMapper();
        IntraProjectReportDTO res = mapper
                .map(document, IntraProjectReportDTO.class);
        res.authors = document.getAuthorReports().stream().map(IntraProjectAuthorDTO::from).toList();
        return res;
    }
}
