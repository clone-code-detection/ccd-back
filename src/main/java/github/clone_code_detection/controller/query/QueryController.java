package github.clone_code_detection.controller.query;

import github.clone_code_detection.entity.highlight.document.SimilarityReport;
import github.clone_code_detection.entity.highlight.dto.SimilarityReportDetailDTO;
import github.clone_code_detection.entity.highlight.dto.SimilarityReportInfoDTO;
import github.clone_code_detection.entity.highlight.request.SimilarityDetectRequest;
import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.service.highlight.ServiceHighlight;
import github.clone_code_detection.service.query.ServiceQuery;
import github.clone_code_detection.util.FileSystemUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;

@RestController
@RequestMapping("/api/query")
public class QueryController {
    private final ServiceQuery serviceQuery;
    private final ServiceHighlight serviceHighlight;

    @Autowired
    public QueryController(ServiceQuery serviceQuery, ServiceHighlight serviceHighlight) {
        this.serviceQuery = serviceQuery;
        this.serviceHighlight = serviceHighlight;
    }

    @RequestMapping(path = "/single-source-match/overview/{id}", method = RequestMethod.GET)
    public Collection<ServiceQuery.TargetMatchOverview> handle(@PathVariable String id) {
        return serviceQuery.handle(id);
    }

    @PostMapping(path = "/query", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public SimilarityReportDetailDTO queryDocument(@RequestParam("source") MultipartFile source) {
        return serviceQuery.detectSync(source, IndexInstruction.getDefaultInstruction());
    }

    @PostMapping(path = "/detect", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public SimilarityReportInfoDTO createSimilarityReport(@RequestParam("source") MultipartFile source) {
        // Validate and extract file from source
        FileSystemUtil.validate(source);
        // Create highlight session request
        SimilarityDetectRequest request = SimilarityDetectRequest.builder()
                                                                 .reportName(FileSystemUtil.getFileName(source))
                                                                 .sources(FileSystemUtil.extractDocuments(source))
                                                                 .build();
        SimilarityReport similarityReport = serviceQuery.createSimilarityReport(request,
                IndexInstruction.getDefaultInstruction());
        return SimilarityReportInfoDTO.from(similarityReport);
    }
}