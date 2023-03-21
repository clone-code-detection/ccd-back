package github.clone_code_detection.entity.authenication;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SignUpRequest {
    @NotNull
    private String username;
    @NotNull
    private String password;
    @NotNull
    private String repeatPassword;
}
