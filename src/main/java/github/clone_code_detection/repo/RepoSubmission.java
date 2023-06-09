package github.clone_code_detection.repo;

import github.clone_code_detection.entity.moodle.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepoSubmission extends JpaRepository<Submission, Long> {
    Submission findFirstByReferenceSubmissionId(long submissionId);

    List<Submission> findAllByIdIn(List<Long> submissionIds);
}