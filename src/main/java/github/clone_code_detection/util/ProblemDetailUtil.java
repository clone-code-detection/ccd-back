package github.clone_code_detection.util;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

public class ProblemDetailUtil {
    private ProblemDetailUtil() {
        throw new IllegalStateException("ProblemDetailUtil is utility class");
    }
    public static ProblemDetail forTypeAndStatusAndDetail(URI type, HttpStatus status, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setType(type);
        return problemDetail;
    }
}
