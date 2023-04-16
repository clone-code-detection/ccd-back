package github.clone_code_detection.repo;

import github.clone_code_detection.entity.fs.FileDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RepoFileDocument extends JpaRepository<FileDocument, UUID> {
}
