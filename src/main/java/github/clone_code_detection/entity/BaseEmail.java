package github.clone_code_detection.entity;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BaseEmail {
    private String recipient;
    private String msg;
    private String subject;
}
