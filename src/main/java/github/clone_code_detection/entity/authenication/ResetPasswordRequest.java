package github.clone_code_detection.entity.authenication;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "token")
    private String token;

    /**
     * @implNote https://stackoverflow.com/questions/19605150/regex-for-password-must-contain-at-least-eight-characters-at-least-one-number-a
     */
    @NotBlank(message = "Password must not be blank")
    @Pattern(regexp = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$",
            message = "Password must has minimum eight characters, at least one letter, one number and one special character")
    private String password;

    @NotBlank(message = "Repeated password must not be blank")
    public String repeat;
}
