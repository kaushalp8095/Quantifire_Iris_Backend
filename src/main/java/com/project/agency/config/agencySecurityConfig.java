package com.project.agency.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import java.util.List;

@Configuration
public class agencySecurityConfig {
	
	@Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/uploads/**");
    }

    @Bean
    @Order(2) 
    public SecurityFilterChain agencyFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS ko Security level par set karna zaroori hai
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(List.of("http://127.0.0.1:5501", "http://localhost:5501",
                		                          "https://quantifire-iris-frontend.vercel.app"));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(true);
                return config;
            }))
            
            .securityMatcher("/api/agency/**", "/api/integration/**", "/api/top-notifications/**") 
            .csrf(csrf -> csrf.disable()) // CSRF ko disable karein taaki AJAX chale
            
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/agency/login",
                    "/api/agency/profile",
                    "/api/agency/update-profile",
                    "/api/agency/register-test", 
                    "/api/agency/campaigns/**",
                    "/api/agency/clients/**",
                    "/api/agency/locations/**",
                    "/api/agency/dashboard/**",
                    "/api/agency/reports/**",
                    "/api/agency/security/**",
                    "/api/integration/google/**",
                    "/api/integration/facebook/**",
                    "/api/integration/status",
                    "/api/integration/disconnect",
                    "/api/agency/notifications/**",
                    "/api/top-notifications/**"
                 
                ).permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
    
}