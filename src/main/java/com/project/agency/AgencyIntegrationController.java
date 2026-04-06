package com.project.agency;

import com.project.common.models.agencyIntegrationModel;
import com.project.common.service.AgencyIntegrationService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.*;


@RestController
@RequestMapping("/api/integration")
@CrossOrigin(origins = {"http://127.0.0.1:5501", "http://localhost:5501", "https://quantifire-iris-frontend.vercel.app/"})
public class AgencyIntegrationController {

    @Autowired private AgencyIntegrationService service;

    @Value("${google.ads.client-id}") private String googleClientId;
    @Value("${google.ads.callback-url}") private String googleRedirectUri;
    @Value("${facebook.app-id}") private String fbAppId;
    @Value("${facebook.callback-url}") private String fbRedirectUri;

    @GetMapping("/google/connect")
    public void connectGoogle(@RequestParam String email, HttpServletResponse response) throws IOException {
        String url = "https://accounts.google.com/o/oauth2/v2/auth?client_id=" + googleClientId +
                     "&redirect_uri=" + googleRedirectUri + "&response_type=code" +
                     "&scope=https://www.googleapis.com/auth/adwords&access_type=offline&prompt=consent&state=" + email;
                     
        // JSON return karne ke bajaye, seedha redirect kar rahe hain
        response.sendRedirect(url);
    }

    @GetMapping("/facebook/connect")
    public void connectFb(@RequestParam String email, HttpServletResponse response) throws IOException {
        String url = "https://www.facebook.com/v18.0/dialog/oauth?client_id=" + fbAppId +
                     "&redirect_uri=" + fbRedirectUri + "&response_type=code" +
                     "&scope=ads_management,ads_read,business_management&state=" + email;
                     
        // JSON return karne ke bajaye, seedha redirect kar rahe hain
        response.sendRedirect(url);
    }
    
    // --- CALLBACK HANDLERS ---
    @GetMapping("/google/callback")
    public void googleCallback(@RequestParam String code, @RequestParam String state, HttpServletResponse response) throws IOException {
    	String[] parsedState = parseState(state);
        String email = parsedState[0];
        String sourcePage = parsedState[1];
    	
    	service.processGoogleCallback(code, email); // state mein email hai
    	response.sendRedirect(buildRedirectUrl(sourcePage, "google_success"));
    }

    @GetMapping("/facebook/callback")
    public void facebookCallback(@RequestParam String code, @RequestParam String state, HttpServletResponse response) throws IOException {
    	String[] parsedState = parseState(state);
        String email = parsedState[0];
        String sourcePage = parsedState[1];
    	
    	service.processFacebookCallback(code, email);
    	response.sendRedirect(buildRedirectUrl(sourcePage, "fb_success"));
    }
    
 // --- HELPER METHODS FOR ROUTING ---
    private String[] parseState(String state) {
        // Default values
        String email = state; 
        String sourcePage = "settings"; 

        if (state != null && state.contains("|")) {
            String[] parts = state.split("\\|");
            email = parts[0];
            if (parts.length > 1) {
                sourcePage = parts[1];
            }
        }
        return new String[]{email, sourcePage};
    }
    
    private String buildRedirectUrl(String sourcePage, String status) {
        String frontendBaseUrl =  "https://quantifire-iris-frontend.vercel.app/"; // Aapka frontend URL
        String platform = status.equals("google_success") ? "google" : "meta";

        // Agar user Add Campaign se aaya tha
        if ("add_campaign".equals(sourcePage)) {
            return frontendBaseUrl + "AddNewCampaign.html?platform=" + platform;
        } 
        // Agar user Edit Campaign se aaya tha (e.g. edit_campaign_12)
        else if (sourcePage != null && sourcePage.startsWith("edit_campaign_")) {
            String campaignId = sourcePage.replace("edit_campaign_", "");
            return frontendBaseUrl + "EditCampaign.html?id=" + campaignId + "&platform=" + platform;
        }
        
        // Default: Agar Settings se aaya tha
        return frontendBaseUrl + "Settings.html?status=" + status;
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
    
    @GetMapping("/sync-recent")
    public ResponseEntity<?> syncRecentCampaigns(@RequestParam String email, @RequestParam String platform) {
        try {
            // Service ko call karke data layenge  
            List<Map<String, Object>> recentCampaigns = service.fetchRecentCampaigns(email, platform);
            return ResponseEntity.ok(recentCampaigns);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
}