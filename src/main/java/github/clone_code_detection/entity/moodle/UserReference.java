package github.clone_code_detection.entity.moodle;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "user_reference", schema = "moodle")
public class UserReference {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "internal_user_id", columnDefinition = "uuid")
    private UUID internalUserId;

    @Column(name = "reference_user_id")
    private long referenceUserId; // The userid of moodle that reference to our server user

    private String token;
}
