package github.clone_code_detection.controllers;

import co.elastic.clients.util.Pair;
import github.clone_code_detection.entity.RenderDocument;
import github.clone_code_detection.entity.ResponseUnified;
import github.clone_code_detection.service.ServiceRender;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(path = "/api/render")
public class RenderController {
    final ServiceRender serviceRender;

    @Autowired
    public RenderController(ServiceRender serviceRender) {
        this.serviceRender = serviceRender;
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(path = "/{id}", method = RequestMethod.GET)
    public ResponseUnified<RenderDocument> getRenderDocument(@PathVariable("id") int id) {
        log.info("Render report id: {}", id);
        Pair<RenderDocument, Integer> response = serviceRender.getRenderDocument(id);
        if (response == null)
            return new ResponseUnified<>("Can not get render data", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
        return switch (response.value()) {
            case 0 -> new ResponseUnified<>("Success", HttpServletResponse.SC_OK, response.key());
            case 1 -> new ResponseUnified<>("Report not found", HttpServletResponse.SC_BAD_REQUEST, null);
            case 2 -> new ResponseUnified<>("Load report failed", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
            default -> new ResponseUnified<>("unspecified", HttpServletResponse.SC_EXPECTATION_FAILED, null);
        };
    }
}
