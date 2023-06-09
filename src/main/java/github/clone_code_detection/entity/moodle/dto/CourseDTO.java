package github.clone_code_detection.entity.moodle.dto;

import github.clone_code_detection.entity.moodle.Assign;
import github.clone_code_detection.entity.moodle.Course;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    Page<Assign> assigns;
    Course course;
}
