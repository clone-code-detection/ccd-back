package github.clone_code_detection.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LanguageUtilTest {

    @Test
    void getIndexFromExtension() {
        String index = LanguageUtil.getInstance()
                                   .getIndexFromFileName("/test-file.java");
        assertThat(index).isEqualTo("java");
    }
}