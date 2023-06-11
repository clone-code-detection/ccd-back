package github.clone_code_detection.repo;

import github.clone_code_detection.entity.moodle.UserReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RepoUserReference extends JpaRepository<UserReference, Long> {
    UserReference findFirstByInternalUserId(UUID internalUserId);
}
