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
@Table(name = "highlight_single_document", schema = "highlight")
public class HighlightSingleDocument {
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "source_id", referencedColumnName = "id")
    private FileDocument source;

    @OneToOne(cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "target_id", referencedColumnName = "id")
    private FileDocument target;
}
