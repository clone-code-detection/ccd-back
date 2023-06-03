package github.clone_code_detection.repo;

import github.clone_code_detection.entity.moodle.RelationSubmissionReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepoRelationSubmissionReport extends JpaRepository<RelationSubmissionReport, Long> {}