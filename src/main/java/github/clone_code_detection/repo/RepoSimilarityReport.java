package github.clone_code_detection.repo;

import github.clone_code_detection.entity.highlight.document.SimilarityReport;
import github.clone_code_detection.entity.highlight.document.SimilarityReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface RepoSimilarityReport extends JpaRepository<SimilarityReport, UUID> {
    @Transactional
    @Modifying
    @Query("update SimilarityReport h set h.status = ?1 where h.id = ?2")
    void updateStatusByIdEquals(@NonNull SimilarityReportStatus status, @NonNull UUID id);

    Collection<SimilarityReport.SimilarityReportDTO> getAllByUserId(UUID userId);
}
