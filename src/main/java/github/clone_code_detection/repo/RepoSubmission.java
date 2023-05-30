package github.clone_code_detection.repo;

import github.clone_code_detection.entity.moodle.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepoSubmission extends JpaRepository<Submission, Long> {
    Submission findByReferenceSubmissionId(long submissionId);
}