package github.clone_code_detection.controller.moodle;

import github.clone_code_detection.entity.moodle.MoodleResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api/moodle")
public class MoodleController {


    @PostMapping(path = "/detect",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE}
    )
    @ResponseStatus(HttpStatus.OK)
    public MoodleResponse createHighlightSessionForMoodle(@RequestParam("source") MultipartFile source) throws IOException {
        // moodle source is a zip file that contains multiple folders.
        // Each folder is list of file submissions of one student.
        // For each file in student's folder, we consider it as one highlight session.
        File file = new File("/Users/zenodinh/Downloads/" + source.getOriginalFilename());
        source.transferTo(file);
        if (file.createNewFile())
            throw new IOException("Can create new file");
        return MoodleResponse
                .builder()
                .message("OK")
                .build();
    }
}
