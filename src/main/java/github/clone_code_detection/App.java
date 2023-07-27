package github.clone_code_detection;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.core.context.SecurityContextHolder;

@SpringBootApplication(scanBasePackages = {"github.clone_code_detection"})
@EntityScan(basePackages = "github.clone_code_detection")
@EnableAsync
public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);
    private final JavaMailSender mailSender;
    @Value("${spring.mail.sender}")
    String sender;

    @Autowired
    public App(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            log.error("[Exception in thread pool] Error from {}", t.getName(), e);
        });
        SpringApplication.run(App.class, args);
    }


    @PostConstruct
    void setGlobalSecurityContext() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
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

