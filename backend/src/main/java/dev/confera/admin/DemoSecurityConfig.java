package dev.confera.admin;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Separate filter chain for /admin/demo/** — JWT is not required; the controller
 * validates the X-Demo-Reset-Key header itself.
 */
@Configuration
@Profile("demo")
public class DemoSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain demoAdminFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/admin/demo/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}