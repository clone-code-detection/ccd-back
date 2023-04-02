package github.clone_code_detection.service.highlight;

import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.service.index.ServiceIndex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nonnull;

@Service
@Validated
public class ServiceHighlight2 {
    private final ServiceIndex serviceIndex;

    @Autowired
    public ServiceHighlight2(ServiceIndex serviceIndex) {
        this.serviceIndex = serviceIndex;
    }

    @Transactional
    public void test(@Nonnull MultipartFile file, @Nonnull IndexInstruction indexInstruction) {
        // index
        serviceIndex.indexAllDocuments(file, indexInstruction);
    }
}