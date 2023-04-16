package github.clone_code_detection.service.mail;

import github.clone_code_detection.entity.mail.BaseEmail;
import github.clone_code_detection.service.mail.IServiceMail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class ServiceMail implements IServiceMail {
    private final JavaMailSender javaMailSender;
    @Value("spring.mail.username")
    String sender;

    @Autowired
    public ServiceMail(JavaMailSender javaMailSender) {this.javaMailSender = javaMailSender;}

    @Override
    public void sendEmail(BaseEmail email) {
        SimpleMailMessage mailMessage = new SimpleMailMessage() {{
            setTo(email.getRecipient());
            setText(email.getMsg());
        }};
        this.sendEmail(mailMessage);
        javaMailSender.send(mailMessage);
    }

    @Override
    public void sendEmail(SimpleMailMessage email) {
        assert email.getFrom() == null : "From must be null";
        email.setFrom(sender);
        javaMailSender.send(email);
    }
}
