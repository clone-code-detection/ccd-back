package github.clone_code_detection.repo;

import github.clone_code_detection.entity.moodle.UserReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RepoUserReference extends JpaRepository<UserReference, Long> {
    UserReference findFirstByInternalUserId(UUID internalUserId);

    UserReference findFirstByToken(String token);
}