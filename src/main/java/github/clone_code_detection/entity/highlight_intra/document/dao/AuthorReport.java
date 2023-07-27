package github.clone_code_detection.entity.highlight_intra.document.dao;

import github.clone_code_detection.entity.highlight.document.ReportSourceDocument;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * An author represents a zip file when submitting
 */

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "author_report", schema = "highlight_intra")
public class AuthorReport {
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name")
    String name;

    @Column(name = "total_files")
    int totalFiles;

    @Column(name = "most_likely_match")
    String other_author;

    @Column(name = "total_other_matches")
    Integer totalMatches;

    /**
     * @see github.clone_code_detection.entity.highlight.document.SimilarityReport
     */
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    @JoinColumn(name = "report_id", referencedColumnName = "id")
    @Fetch(FetchMode.SUBSELECT)
    private Collection<ReportSourceDocument> sources = new ArrayList<>();
}
