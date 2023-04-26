package github.clone_code_detection.entity.highlight.report;

import github.clone_code_detection.entity.highlight.document.HighlightSessionDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HighlightSessionOverviewDTO {
    @Builder.Default
    Set<LanguageCount> set = new HashSet<>();
    Collection<HighlightSessionDocument.HighlightSessionProjection> collection;

    public static HighlightSessionOverviewDTO from(Collection<HighlightSessionDocument.HighlightSessionProjection> sessions) {
        HighlightSessionOverviewDTO dto = HighlightSessionOverviewDTO.builder().collection(sessions).build();
        // Create summary for languages
        Map<String, Integer> languageCount = new HashMap<>();
        sessions.forEach(session -> {
            if (languageCount.get(session.getMainLanguage()) == null)
                languageCount.put(session.getMainLanguage(), 1);
            else
                languageCount.put(session.getMainLanguage(), languageCount.get(session.getMainLanguage()) + 1);
        });
        // Assign map into set
        dto.set = languageCount.entrySet()
                .stream()
                .map(entry -> LanguageCount.builder().language(entry.getKey()).count(entry.getValue()).build())
                .collect(Collectors.toSet());
        return dto;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class LanguageCount {
        String language;
        int count;
    }
}
