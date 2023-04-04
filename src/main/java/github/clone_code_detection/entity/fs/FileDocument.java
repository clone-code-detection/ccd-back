package github.clone_code_detection.entity.fs;

import github.clone_code_detection.entity.authenication.UserImpl;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "file", schema = "file")
public class FileDocument {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "content", nullable = false)
    private byte[] content;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @ManyToOne(cascade = CascadeType.PERSIST, optional = false)
    @JoinColumn(name = "uid", referencedColumnName = "id")
    private UserImpl User;

    public String getContentAsString() {
        return new String(content, StandardCharsets.UTF_8);
    }
}
