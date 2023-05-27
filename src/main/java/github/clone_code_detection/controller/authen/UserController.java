package github.clone_code_detection.controller.authen;

import github.clone_code_detection.service.user.ServiceAuthentication;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Profile("security")
public class UserController {
    private final ServiceAuthentication serviceAuthentication;

    @Autowired
    public UserController(ServiceAuthentication serviceAuthentication) {this.serviceAuthentication = serviceAuthentication;}

    @GetMapping(path = "/info")
    @ResponseStatus(HttpStatus.OK)
    public AuthenticationController.UserAuthenticateResponse info(HttpServletRequest request) {
        UserDetails userDetails = serviceAuthentication.info(request);
        return new AuthenticationController.UserAuthenticateResponse(userDetails);
    }
}
