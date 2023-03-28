package github.clone_code_detection.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

public class ProblemDetailUtil {
    public static ProblemDetail forTypeAndStatusAndDetail(String type, HttpStatus status, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setType(URI.create(type));
        return problemDetail;
    }
}
