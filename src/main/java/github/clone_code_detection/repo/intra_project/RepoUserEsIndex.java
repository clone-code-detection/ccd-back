package github.clone_code_detection.repo.intra_project;

import github.clone_code_detection.entity.highlight_intra.document.IntraProjectReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RepoUserEsIndex extends JpaRepository<IntraProjectReport, UUID> {
}
