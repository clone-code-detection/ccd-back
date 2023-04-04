package github.clone_code_detection.repo;

import github.clone_code_detection.entity.highlight.report.HighlightSessionDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RepoHighlightSessionDocument extends JpaRepository<HighlightSessionDocument, UUID> {
}
