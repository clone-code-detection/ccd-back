package github.clone_code_detection;

import github.clone_code_detection.entity.BaseEmail;
import github.clone_code_detection.service.IServiceMail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class Runner implements ApplicationRunner {
    private final IServiceMail iServiceMail;

    @Autowired
    public Runner(IServiceMail iServiceMail) {this.iServiceMail = iServiceMail;}

    @Override
    public void run(ApplicationArguments args) throws Exception {
        iServiceMail.sendEmail(BaseEmail.builder()
                .recipient("thiensuha01@gmail.com")
                .msg("hi")
                .subject("test")
                .build());
    }
}
