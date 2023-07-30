package github.clone_code_detection.controller.highlight;

import github.clone_code_detection.entity.highlight_intra.document.dto.IntraProjectReportDTO;
import github.clone_code_detection.entity.highlight_intra.document.dto.IntraProjectReportOverviewDTO;
import github.clone_code_detection.service.query.ServiceIntraProjectQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/intra_highlight")
public class IntraProjectHighlightController {
    private final ServiceIntraProjectQuery serviceIntraProjectQuery;

    @Autowired
    public IntraProjectHighlightController(ServiceIntraProjectQuery serviceIntraProjectQuery) {
        this.serviceIntraProjectQuery = serviceIntraProjectQuery;
    }

    @GetMapping(path = "/get-session-by-id/{id}")
    @ResponseStatus(HttpStatus.OK)
    public IntraProjectReportDTO getReportById(@PathVariable(name = "id") String id) {
        return serviceIntraProjectQuery.getSimilarityReportById(id);
    }

    @GetMapping(path = "/all-session")
    @ResponseStatus(HttpStatus.OK)
    public List<IntraProjectReportOverviewDTO> getAllSimilarityReports() {
        return serviceIntraProjectQuery.getAllReports();
    }
}
