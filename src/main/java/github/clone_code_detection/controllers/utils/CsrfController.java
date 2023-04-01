package github.clone_code_detection.controllers.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/csrf")
public class CsrfController {
    @GetMapping
    public CsrfToken getCsrfToken(CsrfToken csrfToken, HttpServletResponse response) {
        Cookie cookie = new Cookie("CSRF-COOKIE", csrfToken.getToken());
        cookie.setHttpOnly(false);
        response.addCookie(cookie);
        return csrfToken;
    }
}