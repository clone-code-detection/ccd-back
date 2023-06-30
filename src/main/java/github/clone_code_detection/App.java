package github.clone_code_detection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication(scanBasePackages = {"github.clone_code_detection"})
@EntityScan(basePackages=  "github.clone_code_detection")
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class , args);
    }
}

