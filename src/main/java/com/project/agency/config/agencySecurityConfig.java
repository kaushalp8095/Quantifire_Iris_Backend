package com.project.agency.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class agencySecurityConfig {

    // 🌟 BRAHMASTRA: Ye filter sabse pehle chalega, kisi bhi Order(1) ya Order(2) se pehle
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of(
            "http://127.0.0.1:5501", 
            "http://localhost:5501",
            "https://quantifire-iris-frontend.vercel.app" // Bina slash ke
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Ye sabhi APIs par VIP pass laga dega
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
    
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/uploads/**");
    }

    @Bean
    @Order(2) 
    public SecurityFilterChain agencyFilterChain(HttpSecurity http) throws Exception {
        http
            // Ab lamba code nahi, bas default CORS customizer call karein jo upar wala bean uthayega
            .cors(Customizer.withDefaults()) 
            
            .securityMatcher("/api/agency/**", "/api/integration/**", "/api/top-notifications/**") 
            .csrf(csrf -> csrf.disable()) // CSRF ko disable karein taaki AJAX chale
            
            .authorizeHttpRequests(auth -> auth
            		.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
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