package github.clone_code_detection.entity.highlight.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.highlight.document.HighlightMatchDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSingleDocument;
import lombok.Builder;
import lombok.Data;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Builder
@Data
public class HighlightSingleMatchDTO {
    private UUID id;

    @JsonProperty("source")
    private String source;

    @JsonProperty("target")
    private String target;

    @JsonProperty("matches")
    @Builder.Default
    private List<HighlightWordMatchDTO> matches = new ArrayList<>();

    public static HighlightSingleMatchDTO fromHighlightSingleMatchDTO(HighlightSingleDocument document) {
        FileDocument targetFile = document.getTarget();
        FileDocument sourceFile = document.getSource();

        String targetFileContentAsString = targetFile.getContentAsString();
        String sourceFileContentAsString = sourceFile.getContentAsString();
        List<HighlightWordMatchDTO> matches = new ArrayList<>();
        return HighlightSingleMatchDTO.builder()
                                      .source(sourceFileContentAsString)
                                      .target(targetFileContentAsString)
                                      .matches(matches)
                                      .build();
    }

    private static List<Integer[]> extractMatches(Collection<HighlightMatchDocument> matches) {
        return matches.stream()
                      .map(highlightMatch -> new Integer[]{highlightMatch.getStart(), highlightMatch.getEnd()})
                      .toList();
    }

    private static Map<String, List<Integer[]>> getAllMatchesFor(String text, Set<String> patterns) {
        Map<String, List<Integer[]>> map = new HashMap<>();
        for (String pattern : patterns) {
            Pattern word = Pattern.compile(pattern);
            Matcher match = word.matcher(text);
//            List<Integer[]> list = map.put(pattern, new ArrayList<>());
            List<Integer[]> list = new ArrayList<>();
            map.put(pattern, list);
            while (match.find()) {
                list.add(new Integer[]{match.start(), match.end()});
            }
        }
        return map;
    }
}
