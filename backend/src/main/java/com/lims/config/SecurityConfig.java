package com.lims.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {
    private static final String[] STAFF = { "ADMIN", "LIBRARIAN" };

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5500", "http://127.0.0.1:5500"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String[] staffWritePaths = {
            "/api/books/**", "/api/categories/**", "/api/publishers/**",
            "/api/authors/**", "/api/copies/**"
        };
        http.csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Personal history and profile endpoints resolve the authenticated user.
                .requestMatchers(HttpMethod.GET, "/api/members/my-history").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/auth/me", "/api/auth/profile").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/auth/profile").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/auth/change-password").authenticated()

                // Member directory and member administration are staff-only.
                .requestMatchers("/api/members/**").hasAnyRole(STAFF)

                // Catalogue writes are staff work; authenticated users may browse.
                .requestMatchers(HttpMethod.POST, staffWritePaths).hasAnyRole(STAFF)
                .requestMatchers(HttpMethod.PUT, staffWritePaths).hasAnyRole(STAFF)
                .requestMatchers(HttpMethod.DELETE, staffWritePaths).hasAnyRole(STAFF)

                // Global circulation, fine, and reservation views expose member data.
                .requestMatchers(HttpMethod.GET, "/api/borrowings").hasAnyRole(STAFF)
                .requestMatchers(HttpMethod.GET, "/api/fines", "/api/fines/member/**").hasAnyRole(STAFF)
                .requestMatchers(HttpMethod.GET, "/api/fines/*/payments").hasAnyRole(STAFF)
                .requestMatchers(HttpMethod.GET, "/api/reservations",
                    "/api/reservations/queue/**", "/api/reservations/ready").hasAnyRole(STAFF)

                // Only staff may issue, renew, return, record payments, or waive fines.
                .requestMatchers(HttpMethod.POST,
                    "/api/borrowings/issue", "/api/borrowings/*/renew", "/api/borrowings/*/return")
                    .hasAnyRole(STAFF)
                .requestMatchers(HttpMethod.POST, "/api/fines/*/payments", "/api/fines/*/waive")
                    .hasAnyRole(STAFF)
                .requestMatchers(HttpMethod.POST,
                    "/api/reservations/*/fulfill", "/api/reservations/expire").hasAnyRole(STAFF)

                .requestMatchers("/api/dashboard/**", "/api/reports/**").hasAnyRole(STAFF)
                .anyRequest().authenticated());
        return http.build();
    }
}
