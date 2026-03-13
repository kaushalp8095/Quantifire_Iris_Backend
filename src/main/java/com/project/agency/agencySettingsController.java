package com.project.agency;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import com.project.common.dto.agencySecurityRequestDto;
import com.project.common.models.adminAddAgenciesModel;
import com.project.common.models.agencyLoginHistory;
import com.project.common.models.agencyNotificationSettings;
import com.project.common.service.AgencyService;
import com.project.common.service.EmailService;
import com.project.common.service.SupabaseStorageService; // 👈 Naya Service Import

@RestController
@RequestMapping("/api/agency") 
public class agencySettingsController {

    @Autowired
    private AgencyService agencyService;
    
    @Autowired
    private EmailService emailService;

    @Autowired
    private SupabaseStorageService storageService; // 👈 S3 Service Inject kiya
    
    private Map<String, String> otpCache = new HashMap<>();

    // ==========================================
    // 1. GET PROFILE DATA
    // ==========================================
    @GetMapping("/profile")
    public ResponseEntity<?> getAgencyProfile(@RequestParam String email) {
        try {
            Optional<adminAddAgenciesModel> agencyOpt = agencyService.findByEmail(email);
            if (agencyOpt.isPresent()) {
                return ResponseEntity.ok(agencyOpt.get());
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Agency not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error: " + e.getMessage()));
        }
    }

    // ==========================================
    // 2. UPDATE PROFILE DATA & LOGO (SUPABASE S3)
    // ==========================================
    @PostMapping("/update-profile")
    public ResponseEntity<?> updateAgencyProfile(
            @RequestParam("agencyData") String agencyJson,
            @RequestParam(value = "agencyLogo", required = false) MultipartFile logo) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
            adminAddAgenciesModel updatedData = mapper.readValue(agencyJson, adminAddAgenciesModel.class);
            Optional<adminAddAgenciesModel> agencyOpt = agencyService.findByEmail(updatedData.getEmail());
            
            if (agencyOpt.isPresent()) {
                adminAddAgenciesModel existingAgency = agencyOpt.get();
                
                // Fields Update
                existingAgency.setOwnerName(updatedData.getOwnerName());
                existingAgency.setPhoneNumber(updatedData.getPhoneNumber());
                existingAgency.setAddress(updatedData.getAddress());
                existingAgency.setCountry(updatedData.getCountry());
                existingAgency.setState(updatedData.getState());
                existingAgency.setCity(updatedData.getCity());
                existingAgency.setPincode(updatedData.getPincode());
                
                // --- 🟢 SUPABASE S3 LOGO UPLOAD ---
                if (logo != null && !logo.isEmpty()) {
                    // Seedha cloud par upload karega aur Public URL laayega
                    String publicImageUrl = storageService.uploadFile(logo, "agency-logos");
                    existingAgency.setAgencyLogo(publicImageUrl);
                }
                
                // SQL + MongoDB dono mein save karega (via AgencyService)
                agencyService.saveAgency(existingAgency);
                
                return ResponseEntity.ok(Map.of(
                    "message", "Profile updated successfully!", 
                    "status", "success",
                    "logoUrl", existingAgency.getAgencyLogo()
                ));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Agency not found"));
            }
        } catch (Exception e) {
            e.printStackTrace(); 
            return ResponseEntity.status(500).body(Map.of("error", "Server Error: " + e.getMessage()));
        }
    }

    // =========================================================
    // 3. SECURITY APIS
    // =========================================================

    @PostMapping("/security/change-password")
    public ResponseEntity<?> changePassword(@RequestBody agencySecurityRequestDto request) {
        try {
            Optional<adminAddAgenciesModel> agencyOpt = agencyService.findByEmail(request.getEmail());
            if (agencyOpt.isPresent()) {
                adminAddAgenciesModel agency = agencyOpt.get();
                if (agency.getPassword() != null && agency.getPassword().equals(request.getCurrentPassword())) {
                    agency.setPassword(request.getNewPassword()); 
                    agencyService.saveAgency(agency);
                    return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
                } else {
                    return ResponseEntity.status(401).body(Map.of("message", "Incorrect current password"));
                }
            }
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Server Error: " + e.getMessage()));
        }
    }

    @GetMapping("/security/preferences")
    public ResponseEntity<?> getPreferences(@RequestParam String email) {
        Optional<adminAddAgenciesModel> agencyOpt = agencyService.findByEmail(email);
        if (agencyOpt.isPresent()) {
            adminAddAgenciesModel agency = agencyOpt.get();
            return ResponseEntity.ok(Map.of(
                    "is2faEnabled", agency.isIs2faEnabled(),
                    "tfaMethod", agency.getTfaMethod() != null ? agency.getTfaMethod() : "email",
                    "loginAlertsEnabled", agency.isLoginAlertsEnabled()
            ));
        }
        return ResponseEntity.status(404).body(Map.of("error", "Agency not found"));
    }

    @PostMapping("/security/update-preferences")
    public ResponseEntity<?> updatePreferences(@RequestBody agencySecurityRequestDto request) {
        Optional<adminAddAgenciesModel> agencyOpt = agencyService.findByEmail(request.getEmail());
        if (agencyOpt.isPresent()) {
            adminAddAgenciesModel agency = agencyOpt.get();
            if (request.getIs2faEnabled() != null) agency.setIs2faEnabled(request.getIs2faEnabled());
            if (request.getTfaMethod() != null) agency.setTfaMethod(request.getTfaMethod());
            if (request.getLoginAlertsEnabled() != null) agency.setLoginAlertsEnabled(request.getLoginAlertsEnabled());

            agencyService.saveAgency(agency); 
            return ResponseEntity.ok(Map.of("message", "Preferences updated"));
        }
        return ResponseEntity.status(404).body(Map.of("error", "Agency not found"));
    }

    @GetMapping("/security/login-history")
    public ResponseEntity<List<agencyLoginHistory>> getLoginHistory(@RequestParam String email) {
        List<agencyLoginHistory> history = agencyService.getLoginHistory(email);
        return ResponseEntity.ok(history);
    }
    
    @PostMapping("/security/request-otp")
    public ResponseEntity<?> requestOtp(@RequestBody Map<String, String> request) {
        String method = request.get("method");
        String target = request.get("target");
        
        if (target == null || target.trim().isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("message", "Target address is required"));
        }

        String otp = String.valueOf((int)((Math.random() * 900000) + 100000));
        otpCache.put(target, otp); 
        
        if ("email".equalsIgnoreCase(method)) {
            emailService.sendOtpEmail(target, otp);
            return ResponseEntity.ok(Map.of("message", "OTP sent to " + target));
        } else {
            System.out.println("📱 SMS OTP for " + target + " is: " + otp);
            return ResponseEntity.ok(Map.of("message", "Check console for OTP (SMS mode)"));
        }
    }

    @PostMapping("/security/verify-2fa")
    public ResponseEntity<?> verify2fa(@RequestBody Map<String, String> request) {
        String target = request.get("target");
        String userOtp = request.get("otp");
        String loggedInEmail = request.get("email");

        if (otpCache.containsKey(target) && otpCache.get(target).equals(userOtp)) {
            Optional<adminAddAgenciesModel> agencyOpt = agencyService.findByEmail(loggedInEmail);
            if (agencyOpt.isPresent()) {
                adminAddAgenciesModel agency = agencyOpt.get();
                agency.setIs2faEnabled(true);
                agency.setTfaMethod(target.contains("@") ? "email" : "sms");
                agencyService.saveAgency(agency); 
                otpCache.remove(target); 
                return ResponseEntity.ok(Map.of("success", true, "message", "2FA Activated!"));
            }
        }
        return ResponseEntity.status(400).body(Map.of("success", false, "message", "Invalid OTP!"));
    }

    // =======================================================
    // 4. NOTIFICATION SETTINGS
    // =======================================================
    @GetMapping("/notifications")
    public ResponseEntity<?> getNotificationSettings(@RequestParam String email) {
        Optional<adminAddAgenciesModel> agencyOpt = agencyService.findByEmail(email);
        if (agencyOpt.isPresent()) {
            agencyNotificationSettings settings = agencyOpt.get().getNotificationSettings();
            if (settings == null) settings = new agencyNotificationSettings(); 
            return ResponseEntity.ok(settings);
        }
        return ResponseEntity.status(404).body(Map.of("message", "Agency not found"));
    }

    @PostMapping("/notifications/update")
    public ResponseEntity<?> updateNotificationSettings(@RequestBody NotifRequest requestData) {
        Optional<adminAddAgenciesModel> agencyOpt = agencyService.findByEmail(requestData.getEmail());
        if (agencyOpt.isPresent()) {
            adminAddAgenciesModel agency = agencyOpt.get();
            agency.setNotificationSettings(requestData.getSettings());
            agencyService.saveAgency(agency); 
            return ResponseEntity.ok(Map.of("message", "Notifications updated!"));
        }
        return ResponseEntity.status(404).body(Map.of("message", "Agency not found"));
    }

    // DTO for Notifications
    public static class NotifRequest {
        private String email;
        private agencyNotificationSettings settings;
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public agencyNotificationSettings getSettings() { return settings; }
        public void setSettings(agencyNotificationSettings settings) { this.settings = settings; }
    }
}