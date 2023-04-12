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

        Map<String, List<Integer[]>> targetMatches = new HashMap<>();
        String targetFileContentAsString = targetFile.getContentAsString();
        String sourceFileContentAsString = sourceFile.getContentAsString();
        for (HighlightMatchDocument match : document.getMatches()) {
            String textMatch = targetFileContentAsString.substring(match.getStart(), match.getEnd());
            if (!targetMatches.containsKey(textMatch)) targetMatches.put(textMatch, new ArrayList<>());
            targetMatches.get(textMatch)
                         .add(new Integer[]{match.getStart(), match.getEnd()});
        }
        var sourceMatches = getAllMatchesFor(sourceFileContentAsString, targetMatches.keySet());

        List<HighlightWordMatchDTO> matches = new ArrayList<>();
        for (Map.Entry<String, List<Integer[]>> entry : sourceMatches.entrySet()) {
            HighlightWordMatchDTO wordMatchDTO = HighlightWordMatchDTO.builder()
                                                                      .word(entry.getKey())
                                                                      .sourceMatches(entry.getValue())
                                                                      .targetMatches(targetMatches.get(entry.getKey()))
                                                                      .build();
            matches.add(wordMatchDTO);
        }
        return HighlightSingleMatchDTO.builder()
                                      .source(sourceFile.getContentAsString())
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
