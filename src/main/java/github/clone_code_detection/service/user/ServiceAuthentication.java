package github.clone_code_detection.service.user;

import github.clone_code_detection.entity.authenication.*;
import github.clone_code_detection.exceptions.authen.ActivateAccountException;
import github.clone_code_detection.exceptions.authen.FieldValidationException;
import github.clone_code_detection.exceptions.authen.InvalidOperationException;
import github.clone_code_detection.exceptions.authen.ResetPasswordException;
import github.clone_code_detection.repo.RepoActivationToken;
import github.clone_code_detection.repo.RepoResetPasswordToken;
import github.clone_code_detection.repo.RepoUser;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.Optional;

@Service
@Profile("security")
@Transactional
public class ServiceAuthentication {
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RepoResetPasswordToken repoResetPasswordToken;
    private final RepoActivationToken repoActivationToken;
    private final RepoUser repoUser;
    private final JavaMailSender mailSender;
    private final AuthenticationHelper authenticationHelper;

    @Autowired
    public ServiceAuthentication(PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, RepoResetPasswordToken repoResetPasswordToken, RepoActivationToken repoActivationToken, RepoUser repoUser, JavaMailSender javaMailSender, AuthenticationHelper authenticationHelper) {
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.repoResetPasswordToken = repoResetPasswordToken;
        this.repoActivationToken = repoActivationToken;
        this.repoUser = repoUser;
        this.mailSender = javaMailSender;
        this.authenticationHelper = authenticationHelper;
    }

    /**
     * Get user from SecurityContextHolder
     */
    @Nullable
    public static UserImpl getUserFromContext() {
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        if (authentication.getPrincipal() instanceof UserImpl userImpl) return userImpl;
        return null;
    }

    // https://stackoverflow.com/questions/5428654/spring-security-auto-login-not-persisted-in-httpsession
    public UserImpl signIn(SignInRequest request, HttpServletRequest httpServletRequest) {
        validateSignInRequest(request);

        Authentication authentication = authenticationManager.authenticate(request.toUsernamePasswordToken());
        SecurityContextHolder.getContext()
                             .setAuthentication(authentication);
        httpServletRequest.getSession()
                          .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                                  SecurityContextHolder.getContext());
        return (UserImpl) authentication.getPrincipal();
    }

    public void signUp(SignUpRequest request, HttpServletRequest httpServletRequest) {
        if (!request.getPassword()
                    .equals(request.getRepeat()))
            throw new FieldValidationException("repeat", "Repeat password must match password");
        validateSignUpRequest(request);

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        // Create new user's account
        UserImpl user = UserImpl.builder()
                                .username(request.getEmail())
                                .password(encodedPassword)
                                .build();
        if (Boolean.TRUE.equals(request.getIsStandalone())) user = repoUser.createStandaloneUser(user);
        else user = repoUser.createOrgUser(user);

        // Save and send activation token to user
        sendActivationMail(user);
    }

    /**
     * @param request
     * @return
     * @implNote check if user is disabled and resend request if no active validation request exists
     * or throw error.
     */
    private void validateSignInRequest(SignInRequest request) {
        UserImpl user;
        try {
            user = repoUser.findUserImplByUsername(request.getEmail());
        } catch (UsernameNotFoundException ignore) {
            user = null;
        }

        // Check if user existed
        if (user == null) return;
        // Check if user is in registered mode
        if ("register".equals(user.getStatus())) {
            if (repoActivationToken.existsByUser_UsernameAndExpirationAfter(user.getUsername(),
                    ZonedDateTime.now(ForgotPasswordToken.ZONE))) {
                throw new InvalidOperationException(MessageFormat.format(
                        "User {0} has pending account activation request, please check your email!",
                        request.getEmail()));
            } else {
                throw new InvalidOperationException(
                        MessageFormat.format("User {0} has no pending account activation request, please sign up again",
                                request.getEmail()));
            }
        }
    }

    /**
     * @param request
     */
    private void validateSignUpRequest(SignUpRequest request) {
        UserImpl user;
        try {
            user = repoUser.findUserImplByUsername(request.getEmail());
        } catch (UsernameNotFoundException ignore) {
            user = null;
        }

        // Check if user existed
        if (user == null) return;
        // Check if user is in registered mode
        if ("register".equals(user.getStatus())) {
            // Override sign up request by deleting this
            repoResetPasswordToken.deleteAllByUser_Id(user.getId());
            repoActivationToken.deleteAllByUser_Id(user.getId());
            repoUser.deleteById(user.getId());
        } else
            throw new InvalidOperationException(MessageFormat.format("User {0} already existed", request.getEmail()));
    }

    private void sendActivationMail(UserImpl user) {
        AccountActivationToken accountActivationToken = new AccountActivationToken();
        accountActivationToken.setCode(RandomStringUtils.randomAlphabetic(10));
        accountActivationToken.setUser(user);

        repoActivationToken.save(accountActivationToken);
        MimeMessage mimeMessage = authenticationHelper.prepareAccountActivationEmail(user.getUsername(),
                accountActivationToken);
        mailSender.send(mimeMessage);
    }

    public UserImpl info(HttpServletRequest request) {
        return (UserImpl) SecurityContextHolder.getContext()
                                               .getAuthentication()
                                               .getPrincipal();
    }

    /**
     * @param request required
     */
    public void resetPassword(ResetPasswordRequest request) {
        assert request.getPassword()
                      .equals(request.getRepeat()) : "Repeat password field does not match";

        Optional<ForgotPasswordToken> resetPwdToken = repoResetPasswordToken.findByToken(request.getToken());
        ForgotPasswordToken forgotPasswordToken = resetPwdToken.orElseThrow(
                () -> new ResetPasswordException("Invalid reset password token"));

        // Validate
        if (!forgotPasswordToken.isValid()) throw new ResetPasswordException("Expired forgot password reset token");
        forgotPasswordToken.expireNow();
        repoResetPasswordToken.save(forgotPasswordToken);
        updatePassword(forgotPasswordToken.getUser(), request.getPassword());
    }

    public void updatePassword(UserImpl user, String pwd) {
        String encodedPassword = passwordEncoder.encode(pwd);
        user.setPassword(encodedPassword);
        repoUser.save(user); // just for sure
    }

    public void forgetpassword(String email) {
        assert email != null;
        UserImpl user = repoUser.findUserImplByUsername(email);
        if (user == null || !user.isEnabled())
            throw new InvalidOperationException("You are not allowed to reset this account's password");
        String token = RandomStringUtils.randomAlphanumeric(10);
        ZonedDateTime expiration = ZonedDateTime.now(ForgotPasswordToken.ZONE)
                                                .plusMinutes(5);
        ForgotPasswordToken forgotPasswordToken = ForgotPasswordToken.builder()
                                                                     .token(token)
                                                                     .expiration(expiration)
                                                                     .user(user)
                                                                     .build();
        repoResetPasswordToken.save(forgotPasswordToken);
        MimeMessage message = authenticationHelper.prepareForgetPasswordEmail(email, forgotPasswordToken);
        mailSender.send(message);
    }


    public void activateAccount(String code) {
        AccountActivationToken activationToken = repoActivationToken.findByCode(code)
                                                                    .orElseThrow(() -> new ActivateAccountException(
                                                                            "Invalid activate account token"));

        if (!activationToken.isValid()) throw new ActivateAccountException("Activate account token expired");
        UserImpl user = activationToken.getUser();
        if (user == null || user.isEnabled()) return;
        user.setStatus("active");
    }
}