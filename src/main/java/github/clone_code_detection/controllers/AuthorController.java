package github.clone_code_detection.controllers;

import github.clone_code_detection.entity.ResponseUnified;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/authors")
public class AuthorController {
    public static final ResponseUnified<String> SUCCESS =
            new ResponseUnified<>("success" , HttpServletResponse.SC_OK ,
                    "We are two students from Ho Chi Minh city University of Science.");
    final static List<String> authors = new ArrayList<>() {{
        add("Quan Trong Dinh");
        add("Ha Hai Nguyen");
    }};
    public static final ResponseUnified<String> OUT_OF_BOUND = new ResponseUnified<>(
            String.format("Invalid id, must be integer and in range (0, %d)" , authors.size()) ,
            HttpServletResponse.SC_BAD_REQUEST , null);

    final static String statusMessage =
            String.format("Invalid id, must be integer and in range (0, %d)" , authors.size());

    @RequestMapping(path = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<ResponseUnified<String>> getAuthorInfo(@PathVariable Integer id) {
        try {
            String author = authors.get(id);
            var response = new ResponseUnified<>("success" , HttpServletResponse.SC_OK , author);
            return ResponseEntity.ok(response);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return ResponseEntity.badRequest()
                    .body(OUT_OF_BOUND);
        }
    }

    @RequestMapping(path = "/all", method = RequestMethod.GET)
    public ResponseEntity<ResponseUnified<String>> getAuthorsInfo() {
        return ResponseEntity.ok(SUCCESS);
    }
}