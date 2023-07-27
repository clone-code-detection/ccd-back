package github.clone_code_detection.repo.intra_project;

import github.clone_code_detection.entity.highlight_intra.document.dao.IntraProjectReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface RepoIntraProjectReport extends JpaRepository<IntraProjectReport, UUID> {
    Collection<IntraProjectReport> getAllByUserId(UUID userId);
}
