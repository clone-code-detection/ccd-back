package github.clone_code_detection.configuration;


import com.fasterxml.jackson.databind.ObjectMapper;
import github.clone_code_detection.util.ProblemDetailUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableTransactionManagement
@EnableMethodSecurity
@Profile("security")
@Slf4j
public class ApplicationSecurity {
    /**
     * @implNote Autowired to UserDetailsServiceImpl
     */
    private final UserDetailsService userDetailsService;

    @Autowired
    public ApplicationSecurity(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authProvider);
    }

    /**
     * @param http
     * @param authenticationManager
     * @return
     * @throws Exception
     * @implNote https://stackoverflow.com/questions/25230861/spring-security-get-user-info-in-rest-service-for-authenticated-and-not-authent/25280897#25280897
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           @Autowired AuthenticationManager authenticationManager) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        // set the name of the attribute the CsrfToken will be populated on
        requestHandler.setCsrfRequestAttributeName("_csrf");
        http.csrf()
            .ignoringRequestMatchers("/authentication/**")
            .csrfTokenRequestHandler(requestHandler)
            .csrfTokenRepository(new HttpSessionCsrfTokenRepository())
            .and()
            .cors()
            .configurationSource(corsConfigurationSource())
        ;
        http.authorizeHttpRequests()
            .requestMatchers("/authentication/**", "/csrf/**")
            .permitAll()

            .anyRequest()
            .authenticated();
        http.exceptionHandling()
            .authenticationEntryPoint(getAuthenticationEntryPoint);
        http.authenticationManager(authenticationManager);
        return http.build();
    }

    private static final ObjectMapper om = new ObjectMapper();

    private static final AuthenticationEntryPoint getAuthenticationEntryPoint = (request, response, authException) -> {
        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        ProblemDetail problemDetail = null;
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        log.info("Authen: {}", authentication);
        if (authException instanceof InsufficientAuthenticationException) {
            statusCode = HttpStatus.UNAUTHORIZED.value();
            problemDetail = ProblemDetailUtil.forTypeAndStatusAndDetail("undocumented", HttpStatus.UNAUTHORIZED,
                                                                        authException.getMessage());
        } else {
            problemDetail = ProblemDetailUtil.forTypeAndStatusAndDetail("undocumented",
                                                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                                                        authException.getMessage());
        }
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(statusCode);
        response.getOutputStream()
                .print(om.writeValueAsString(problemDetail));
    };

    // https://stackoverflow.com/questions/51719889/spring-boot-cors-issue
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type", "X-CSRF-TOKEN"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PUT", "OPTIONS", "PATCH", "DELETE"));
        configuration.setExposedHeaders(List.of("Set-Cookie"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
