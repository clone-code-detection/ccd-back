package github.clone_code_detection.entity.authenication;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor

@Entity
@Table(name = "forgot-password-token", schema = "authen")
public class ForgotPasswordToken {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "expiration")
    @Builder.Default
    @Temporal(TemporalType.TIMESTAMP)
    private ZonedDateTime expiration = ZonedDateTime.now(ZONE);

    @Column(name = "token")
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserImpl user;

    public boolean isValid() {
        ZonedDateTime now = ZonedDateTime.now(ForgotPasswordToken.ZONE);
        return expiration != null && expiration.isAfter(now);
    }

    public void expireNow() {
        this.expiration = ZonedDateTime.now(ZONE);
    }
}
