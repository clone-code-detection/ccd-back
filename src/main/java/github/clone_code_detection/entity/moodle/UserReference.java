package github.clone_code_detection.entity.moodle;

import github.clone_code_detection.entity.authenication.UserImpl;
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
@Table(name = "user_reference", schema = "moodle")
public class UserReference {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "internal_user_id", referencedColumnName = "id")
    private UserImpl internalUser;

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reference_user_id", referencedColumnName = "reference_user_id")
    // The userid of moodle that reference to our server user
    private MoodleUser referenceUser;

    @Column(name = "token")
    private String token;

    @Column(name = "moodle_url")
    private String moodleUrl;
}
