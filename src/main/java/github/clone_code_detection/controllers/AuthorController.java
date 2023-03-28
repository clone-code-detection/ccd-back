package github.clone_code_detection.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/authors")
public class AuthorController {
    final static List<String> authors = new ArrayList<>() {{
        add("Quan Trong Dinh");
        add("Ha Hai Nguyen");
    }};
    final static String statusMessage =
            String.format("Invalid id, must be integer and in range (0, %d)", authors.size());

    @RequestMapping(path = "/{id}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public String getAuthorInfo(@PathVariable Integer id) {
        return authors.get(id);
    }

    @ExceptionHandler({NumberFormatException.class, IndexOutOfBoundsException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleException(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @RequestMapping(path = "/all", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public String getAuthorsInfo() {
        return "We are two students from Ho Chi Minh city University of Science.";
    }
}