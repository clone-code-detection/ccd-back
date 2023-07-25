package github.clone_code_detection.controller.highlight;

import github.clone_code_detection.entity.highlight.dto.ReportSourceDocumentDTO;
import github.clone_code_detection.entity.highlight.dto.SimilarityReportDetailDTO;
import github.clone_code_detection.entity.highlight.dto.SimilarityReportOverviewDTO;
import github.clone_code_detection.service.highlight.ServiceAdvancedHighlight;
import github.clone_code_detection.service.highlight.ServiceHighlight;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping(path = "/advance-source-highlight/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Collection<ServiceAdvancedHighlight.ExtendHighlightReturn> advanceHighlightBySourceId(@PathVariable(name = "id") String id) {
        return advanceServiceHighlight.advanceHighlightBySourceId(id);
    }
}
