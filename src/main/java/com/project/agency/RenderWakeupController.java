package com.project.agency;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RenderWakeupController {

    // Is endpoint ko Cron-job.org hit karega
    @GetMapping("/api/agency/ping")
    public ResponseEntity<String> ping() {
        // Bina database call ke direct response bhej rahe hain
        return ResponseEntity.ok("Server is running!");
    }
}