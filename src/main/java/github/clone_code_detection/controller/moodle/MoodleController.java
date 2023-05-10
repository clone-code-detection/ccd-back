package github.clone_code_detection.controller.moodle;

import github.clone_code_detection.entity.highlight.report.HighlightSessionReportDTO;
import github.clone_code_detection.entity.highlight.request.HighlightSessionRequest;
import github.clone_code_detection.entity.index.IndexInstruction;
import github.clone_code_detection.entity.moodle.MoodleResponse;
import github.clone_code_detection.service.highlight.ServiceHighlight;
import github.clone_code_detection.service.moodle.ServiceMoodle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

@RestController
@RequestMapping("/api/moodle")
public class MoodleController {
    private final ServiceHighlight serviceHighlight;

    @Autowired
    public MoodleController(ServiceHighlight serviceHighlight) {
        this.serviceHighlight = serviceHighlight;
    }


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
        Collection<HighlightSessionRequest> requests = ServiceMoodle.unzipMoodleFileAndGetRequests(source);
        Collection<HighlightSessionReportDTO> reports = new ArrayList<>();
        requests.forEach(request -> reports.add(serviceHighlight.createHighlightSession(request, IndexInstruction.getDefaultInstruction())));
        return MoodleResponse.builder().message("OK").data(reports).build();
    }
}
