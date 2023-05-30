package github.clone_code_detection.entity.moodle;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.JsonArray;
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
    @Builder.Default
    List<AssignDTO> assigns = new ArrayList<>();
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

    public static List<CourseDTO> from(JsonArray moodleCourses) {
        List<CourseDTO> courses = new ArrayList<>();
        moodleCourses.forEach(course -> courses.add(CourseDTO.builder()
                                                             .id(course.getAsJsonObject().get("id").getAsInt())
                                                             .shortname(course.getAsJsonObject()
                                                                              .get("shortname")
                                                                              .getAsString())
                                                             .fullname(course.getAsJsonObject()
                                                                             .get("fullname")
                                                                             .getAsString())
                                                             .enrolledUserCount(course.getAsJsonObject()
                                                                                      .get("enrolledusercount")
                                                                                      .getAsInt())
                                                             .startDate(TimeUtil.parseZoneDateTime(course.getAsJsonObject()
                                                                                                         .get("startdate")))
                                                             .endDate(TimeUtil.parseZoneDateTime(course.getAsJsonObject()
                                                                                                       .get("enddate")))
                                                             .completed(course.getAsJsonObject()
                                                                              .get("completed")
                                                                              .getAsBoolean())
                                                             .build()));
        return courses;
    }
}
