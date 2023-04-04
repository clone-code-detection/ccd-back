package github.clone_code_detection.entity.highlight.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.fs.FileDocument;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.util.Pair;

import java.sql.Time;
import java.util.*;
import java.util.stream.Collectors;

@Builder
@Data
public class HighlightSessionReportDTO {
    @JsonProperty
    private UUID session;

    @JsonProperty
    private Time created;

    @JsonProperty
    private Collection<HighlightReportDTO> reports;

    public static HighlightSessionReportDTO from(HighlightSessionDocument sessionDocument) {
        Map<FileDocument, List<HighlightSingleDocument>> documentListMap = new HashMap<>();
        for (HighlightSingleDocument singleDocument : sessionDocument.getMatches()) {
            FileDocument source = singleDocument.getSource();
            if (!documentListMap.containsKey(source)) {
                documentListMap.put(source, new ArrayList<>());
            }
            documentListMap.get(source)
                           .add(singleDocument);
        }
        List<HighlightReportDTO> reports = new ArrayList<>();
        for (Map.Entry<FileDocument, List<HighlightSingleDocument>> fileDocumentListEntry : documentListMap.entrySet()) {
            FileDocument key = fileDocumentListEntry.getKey();
            List<HighlightSingleDocument> value = fileDocumentListEntry.getValue();
            reports.add(HighlightReportDTO.from(key, value));
        }
        return HighlightSessionReportDTO.builder()
                                        .reports(reports)
                                        .session(sessionDocument.getId())
                                        .created(sessionDocument.getCreated())
                                        .build();
    }

    /**
     * for a single source file
     */
    @Builder
    @Data
    private static class HighlightReportDTO {
        @JsonProperty("source_id")
        private UUID sourceId;
        private String source;
        private List<HighlightCloneDTO> clones;

        private static HighlightReportDTO from(FileDocument source, List<HighlightSingleDocument> listOfClones) {
            List<HighlightCloneDTO> clones = listOfClones.stream()
                                                         .map(HighlightCloneDTO::from)
                                                         .collect(Collectors.toList());
            return HighlightReportDTO.builder()
                                     .source(source.getContentAsString())
                                     .sourceId(source.getId())
                                     .clones(clones)
                                     .build();
        }
    }

    /**
     * for a single target
     */
    @Builder
    @Data
    private static class HighlightCloneDTO {
        @JsonProperty("target_id")
        private UUID targetId;
        @JsonProperty("target")
        private String target;
        private List<Pair<Integer, Integer>> matches;

        private static HighlightCloneDTO from(HighlightSingleDocument highlightSingleDocument) {
            return HighlightCloneDTO.builder()
                                    .targetId(highlightSingleDocument.getTarget()
                                                                     .getId())
                                    .target(highlightSingleDocument.getTarget()
                                                                   .getContentAsString())
                                    .matches(highlightSingleDocument.getMatches()
                                                                    .stream()
                                                                    .peek(highlightMatch -> {
                                                                        System.out.printf(highlightMatch.getId().toString());
                                                                    })
                                                                    .map(highlightMatch -> Pair.of(
                                                                            highlightMatch.getStart(),
                                                                            highlightMatch.getEnd()))
                                                                    .collect(Collectors.toList()))
                                    .build();
        }
    }
}
