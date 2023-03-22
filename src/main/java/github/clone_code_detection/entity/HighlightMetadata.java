package github.clone_code_detection.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HighlightMetadata {
    String author;
    String organization;
    int semester;
    String course;
    String filename;
    int year;
    String project;
    String assigner;
}
