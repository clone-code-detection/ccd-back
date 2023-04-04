package github.clone_code_detection.service.index;

import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.index.IndexInstruction;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;

@Validated
@Transactional
public interface IServiceIndex {
    Collection<FileDocument> indexAllDocuments(MultipartFile file, IndexInstruction body);
}
