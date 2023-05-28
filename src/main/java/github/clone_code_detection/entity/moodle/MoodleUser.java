package github.clone_code_detection.entity.moodle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoodleUser {
    private String token;
    private long userId;
}
