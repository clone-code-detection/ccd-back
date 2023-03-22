package github.clone_code_detection.entity.authenication;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SignUpRequest {
    @JsonProperty("username")
    @NotNull
    @NotBlank(message = "Username must not be blank")
    private String username;

    /**
     * @implNote https://stackoverflow.com/questions/19605150/regex-for-password-must-contain-at-least-eight-characters-at-least-one-number-a
     */
    @JsonProperty("password")
    @NotBlank(message = "Password must not be blank")
    @Pattern(regexp = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$",
            message = "Password must has minimum eight characters, at least one letter, one number and one special character")
    private String password;

    @JsonProperty("repeat_password")
//    @NotBlank(message = "Repeated password must not be blank")
    private String repeatPassword;
}