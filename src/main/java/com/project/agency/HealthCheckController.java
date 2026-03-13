package com.project.agency;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Agar frontend kisi aur domain pe hai (Vercel), toh CORS allow karne ke liye
public class HealthCheckController {

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        // Ye simple message return karega aur 200 OK status dega
        return ResponseEntity.ok("Quantifyre Iris Server is awake and running smoothly!");
    }
}
