package github.clone_code_detection.entity.highlight.document;


import github.clone_code_detection.entity.fs.FileDocument;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data

@Entity
@Table(name = "highlight_single_target_match", schema = "highlight")
public class HighlightSingleTargetMatchDocument {
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "target_file_id")
    private FileDocument target;

    @Column(name = "score")
    private Float score;

    @ManyToOne
    @JoinColumn(name = "single_document_id")
    private HighlightSingleDocument source;
}
