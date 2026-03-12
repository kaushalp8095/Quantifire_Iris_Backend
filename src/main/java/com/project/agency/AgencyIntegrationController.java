package com.project.agency;

import com.project.common.models.agencyIntegrationModel;
import com.project.common.service.AgencyIntegrationService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/integration")
@CrossOrigin(origins = {"http://127.0.0.1:5501", "http://localhost:5501"})
public class AgencyIntegrationController {

    @Autowired private AgencyIntegrationService service;

    @Value("${google.ads.client-id}") private String googleClientId;
    @Value("${google.ads.callback-url}") private String googleRedirectUri;
    @Value("${facebook.app-id}") private String fbAppId;
    @Value("${facebook.callback-url}") private String fbRedirectUri;

    // --- REDIRECT URL GENERATORS ---
    @GetMapping("/google/connect")
    public ResponseEntity<Map<String, String>> connectGoogle(@RequestParam String email) {
        String url = "https://accounts.google.com/o/oauth2/v2/auth?client_id=" + googleClientId +
                     "&redirect_uri=" + googleRedirectUri + "&response_type=code" +
                     "&scope=https://www.googleapis.com/auth/adwords&access_type=offline&prompt=consent&state=" + email;
        return ResponseEntity.ok(Collections.singletonMap("url", url));
    }

    @GetMapping("/facebook/connect")
    public ResponseEntity<Map<String, String>> connectFb(@RequestParam String email) {
        String url = "https://www.facebook.com/v18.0/dialog/oauth?client_id=" + fbAppId +
                     "&redirect_uri=" + fbRedirectUri + "&response_type=code" +
                     "&scope=ads_management,ads_read,business_management&state=" + email;
        return ResponseEntity.ok(Collections.singletonMap("url", url));
    }

    // --- CALLBACK HANDLERS ---
    @GetMapping("/google/callback")
    public void googleCallback(@RequestParam String code, @RequestParam String state, HttpServletResponse response) throws IOException {
        service.processGoogleCallback(code, state); // state mein email hai
        response.sendRedirect("http://127.0.0.1:5501/Settings.html?status=google_success");
    }

    @GetMapping("/facebook/callback")
    public void facebookCallback(@RequestParam String code, @RequestParam String state, HttpServletResponse response) throws IOException {
        service.processFacebookCallback(code, state);
        response.sendRedirect("http://127.0.0.1:5501/Settings.html?status=fb_success");
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Boolean>> getIntegrationStatus(@RequestParam String email) {
        // Repository se data nikalo
        agencyIntegrationModel model = service.getIntegrationData(email);
        
     // Debugging: Console mein dekho model mil raha hai ya nahi
        if(model != null) {
            System.out.println("API Status for " + email + ": Google=" + model.isGoogleConnected());
        } else {
            System.out.println("API Status: No model found for " + email);
        }
        
        Map<String, Boolean> status = new HashMap<>();
        status.put("google", model != null && model.isGoogleConnected());
        status.put("facebook", model != null && model.isFbConnected());
        
        return ResponseEntity.ok(status);
    }
    
    @GetMapping("/disconnect")
    public ResponseEntity<Map<String, String>> disconnect(@RequestParam String email, @RequestParam String platform) {
        service.disconnectPlatform(email, platform);
        return ResponseEntity.ok(Collections.singletonMap("message", platform + " disconnected successfully"));
    }
}