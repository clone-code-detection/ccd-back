package github.clone_code_detection.entity.moodle;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {
    private long id;
    private String shortname;
    private String fullname;
    @JsonProperty("enrolled_user_count")
    private long enrolledUserCount;
    @JsonProperty("start_date")
    private long startDate;
    @JsonProperty("end_date")
    private long endDate;
    private boolean completed;

    public static List<Course> asList(JsonNode moodleCourses) {
        List<Course> courses = new ArrayList<>();
        moodleCourses.forEach(course -> courses.add(Course.builder()
                                                          .id(course.get("id").asLong())
                                                          .shortname(course.get("shortname").asText())
                                                          .fullname(course.get("fullname").asText())
                                                          .enrolledUserCount(course.get("enrolledusercount").asInt())
                                                          .startDate(course.get("startdate").asLong())
                                                          .endDate(course.get("enddate").asLong())
                                                          .completed(course.get("completed").asBoolean())
                                                          .build()));
        return courses;
    }

    public static Course asDetail(JsonNode courseDetail) {
        return Course.builder()
                     .id(courseDetail.get("id").asLong())
                     .shortname(courseDetail.get("shortname").asText())
                     .fullname(courseDetail.get("fullname").asText())
                     .startDate(courseDetail.get("startdate").asLong())
                     .endDate(courseDetail.get("enddate").asLong())
                     .build();
    }
}
