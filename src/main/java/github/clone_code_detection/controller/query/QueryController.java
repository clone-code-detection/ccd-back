package github.clone_code_detection.controller.query;

import github.clone_code_detection.service.query.ServiceQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping("/api/query")
public class QueryController {
    private final ServiceQuery serviceQuery;

    @Autowired
    public QueryController(ServiceQuery serviceQuery) {
        this.serviceQuery = serviceQuery;
    }

    @RequestMapping(path = "/single-source-match/overview/{id}", method = RequestMethod.GET)
    public Collection<ServiceQuery.TargetMatchOverview> handle(@PathVariable String id) {
        return serviceQuery.handle(id);
    }
}