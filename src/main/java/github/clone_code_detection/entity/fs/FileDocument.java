package github.clone_code_detection.entity.fs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import github.clone_code_detection.entity.authenication.UserImpl;
import jakarta.persistence.*;
import lombok.*;

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
    @Column(name = "id", columnDefinition = "uuid")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "content", nullable = false)
    @Getter(AccessLevel.NONE)
    private byte[] content;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, optional = true)
    @JoinColumn(name = "uid")
    private UserImpl User;


    public String getContent() {
        return new String(content, StandardCharsets.UTF_8);
    }

    @JsonIgnore
    public byte[] getByteContent() {
        return this.content;
    }
}
