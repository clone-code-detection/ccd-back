package github.clone_code_detection.controllers;

import github.clone_code_detection.entity.RenderDocument;
import github.clone_code_detection.entity.ResponseUnified;
import github.clone_code_detection.service.ServiceRender;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.MissingResourceException;

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
        RenderDocument response;
        try {
            response = serviceRender.getRenderDocument(id);
            if (response == null)
                throw new NullPointerException("Can not get render data");
            return new ResponseUnified<>("Success", HttpServletResponse.SC_OK, response);
        } catch (Exception e) {
            if (e.getClass() == SQLException.class || e.getClass() == NegativeArraySizeException.class)
                new ResponseUnified<>(e.getMessage(), HttpServletResponse.SC_BAD_REQUEST, null);
            if (e.getClass() == MissingResourceException.class || e.getClass() == NullPointerException.class)
                return new ResponseUnified<>(e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
            return new ResponseUnified<>("Success", HttpServletResponse.SC_SERVICE_UNAVAILABLE, null);
        }
    }
}
