package github.clone_code_detection.repo;

import github.clone_code_detection.entity.authenication.AccountActivationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepoActivationToken extends JpaRepository<AccountActivationToken, UUID> {
    Optional<AccountActivationToken> findByCode(String code);
    Boolean existsByUser_UsernameAndExpirationAfter(String username, ZonedDateTime time);
    void deleteAllByUser_Id(UUID uuid);
}
