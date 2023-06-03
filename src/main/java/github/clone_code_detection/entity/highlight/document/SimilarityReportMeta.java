package github.clone_code_detection.entity.highlight.document;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "similarity_report_meta", schema = "highlight")
public class SimilarityReportMeta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String author;
    // indicate where the project is from.
    // Project can be from private source like Moodle partners
    // Or can be from public internet like github.
    private String origin;
    // The link to the origin project (optional)
    private String link;
}
