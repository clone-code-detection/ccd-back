package github.clone_code_detection.entity.highlight.document;


import github.clone_code_detection.entity.fs.FileDocument;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data

@Entity
@Table(name = "report_source_document", schema = "highlight")
public class ReportSourceDocument {
    @Id
    @Column(name = "id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumn(name = "source_file_id", referencedColumnName = "id")
    private FileDocument source;

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, fetch = FetchType.EAGER, mappedBy = "source")
    private Collection<ReportTargetDocument> matches = new ArrayList<>();
}
