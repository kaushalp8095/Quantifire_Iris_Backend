package com.project.agency.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Iska matlab saare API endpoints (/api/agency/login, etc.) open hain
                .allowedOrigins(
                    "https://quantifire-iris-frontend.vercel.app", // Aapka Vercel Live Frontend
                    "http://localhost:3000", // (Optional) Agar aap local React/Vite use kar rahe hain
                    "http://127.0.0.1:5500"  // (Optional) Agar aap VS Code Live Server use kar rahe hain
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Ye saare actions allowed hain
                .allowedHeaders("*") // Saare headers allowed hain
                .allowCredentials(true); // Agar login cookies ya tokens use kar rahe hain toh ye zaroori hai
    }
}
