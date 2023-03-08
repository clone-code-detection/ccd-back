package github.clone_code_detection.service;

import github.clone_code_detection.entity.BaseEmail;
import org.springframework.mail.SimpleMailMessage;

public interface IServiceMail {
    void sendEmail(BaseEmail email);

    void sendEmail(SimpleMailMessage email);
}
