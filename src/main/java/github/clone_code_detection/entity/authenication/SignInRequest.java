package github.clone_code_detection.entity.authenication;


import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@Data
public class SignInRequest {
    @NotNull
    private String email;
    @NotNull
    private String password;

    public UsernamePasswordAuthenticationToken toUsernamePasswordToken() {
        return new UsernamePasswordAuthenticationToken(this.email, this.password);
    }
}
