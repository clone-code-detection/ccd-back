package github.clone_code_detection.controller.highlight;

import github.clone_code_detection.entity.highlight.dto.ReportSourceDocumentDTO;
import github.clone_code_detection.entity.highlight.dto.SimilarityReportDetailDTO;
import github.clone_code_detection.entity.highlight.dto.SimilarityReportInfoDTO;
import github.clone_code_detection.entity.highlight.dto.SimilarityReportOverviewDTO;
import github.clone_code_detection.entity.highlight.request.SimilarityDetectRequest;
import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.service.highlight.ServiceAdvancedHighlight;
import github.clone_code_detection.service.highlight.ServiceHighlight;
import github.clone_code_detection.util.FileSystemUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;


@RestController
@RequestMapping("/api/highlight")
public class HighlightController {
    private final ServiceHighlight serviceHighlight;
    private final ServiceAdvancedHighlight advanceServiceHighlight;

    @Autowired
    public HighlightController(ServiceHighlight serviceHighlightTest, ServiceAdvancedHighlight advanceServiceHighlight) {
        this.serviceHighlight = serviceHighlightTest;
        this.advanceServiceHighlight = advanceServiceHighlight;
    }

    @PostMapping(path = "/query",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public SimilarityReportDetailDTO queryDocument(@RequestParam("source") MultipartFile source) {
        return serviceHighlight.detectSync(source, IndexInstruction.getDefaultInstruction());
    }

    @GetMapping(path = "/get-session-by-id/{id}")
    @ResponseStatus(HttpStatus.OK)
    public SimilarityReportDetailDTO getReportById(@PathVariable(name = "id") String id) {
        return serviceHighlight.getSimilarityReportById(id);
    }

    @GetMapping(path = "/all-session")
    @ResponseStatus(HttpStatus.OK)
    public SimilarityReportOverviewDTO getAllSimilarityReports() {
        return SimilarityReportOverviewDTO.from(serviceHighlight.getAllReports());
    }

    @GetMapping(path = "/get-match-source/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ReportSourceDocumentDTO getReportSourceDocumentById(@PathVariable(name = "id") String id) {
        return serviceHighlight.getReportSourceDocumentById(id);
    }

    @PostMapping(path = "/detect",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public SimilarityReportInfoDTO createSimilarityReport(@RequestParam("source") MultipartFile source) {
        // Validate and extract file from source
        FileSystemUtil.validate(source);
        // Create highlight session request
        SimilarityDetectRequest request = SimilarityDetectRequest.builder()
                                                                 .reportName(FileSystemUtil.getFileName(source))
                                                                 .sources(FileSystemUtil.extractDocuments(source))
                                                                 .build();
        return SimilarityReportInfoDTO.from(serviceHighlight.createSimilarityReport(request, IndexInstruction.getDefaultInstruction()));
    }

    @GetMapping(path = "/advance-target-highlight/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Collection<ServiceHighlight.HighlightReturn> advanceHighlightById(@PathVariable(name = "id") String id) {
        return serviceHighlight.handleAdvancedHighlightById(id);
    }

    @GetMapping(path = "/advance-source-highlight/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Collection<ServiceAdvancedHighlight.ExtendHighlightReturn> advanceHighlightBySourceId(@PathVariable(name = "id") String id) {
        return advanceServiceHighlight.advanceHighlightBySourceId(id);
    }
}
