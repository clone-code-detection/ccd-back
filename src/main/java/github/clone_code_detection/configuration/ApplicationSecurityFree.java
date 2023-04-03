package github.clone_code_detection.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableWebSecurity
@EnableTransactionManagement
@EnableMethodSecurity
@Profile("!security")
@Slf4j
public class ApplicationSecurityFree {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // set the name of the attribute the CsrfToken will be populated on
        http.csrf()
            .disable()
            .cors()
            .disable()
        ;
        http.authorizeHttpRequests()
            .anyRequest()
            .permitAll();
        return http.build();
    }
}
