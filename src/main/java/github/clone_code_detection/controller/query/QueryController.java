package github.clone_code_detection.controller.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.clone_code_detection.entity.highlight.document.SimilarityReport;
import github.clone_code_detection.entity.highlight.dto.SimilarityReportDetailDTO;
import github.clone_code_detection.entity.highlight.dto.SimilarityReportInfoDTO;
import github.clone_code_detection.entity.highlight.request.SimilarityDetectRequest;
import github.clone_code_detection.entity.query.QueryInstruction;
import github.clone_code_detection.service.query.ServiceIntraProjectQuery;
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
    private static final ObjectMapper mapper = new ObjectMapper();
    private final ServiceQuery serviceQuery;
    private final ServiceIntraProjectQuery serviceIntraProjectQuery;

    @Autowired
    public QueryController(ServiceQuery serviceQuery, ServiceIntraProjectQuery serviceIntraProjectQuery) {
        this.serviceQuery = serviceQuery;
        this.serviceIntraProjectQuery = serviceIntraProjectQuery;
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
                                                           defaultValue = "70%") String minimumShouldMatch,
                                                   @RequestParam(value = "author", required = false,
                                                           defaultValue = "anonymous") String author,
                                                   @RequestParam(value = "origin", required = false,
                                                           defaultValue = "local") String origin,
                                                   @RequestParam(value = "origin_link", required = false,
                                                           defaultValue = "") String originLink,
                                                   @RequestParam(value = "name", required = false,
                                                           defaultValue = "") String reportName) {
        ObjectNode meta = mapper.createObjectNode();
        meta.put("author", author);
        meta.put("origin", origin);
        meta.put("origin_link", originLink);
        if (reportName.equals(""))
            reportName = FileSystemUtil.getFileName(source);
        QueryInstruction queryInstruction = QueryInstruction.builder()
                .type(type)
                .minimumShouldMatch(minimumShouldMatch)
                .build();
        return serviceQuery.detectSync(source, queryInstruction, reportName, meta);
    }

    @PostMapping(path = "/detect",
            consumes = {MediaType.APPLICATION_JSON_VALUE,
                    MediaType.MULTIPART_FORM_DATA_VALUE,
                    MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public SimilarityReportInfoDTO createSimilarityReport(@RequestParam("source") MultipartFile source,
                                                          @RequestParam(value = "type", required = false,
                                                                  defaultValue = "1") Integer type,
                                                          @RequestParam(value = "minimum_should_match",
                                                                  required = false,
                                                                  defaultValue = "70%") String minimumShouldMatch,
                                                          @RequestParam(value = "author", required = false,
                                                                  defaultValue = "anonymous") String author,
                                                          @RequestParam(value = "origin", required = false,
                                                                  defaultValue = "local") String origin,
                                                          @RequestParam(value = "origin_link", required = false,
                                                                  defaultValue = "") String originLink,
                                                          @RequestParam(value = "name", required = false,
                                                                  defaultValue = "") String reportName) {
        ObjectNode meta = mapper.createObjectNode();
        meta.put("author", author);
        meta.put("origin", origin);
        meta.put("origin_link", originLink);
        if (reportName.equals(""))
            reportName = FileSystemUtil.getFileName(source);
        QueryInstruction queryInstruction = QueryInstruction.builder()
                .type(type)
                .minimumShouldMatch(minimumShouldMatch)
                .build();
        // Validate and extract file from source
        FileSystemUtil.validate(source);
        // Create highlight session request
        SimilarityDetectRequest request = SimilarityDetectRequest.builder()
                .reportName(FileSystemUtil.getFileName(source))
                .sources(FileSystemUtil.extractDocuments(source, meta))
                .author(author)
                .link(originLink)
                .origin(origin).reportName(reportName)
                .build();
        SimilarityReport similarityReport = serviceQuery.createSimilarityReport(request, queryInstruction);
        return SimilarityReportInfoDTO.from(similarityReport);
    }

    @PostMapping(path = "/detect_intra", consumes = {MediaType.APPLICATION_JSON_VALUE,
            MediaType.MULTIPART_FORM_DATA_VALUE,
            MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public void queryIntraProject(@RequestParam("source") MultipartFile[] source,
                                  @RequestParam(value = "name") String projectName,
                                  @RequestParam(value = "note", required = false) String note
    ) {
        QueryInstruction queryInstruction = QueryInstruction.builder()
                .type(1)
                .build();
        ServiceIntraProjectQuery.IntraProjectQueryRequest request = ServiceIntraProjectQuery.IntraProjectQueryRequest.builder()
                .files(source)
                .note(note)
                .projectName(projectName)
                .build();
        serviceIntraProjectQuery.queryIntraProject(request, queryInstruction);
    }
}