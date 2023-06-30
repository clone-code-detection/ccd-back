package github.clone_code_detection.repo;

import github.clone_code_detection.entity.authenication.ForgotPasswordToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepoResetPasswordToken extends JpaRepository<ForgotPasswordToken, UUID> {
    Optional<ForgotPasswordToken> findByToken(String token);
    Optional<ForgotPasswordToken> deleteAllByUser_Id(UUID uuid);
}
