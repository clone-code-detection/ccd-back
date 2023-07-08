package github.clone_code_detection.entity.authenication;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

import static github.clone_code_detection.entity.authenication.ForgotPasswordToken.ZONE;

@Data
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "account-activation-code", schema = "authen")
public class AccountActivationToken {
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "activation_code")
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private UserImpl user;

    @Column(name = "expiration")
    @Temporal(TemporalType.TIMESTAMP)
    private ZonedDateTime expiration = ZonedDateTime.now(ZONE).plusMinutes(10);

    public boolean isValid() {
        ZonedDateTime now = ZonedDateTime.now(ForgotPasswordToken.ZONE);
        return expiration != null && expiration.isAfter(now);
    }

    public void expireNow() {
        this.expiration = ZonedDateTime.now(ZONE);
    }
}
