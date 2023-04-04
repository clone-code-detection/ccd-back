package github.clone_code_detection.controller.index;

import github.clone_code_detection.entity.fs.FileDocument;
import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.service.index.ServiceIndex;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;

@RestController
@RequestMapping("/api/index")
public class IndexController {
    private final ServiceIndex serviceIndex;

    public IndexController(ServiceIndex serviceIndex) {
        this.serviceIndex = serviceIndex;
    }

    @RequestMapping(path = "/zip", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public Collection<FileDocument> index(IndexInstruction instruction, @RequestParam("file") MultipartFile file) {
        return serviceIndex.indexAllDocuments(file, instruction);
    }
}
