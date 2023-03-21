package github.clone_code_detection.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import github.clone_code_detection.entity.HighlightReport;
import github.clone_code_detection.entity.HighlightResponse;
import github.clone_code_detection.entity.RenderData;
import github.clone_code_detection.entity.RenderDocument;
import github.clone_code_detection.repositories.HighlightReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ServiceRender {
    final static ObjectMapper mapper = new ObjectMapper();
    final HighlightReportRepository repository;

    @Autowired
    public ServiceRender(HighlightReportRepository repository) {
        this.repository = repository;
    }

    public RenderDocument getRenderDocument(int id) throws Exception {
        // Get report by id
        HighlightReport report = repository.findById(id).orElse(null);
        if (report == null) {
            log.error("Report not found");
            throw new SQLException("Report not found");
        }
        // Get file content and parse to sources
        RenderDocument renderDocument = RenderDocument.builder().report(report).build();
        // Check whether there are any report files has been highlighted yet
        if (report.getExtraData().isEmpty()) {
            log.error("No report files found");
            throw new NegativeArraySizeException("No report files found");
        }
        Set<RenderData> data = getReportFileContent(report.getUri());
        if (data == null) {
            log.error("Can not load report files");
            throw new MissingResourceException("Can not load report files", "Render data", "report files");
        }
        renderDocument.setSources(data);
        return renderDocument;
    }

    private Set<RenderData> getReportFileContent(String path) {
        // Access folder
        File folder = new File(path);
        if (!folder.exists() || !folder.isDirectory()) {
            log.error("No folder found");
            return null;
        }
        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            log.error("No files found in folder");
            return null;
        }
        return Arrays.stream(files)
                .map(file -> {
                    try {
                        return RenderData.builder()
                                .filename(file.getName().substring(0, file.getName().lastIndexOf(".")))
                                .source(mapper.readValue(file, HighlightResponse.class))
                                .build();
                    } catch (IOException e) {
                        log.error(e.getMessage());
                        return null;
                    }
                })
                .collect(Collectors.toCollection(HashSet::new));
    }
}
