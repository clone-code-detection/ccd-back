package github.clone_code_detection.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import github.clone_code_detection.entity.LanguageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class UtilLanguageMap {
    public static final String LANGUAGE_JSON = "classpath:data/extension_language.json";
    final LanguageInfo[] languageInfos;
    final Map<String, String> onMemExtensionLanguageMapping = new HashMap<>(); // Extension, Index

    @Autowired
    public UtilLanguageMap(ResourceLoader resourceLoader) throws IOException {
        Resource resourceFile = resourceLoader.getResource(LANGUAGE_JSON);
        ObjectMapper mapper = new ObjectMapper();
        languageInfos = mapper.readerForArrayOf(LanguageInfo.class)
                              .readValue(resourceFile.getInputStream());
        for (LanguageInfo languageInfo : languageInfos) {
            log.info("Supported languages {}", languageInfo);
            for (String ext : languageInfo.getExtensions())
                onMemExtensionLanguageMapping.put(ext, languageInfo.getIndex());
        }
    }

    public String getIndexFromExtension(String extension) {
        return onMemExtensionLanguageMapping.get(extension);
    }
}
