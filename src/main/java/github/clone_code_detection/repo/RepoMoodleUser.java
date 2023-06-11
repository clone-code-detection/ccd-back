package github.clone_code_detection.repo;

import github.clone_code_detection.entity.moodle.MoodleUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepoMoodleUser extends JpaRepository<MoodleUser, Long> {
    MoodleUser findFirstByReferenceUserId(long referenceId);
}