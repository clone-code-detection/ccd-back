package github.clone_code_detection.entity.moodle;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import github.clone_code_detection.util.TimeUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {
    private long id;
    private String shortname;
    private String fullname;
    @JsonProperty("enrolled_user_count")
    private long enrolledUserCount;
    @JsonProperty("start_date")
    private ZonedDateTime startDate;
    @JsonProperty("end_date")
    private ZonedDateTime endDate;
    private boolean completed;

    public static List<CourseDTO> asList(JsonNode moodleCourses) {
        List<CourseDTO> courses = new ArrayList<>();
        moodleCourses.forEach(course -> courses.add(CourseDTO.builder()
                                                             .id(course.get("id").asLong())
                                                             .shortname(course.get("shortname").asText())
                                                             .fullname(course.get("fullname").asText())
                                                             .enrolledUserCount(course.get("enrolledusercount").asInt())
                                                             .startDate(TimeUtil.parseZoneDateTime(course.get(
                                                                     "startdate").asLong()))
                                                             .endDate(TimeUtil.parseZoneDateTime(course.get("enddate")
                                                                                                       .asLong()))
                                                             .completed(course.get("completed").asBoolean())
                                                             .build()));
        return courses;
    }

    public static CourseDTO asDetail(JsonNode courseDetail) {
        return CourseDTO.builder()
                        .id(courseDetail.get("id").asLong())
                        .shortname(courseDetail.get("shortname").asText())
                        .fullname(courseDetail.get("fullname").asText())
                        .startDate(TimeUtil.parseZoneDateTime(courseDetail.get("startdate").asLong()))
                        .endDate(TimeUtil.parseZoneDateTime(courseDetail.get("enddate").asLong()))
                        .build();
    }
}
