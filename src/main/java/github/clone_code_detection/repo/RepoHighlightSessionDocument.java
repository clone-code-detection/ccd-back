package github.clone_code_detection.repo;

import github.clone_code_detection.entity.highlight.document.HighlightSessionDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface RepoHighlightSessionDocument extends JpaRepository<HighlightSessionDocument, UUID> {
    //    Collection<HighlightSessionDocument.HighlightSessionProjection> getAllByUserId(UUID user_id);
    Collection<HighlightSessionDocument.HighlightSessionProjection> getAllByUser_Id(UUID user_id);
}
