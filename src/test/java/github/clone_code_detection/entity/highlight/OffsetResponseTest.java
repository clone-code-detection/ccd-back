package github.clone_code_detection.entity.highlight;

import org.junit.jupiter.api.Test;
import org.springframework.data.util.Pair;

import static org.assertj.core.api.Assertions.assertThat;

class OffsetResponseTest {

    @Test
    void fromString() {
        String split = "0:5-11,12-17,18-23,24-25,37-38:41";
        OffsetResponse offsetResponse = OffsetResponse.fromString(split);
        String a = "\n" +
                "    public class Hello {\n" +
                "      \n" +
                "    }\n" +
                "  ";
        String[] expectedResult = new String[]{"public", "class", "Hello", "{", "}"};
        assertThat(offsetResponse.getMatches()).hasSameSizeAs(expectedResult);

        for (int i = 0; i < expectedResult.length; i++) {
            Pair<Integer, Integer> match = offsetResponse.getMatches()
                                                         .get(i);
            CharSequence charSequence = a.subSequence(match.getFirst(), match.getSecond());
            assertThat(charSequence).isEqualTo(expectedResult[i]);
        }
    }
}