package github.clone_code_detection.repo;

import github.clone_code_detection.entity.moodle.RelationSubmissionSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepoRelationSubmissionSession extends JpaRepository<RelationSubmissionSession, Long> {}