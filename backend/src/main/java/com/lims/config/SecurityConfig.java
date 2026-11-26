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
                // Public registration; everything else needs a login.
                .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                // Administration (audit log) is admin-only.
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // A member can read their own borrowing history.
                .requestMatchers(HttpMethod.GET, "/api/members/my-history").authenticated()
                // Member management is staff work.
                .requestMatchers(HttpMethod.GET, "/api/members/**").hasAnyRole("ADMIN", "LIBRARIAN")
                .requestMatchers(HttpMethod.POST, "/api/members/**").hasAnyRole("ADMIN", "LIBRARIAN")
                .requestMatchers(HttpMethod.PUT, "/api/members/**").hasAnyRole("ADMIN", "LIBRARIAN")
                .requestMatchers(HttpMethod.DELETE, "/api/members/**").hasAnyRole("ADMIN", "LIBRARIAN")
                // Catalogue writes are staff work; reads stay open to any logged-in user.
                .requestMatchers(HttpMethod.POST, staffWritePaths).hasAnyRole("ADMIN", "LIBRARIAN")
                .requestMatchers(HttpMethod.PUT, staffWritePaths).hasAnyRole("ADMIN", "LIBRARIAN")
                .requestMatchers(HttpMethod.DELETE, staffWritePaths).hasAnyRole("ADMIN", "LIBRARIAN")
                // Circulation desk operations.
                .requestMatchers(HttpMethod.POST,
                    "/api/borrowings/issue", "/api/borrowings/*/renew", "/api/borrowings/*/return")
                    .hasAnyRole("ADMIN", "LIBRARIAN")
                // Fine desk operations.
                .requestMatchers(HttpMethod.POST, "/api/fines/*/payments", "/api/fines/*/waive")
                    .hasAnyRole("ADMIN", "LIBRARIAN")
                // Dashboard and reports are staff-only.
                .requestMatchers("/api/dashboard/**", "/api/reports/**").hasAnyRole("ADMIN", "LIBRARIAN")
                .anyRequest().authenticated());
        return http.build();
    }
}
