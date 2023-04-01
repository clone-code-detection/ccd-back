package github.clone_code_detection.controllers.query;

import github.clone_code_detection.entity.ElasticsearchDocument;
import github.clone_code_detection.entity.query.QueryDocument;
import github.clone_code_detection.service.query.ServiceQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
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

    @RequestMapping(path = "/form", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_JSON_VALUE})
    public Collection<ElasticsearchDocument> queryDocument(@RequestBody QueryDocument queryDocument) {
        return serviceQuery.search(queryDocument);
    }
}