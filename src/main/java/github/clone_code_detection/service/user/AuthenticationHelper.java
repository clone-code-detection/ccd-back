package github.clone_code_detection.service.user;

import github.clone_code_detection.entity.authenication.AccountActivationToken;
import github.clone_code_detection.entity.authenication.ForgotPasswordToken;
import jakarta.mail.internet.MimeMessage;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class AuthenticationHelper {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy - hh:mm");
    @Value("${spring.mail.sender}")
    String sender;

    @Value("${ccd.domain}")
    String domain;

    private final JavaMailSender mailSender;

    @Autowired
    public AuthenticationHelper(JavaMailSender mailSender) {this.mailSender = mailSender;}

    private String format(ZonedDateTime dateTime) {
        return DATE_TIME_FORMATTER.format(dateTime);
    }

    @SneakyThrows
    @NonNull
    public MimeMessage prepareAccountActivationEmail(String email, AccountActivationToken token) {
        // Build link
        ZonedDateTime expiration = token.getExpiration();
        String code = token.getCode();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(domain)
                                                           .path("/authen/activate") // activate at FE
                                                           .queryParam("code", code);
        String resetLink = builder.toUriString();

        // Prepare and send email
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        helper.setSubject("Confirm account for Code Clone Detection");
        helper.setFrom(sender);
        helper.setTo(email);
        helper.setText(MessageFormat.format(
                "Follow this <a href=\"{0}\">link</a> to activate your account." + "\n" + "The link will expire at {1}",
                resetLink, format(expiration)), true);
        return message;
    }

    @SneakyThrows
    public MimeMessage prepareForgetPasswordEmail(String email, ForgotPasswordToken forgotPasswordToken) {
        // Build link
        ZonedDateTime expiration = forgotPasswordToken.getExpiration();
        String code = forgotPasswordToken.getToken();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(domain)
                                                           .path("/authen/reset-password") // reset password path at FE
                                                           .queryParam("code", code);
        String resetLink = builder.toUriString();

        // Prepare and send email
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        helper.setSubject("Confirm reset password for Code Clone Detection");
        helper.setFrom(sender);
        helper.setTo(email);
        helper.setText(MessageFormat.format(
                "Follow this <a href=\"{0}\">link</a> to reset your account's password." + "\n" + "The link will expire at {1}",
                resetLink, format(expiration)), true);
        return message;
    }
}
