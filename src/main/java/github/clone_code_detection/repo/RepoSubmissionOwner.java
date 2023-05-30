package github.clone_code_detection.repo;

import github.clone_code_detection.entity.moodle.SubmissionOwner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepoSubmissionOwner extends JpaRepository<SubmissionOwner, Long> {
    SubmissionOwner findByReferenceOwnerId(long ownerId);
}