package github.clone_code_detection.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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