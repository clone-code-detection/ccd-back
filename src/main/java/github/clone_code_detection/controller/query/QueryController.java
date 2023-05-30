package github.clone_code_detection.controller.query;

import github.clone_code_detection.entity.ElasticsearchDocument;
import github.clone_code_detection.entity.query.QueryInstruction;
import github.clone_code_detection.service.query.ServiceQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/query")
public class QueryController {
    private final ServiceQuery serviceQuery;

    @Autowired
    public QueryController(ServiceQuery serviceQuery) {
        this.serviceQuery = serviceQuery;
    }

    @RequestMapping(path = "/form", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE})
    public Collection<ElasticsearchDocument> queryDocument(@RequestBody QueryInstruction queryInstruction) {
        return serviceQuery.search(queryInstruction);
    }

    @RequestMapping(path = "/single-source-match/overview/{id}", method = RequestMethod.GET)
    public Collection<ServiceQuery.TargetMatchOverview> handle(@PathVariable String id) {
        return serviceQuery.handle(id);
    }
}