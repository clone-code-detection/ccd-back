package github.clone_code_detection.entity.moodle.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoodleLinkRequest {
    @NotNull
    private String username;
    @NotNull
    private String password;
    @NotNull
    private String moodleUrl;
}
