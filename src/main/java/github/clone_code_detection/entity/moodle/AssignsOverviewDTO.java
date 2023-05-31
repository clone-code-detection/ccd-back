package github.clone_code_detection.entity.moodle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignsOverviewDTO {
    Page<AssignDTO> assigns;
    CourseDTO course;
}
