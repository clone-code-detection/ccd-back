package github.clone_code_detection.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class FileSystemUtil {
    public static void saveFileToLocal(MultipartFile file) {

    }

    public static byte[] getContent(MultipartFile file) {
        try {
            return file.getInputStream()
                       .readAllBytes();
        } catch (IOException e) {
            return new byte[0];
        }
    }
}
