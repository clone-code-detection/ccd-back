package github.clone_code_detection.repo;

import github.clone_code_detection.entity.highlight.document.HighlightSessionDocument;
import github.clone_code_detection.entity.highlight.document.HighlightSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface RepoHighlightSessionDocument extends JpaRepository<HighlightSessionDocument, UUID> {
    @Transactional
    @Modifying
    @Query("update HighlightSessionDocument h set h.status = ?1 where h.id = ?2")
    void updateStatusByIdEquals(@NonNull HighlightSessionStatus status, @NonNull UUID id);
    Collection<HighlightSessionDocument.HighlightSessionProjection> getAllByUserId(UUID userId);
}
