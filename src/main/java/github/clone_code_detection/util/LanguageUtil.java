package github.clone_code_detection.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import github.clone_code_detection.entity.LanguageInfo;
import github.clone_code_detection.entity.highlight.document.HighlightSingleDocument;
import github.clone_code_detection.exceptions.UnsupportedLanguage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class LanguageUtil {
    private static LanguageUtil instance = null;
    private static final String LANGUAGE_JSON = "data/extension_language.json";
    private final Map<String, String> onMemExtensionLanguageMapping = new HashMap<>(); // Extension, Index

    public static LanguageUtil getInstance() {
        if (instance == null) {
            try {
                instance = new LanguageUtil();
            } catch (IOException e) {
                throw new RuntimeException("Failed to instantiate language util", e);
            }
        }
        return instance;
    }

    private LanguageUtil() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream resourceAsStream = this.getClass()
                                           .getClassLoader()
                                           .getResourceAsStream(LANGUAGE_JSON);
        final LanguageInfo[] languageInfos = mapper.readerForArrayOf(LanguageInfo.class)
                                                   .readValue(resourceAsStream);
        for (LanguageInfo languageInfo : languageInfos) {
            log.info("Supported languages {}", languageInfo);
            for (String ext : languageInfo.getExtensions())
                onMemExtensionLanguageMapping.put(ext, languageInfo.getIndex());
        }
    }

    @Nonnull
    public String getIndexFromFileName(String fileName) {
        String extension = FilenameUtils.getExtension(fileName);
        return this.getIndexFromExtension(extension);
    }

    @Nonnull
    public String getIndexFromExtension(String extension) {
        String s = onMemExtensionLanguageMapping.get(extension);
        if (s == null) {
            throw new UnsupportedLanguage("Unsupported language: " + extension);
        }
        return s;
    }

    public static String getMainLanguageOfSingleDocuments(Collection<HighlightSingleDocument> matches) {
        Map<String, Integer> map = new HashMap<>();
        for (HighlightSingleDocument match : matches) {
            String language = LanguageUtil.getInstance().getIndexFromFileName(match.getSource().getFileName());
            if (map.get(language) != null)
                map.put(language, map.get(language) + 1);
            else
                map.put(language, 0);
        }
        int max = 0;
        String language = "";
        for (Map.Entry<String, Integer> set : map.entrySet()) {
            if (set.getValue() > max) {
                max = set.getValue();
                language = set.getKey();
            }
        }
        return language;
    }
}
