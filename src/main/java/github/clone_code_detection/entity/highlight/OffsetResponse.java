package github.clone_code_detection.entity.highlight;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.data.util.Pair;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;

@Getter
public class OffsetResponse {
    @NotNull
    private Integer startSnippetOffset;
    @NotNull
    private Integer endSnippetOffset;
    @NotNull
    private List<Pair<Integer, Integer>> matches;

    private OffsetResponse() {}

    /**
     * @param response from highlight-plugin
     *                 {@code @formatResponse} $startSnippetOffset:$matches:$endSnippetOffset
     *                 {@code @formatMatches} $start-$end
     * @link https://github.com/wikimedia/search-highlighter
     */
    public static OffsetResponse fromString(String response) {
        OffsetResponse builder = new OffsetResponse();
        String[] strings = response.split(":");
        assert strings.length == 3 : "Invalid response format";
        builder.startSnippetOffset = parseInt(strings[0]);
        builder.endSnippetOffset = parseInt(strings[2]);
        builder.matches = OffsetResponse.matchesFromString(strings[1]);

        return builder;
    }

    @Validated
    @NonNull
    public OffsetResponse union(@NotNull @Valid OffsetResponse other) {
        assert this.startSnippetOffset != null;
        assert this.endSnippetOffset != null;
        assert !CollectionUtils.isEmpty(this.matches);
        OffsetResponse offsetResponse = new OffsetResponse();
        offsetResponse.startSnippetOffset = Math.min(this.startSnippetOffset, other.startSnippetOffset);
        offsetResponse.endSnippetOffset = Math.max(this.endSnippetOffset, other.endSnippetOffset);
        offsetResponse.matches = new ArrayList<>(this.matches);
        offsetResponse.matches.addAll(other.matches);
        return offsetResponse;
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
        return Pair.of(parseInt(arr[0]), parseInt(arr[1]));
    }
}