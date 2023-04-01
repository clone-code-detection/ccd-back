package github.clone_code_detection.entity.highlight;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.springframework.data.util.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Data
class OffsetResponse {
    private Integer startSnippetOffset;
    private Integer endSnippetOffset;
    private List<Pair<Integer, Integer>> matches;

    private OffsetResponse() {}

    /**
     * @param response from highlight-plugin
     *                 {@code @formatResponse} $startSnippetOffset:$matches:$endSnippetOffset
     *                 {@code @formatMatches} $start-$end
     * @link https://github.com/wikimedia/search-highlighter
     */
    public static OffsetResponse fromString(String response) {
        OffsetResponseBuilder builder = OffsetResponse.builder();
        String[] strings = response.split(":");
        assert strings.length == 3 : "Invalid response format";
        builder.startSnippetOffset(Integer.parseInt(strings[0]));
        builder.endSnippetOffset(Integer.parseInt(strings[2]));
        builder.matches(OffsetResponse.matchesFromString(strings[1]));

        return builder.build();
    }

    private static List<Pair<Integer, Integer>> matchesFromString(String matches) {
        return Arrays.stream(matches.split(","))
                     .map(OffsetResponse::matchFromString)
                     .collect(Collectors.toList());
    }

    @NonNull
    private static Pair<Integer, Integer> matchFromString(String match) {
        String[] arr = match.split("-");
        assert arr.length == 2;
        return Pair.of(Integer.parseInt(arr[0]), Integer.parseInt(arr[1]));
    }
}