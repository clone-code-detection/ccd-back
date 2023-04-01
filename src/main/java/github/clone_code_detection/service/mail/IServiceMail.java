package github.clone_code_detection.service.mail;

import github.clone_code_detection.entity.mail.BaseEmail;
import org.springframework.mail.SimpleMailMessage;

public interface IServiceMail {
    void sendEmail(BaseEmail email);

    void sendEmail(SimpleMailMessage email);
}
