package github.clone_code_detection.entity.highlight.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.highlight.document.HighlightSessionDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSingleDocument;
import lombok.Builder;
import lombok.Data;

import java.sql.Time;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Builder
@Data
public class HighlightSessionReportDTO {
    @JsonProperty
    private UUID session;

    @JsonProperty
    private Time created;

    @JsonProperty("matches")
    private Collection<UUID> reports;

    public static HighlightSessionReportDTO from(HighlightSessionDocument sessionDocument) {
        List<UUID> uuids = sessionDocument.getMatches()
                                          .stream()
                                          .map(HighlightSingleDocument::getId)
                                          .toList();
        return HighlightSessionReportDTO.builder()
                                        .reports(uuids)
                                        .session(sessionDocument.getId())
                                        .created(sessionDocument.getCreated())
                                        .build();
    }
}
