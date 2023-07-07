package github.clone_code_detection;

import jakarta.annotation.PreDestroy;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@SpringBootApplication(scanBasePackages = {"github.clone_code_detection"})
@EntityScan(basePackages=  "github.clone_code_detection")
public class App {
    private final JavaMailSender mailSender;

    @Autowired
    public App(JavaMailSender mailSender) {this.mailSender = mailSender;}

    @Value("${spring.mail.sender}")
    String sender;

    public static void main(String[] args) {
        SpringApplication.run(App.class , args);
    }


    @EventListener(ApplicationReadyEvent.class)
    public void sendSuccessMail() throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        helper.setSubject("Confirm reset password for Code Clone Detection");
        helper.setFrom(sender);
        helper.setTo(sender);
        helper.setText("Service is up", true);
        mailSender.send(message);
    }

    @PreDestroy
    public void sendServerDownEmail() throws MessagingException {
    }
}

