package github.clone_code_detection.repo;

import github.clone_code_detection.entity.highlight.document.ReportSourceDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RepoReportSourceDocument extends JpaRepository<ReportSourceDocument, UUID> {}
