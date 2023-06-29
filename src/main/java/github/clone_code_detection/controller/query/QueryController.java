package github.clone_code_detection.controller.query;

import github.clone_code_detection.entity.highlight.document.SimilarityReport;
import github.clone_code_detection.entity.highlight.dto.SimilarityReportDetailDTO;
import github.clone_code_detection.entity.highlight.dto.SimilarityReportInfoDTO;
import github.clone_code_detection.entity.highlight.request.SimilarityDetectRequest;
import github.clone_code_detection.entity.query.QueryInstruction;
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

    @Autowired
    public QueryController(ServiceQuery serviceQuery, ServiceHighlight serviceHighlight) {
        this.serviceQuery = serviceQuery;
    }

    @GetMapping(path = "/single-source-match/overview/{id}")
    public Collection<ServiceQuery.TargetMatchOverview> handle(@PathVariable String id) {
        return serviceQuery.handle(id);
    }

    @PostMapping(path = "/query",
                 consumes = {MediaType.APPLICATION_JSON_VALUE,
                             MediaType.MULTIPART_FORM_DATA_VALUE,
                             MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public SimilarityReportDetailDTO queryDocument(@RequestParam("source") MultipartFile source,
                                                   @RequestParam(value = "type",
                                                                 required = false,
                                                                 defaultValue = "1") Integer type,
                                                   @RequestParam(value = "minimum_should_match",
                                                                 required = false,
                                                                 defaultValue = "70%") String minimumShouldMatch) {
        QueryInstruction queryInstruction = QueryInstruction.builder()
                                                            .type(type)
                                                            .minimumShouldMatch(minimumShouldMatch)
                                                            .build();
        return serviceQuery.detectSync(source, queryInstruction);
    }

    @PostMapping(path = "/detect",
                 consumes = {MediaType.APPLICATION_JSON_VALUE,
                             MediaType.MULTIPART_FORM_DATA_VALUE,
                             MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public SimilarityReportInfoDTO createSimilarityReport(@RequestParam("source") MultipartFile source,
                                                          @RequestParam(value = "type",
                                                                        required = false,
                                                                        defaultValue = "1") Integer type,
                                                          @RequestParam(value = "minimum_should_match",
                                                                        required = false,
                                                                        defaultValue = "70%") String minimumShouldMatch) {
        QueryInstruction queryInstruction = QueryInstruction.builder()
                                                            .type(type)
                                                            .minimumShouldMatch(minimumShouldMatch)
                                                            .build();
        // Validate and extract file from source
        FileSystemUtil.validate(source);
        // Create highlight session request
        SimilarityDetectRequest request = SimilarityDetectRequest.builder()
                                                                 .reportName(FileSystemUtil.getFileName(source))
                                                                 .sources(FileSystemUtil.extractDocuments(source))
                                                                 .build();
        SimilarityReport similarityReport = serviceQuery.createSimilarityReport(request, queryInstruction);
        return SimilarityReportInfoDTO.from(similarityReport);
    }
}