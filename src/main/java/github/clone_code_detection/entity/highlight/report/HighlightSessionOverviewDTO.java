package github.clone_code_detection.entity.highlight.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.highlight.document.HighlightSessionDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HighlightSessionOverviewDTO {
    private HighlightSessionOverviewSummary summary;
    private Collection<HighlightSessionOverviewDetail> collection;

    public static HighlightSessionOverviewDTO from(Collection<HighlightSessionDocument.HighlightSessionProjection> sessions) {
        return HighlightSessionOverviewDTO.builder()
                                          .summary(HighlightSessionOverviewSummary.builder().build())
                                          .collection(sessions.stream()
                                                              .map(HighlightSessionOverviewDetail::from)
                                                              .toList())
                                          .build();
    }

    @Builder
    @Data
    private static class HighlightSessionOverviewSummary {
        private String language;
    }

    @Builder
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class HighlightSessionOverviewDetail {
        private UUID id;
        private String name;
        private ZonedDateTime created;
        private HighlightSessionStatus status;
        @JsonProperty("total_files")
        private int totalFiles;
        @JsonProperty("matched_files")
        private int matchedFiles;

        public static HighlightSessionOverviewDetail from(HighlightSessionDocument.HighlightSessionProjection session) {
            return HighlightSessionOverviewDetail.builder()
                                                 .id(session.getId())
                                                 .name(session.getName())
                                                 .created(session.getCreated())
                                                 .status(session.getStatus())
                                                 .totalFiles(session.getMatches().size())
                                                 .matchedFiles((int) session.getMatches()
                                                                            .stream()
                                                                            .filter(match -> !match.getMatches()
                                                                                                   .isEmpty())
                                                                            .count())
                                                 .build();
        }
    }
}
