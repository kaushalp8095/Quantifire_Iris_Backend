package com.project.agency;

import java.io.File; 
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

@RestController
@RequestMapping("/api/agency") 
public class agencySettingsController {

    @Autowired
    private AgencyService agencyService;
    
    @Autowired
    private EmailService emailService;
    
    private Map<String, String> otpCache = new HashMap<>();
    
    private final String UPLOAD_DIR = "C:/Users/user/eclipse-workspace/Quantifyre_Iris_SuperAdmin/uploads/logos/";

    // ==========================================
    // 1. GET PROFILE DATA (Original)
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
    // 2. UPDATE PROFILE DATA & LOGO (Original)
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
                
                existingAgency.setOwnerName(updatedData.getOwnerName());
                existingAgency.setPhoneNumber(updatedData.getPhoneNumber());
                existingAgency.setAddress(updatedData.getAddress());
                existingAgency.setCountry(updatedData.getCountry());
                existingAgency.setState(updatedData.getState());
                existingAgency.setCity(updatedData.getCity());
                existingAgency.setPincode(updatedData.getPincode());
                
                // Logo Update Logic
                if (logo != null && !logo.isEmpty()) {
                    
                    // --- 🔴 DELETE OLD LOGO (Fixed Pathing) ---
                    String oldLogoFileName = existingAgency.getAgencyLogo();
                    if (oldLogoFileName != null && !oldLogoFileName.trim().isEmpty()) {
                        File oldFile = new File(UPLOAD_DIR + oldLogoFileName);
                        if (oldFile.exists() && oldFile.isFile()) {
                            if (oldFile.delete()) {
                                System.out.println("✅ Purana logo delete ho gaya: " + oldLogoFileName);
                            } else {
                                System.err.println("❌ Purana logo delete nahi hua: File is in use or locked");
                            }
                        } else {
                            System.out.println("⚠️ Purani file nahi mili, sayad pehle hi delete ho chuki hai: " + oldLogoFileName);
                        }
                    }
                    
                    // --- SAVE NEW LOGO ---
                    String fileName = System.currentTimeMillis() + "_" + logo.getOriginalFilename();
                    Path path = Paths.get(UPLOAD_DIR + fileName);
                    Files.createDirectories(path.getParent()); 
                    Files.write(path, logo.getBytes());
                    
                    existingAgency.setAgencyLogo(fileName);
                }
                
                agencyService.saveAgency(existingAgency);
                return ResponseEntity.ok(Map.of("message", "Profile updated successfully!", "status", "success"));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Agency not found"));
            }
        } catch (Exception e) {
            e.printStackTrace(); 
            return ResponseEntity.status(500).body(Map.of("error", "Server Error: " + e.getMessage()));
        }
    }


    // =========================================================
    // 3. SECURITY APIS (NEWLY ADDED)
    // =========================================================

    // A. CHANGE PASSWORD
    @PostMapping("/security/change-password")
    public ResponseEntity<?> changePassword(@RequestBody agencySecurityRequestDto request) {
        try {
            Optional<adminAddAgenciesModel> agencyOpt = agencyService.findByEmail(request.getEmail());
            if (agencyOpt.isPresent()) {
                adminAddAgenciesModel agency = agencyOpt.get();

                // Note: Agar aap BCrypt use nahi kar rahe toh simple text match.
                // Best practice is to use BCryptPasswordEncoder in future.
                if (agency.getPassword() != null && agency.getPassword().equals(request.getCurrentPassword())) {
                    agency.setPassword(request.getNewPassword()); 
                    agencyService.saveAgency(agency); // Save using your existing service
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

    // B. GET SECURITY PREFERENCES (For toggles)
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

    // C. UPDATE SECURITY PREFERENCES (When toggles are changed)
    @PostMapping("/security/update-preferences")
    public ResponseEntity<?> updatePreferences(@RequestBody agencySecurityRequestDto request) {
        Optional<adminAddAgenciesModel> agencyOpt = agencyService.findByEmail(request.getEmail());
        if (agencyOpt.isPresent()) {
            adminAddAgenciesModel agency = agencyOpt.get();
            
            // Only update fields that are provided
            if (request.getIs2faEnabled() != null) agency.setIs2faEnabled(request.getIs2faEnabled());
            if (request.getTfaMethod() != null) agency.setTfaMethod(request.getTfaMethod());
            if (request.getLoginAlertsEnabled() != null) agency.setLoginAlertsEnabled(request.getLoginAlertsEnabled());

            agencyService.saveAgency(agency); 
            return ResponseEntity.ok(Map.of("message", "Preferences updated"));
        }
        return ResponseEntity.status(404).body(Map.of("error", "Agency not found"));
    }

    // D. GET RECENT LOGIN HISTORY
    @GetMapping("/security/login-history")
    public ResponseEntity<List<agencyLoginHistory>> getLoginHistory(@RequestParam String email) {
        List<agencyLoginHistory> history = agencyService.getLoginHistory(email);
        return ResponseEntity.ok(history);
    }
    
 // =========================================================
    // 1. SEND OTP API
    // =========================================================
    @PostMapping("/security/request-otp")
    public ResponseEntity<?> requestOtp(@RequestBody Map<String, String> request) {
        String method = request.get("method"); // "email" ya "mobile"
        String target = request.get("target"); // email id ya phone number
        
        if (target == null || target.trim().isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("message", "Target address is required"));
        }

        // 6-Digit Random OTP Generate karein
        String otp = String.valueOf((int)((Math.random() * 900000) + 100000));
        
        // OTP ko memory mein save karein
        otpCache.put(target, otp); 
        
        if ("email".equalsIgnoreCase(method)) {
            // Asli Email bhejein
            emailService.sendOtpEmail(target, otp);
            return ResponseEntity.ok(Map.of("message", "OTP sent successfully to " + target));
        } else {
            // Mobile (SMS) abhi integrate nahi hai, toh console me print karenge testing ke liye
            System.out.println("📱 SMS OTP for " + target + " is: " + otp);
            return ResponseEntity.ok(Map.of("message", "SMS feature coming soon! Check backend console for OTP."));
        }
    }

    // =========================================================
    // 2. VERIFY OTP & ENABLE 2FA API
    // =========================================================
    @PostMapping("/security/verify-2fa")
    public ResponseEntity<?> verify2fa(@RequestBody Map<String, String> request) {
        String target = request.get("target"); // jo email/phone user ne daala tha
        String userOtp = request.get("otp");   // jo OTP user ne form mein daala
        String loggedInEmail = request.get("email"); // Current logged in agency ki email

        // Check karein ki OTP sahi hai ya nahi
        if (otpCache.containsKey(target) && otpCache.get(target).equals(userOtp)) {
            
            // OTP Sahi hai! Ab Database mein 2FA ON karein
            Optional<adminAddAgenciesModel> agencyOpt = agencyService.findByEmail(loggedInEmail);
            
            if (agencyOpt.isPresent()) {
                adminAddAgenciesModel agency = agencyOpt.get();
                agency.setIs2faEnabled(true);
                // Agar email mein @ hai toh email set karo, warna sms set karo
                agency.setTfaMethod(target.contains("@") ? "email" : "sms");
                
                agencyService.saveAgency(agency); 
                
                otpCache.remove(target); 
                return ResponseEntity.ok(Map.of("success", true, "message", "2FA Successfully Activated!"));
            }
        }
        
        return ResponseEntity.status(400).body(Map.of("success", false, "message", "Invalid OTP! Please try again."));
    }
    
 // =======================================================
    // 1. GET API - Notifications Load Karne Ke Liye
    // =======================================================
    @GetMapping("/notifications")
    public ResponseEntity<?> getNotificationSettings(@RequestParam String email) {
        Optional<adminAddAgenciesModel> agencyOpt = agencyService.findByEmail(email);
        if (agencyOpt.isPresent()) {
            agencyNotificationSettings settings = agencyOpt.get().getNotificationSettings();
            
            // FIX: Agar purane data mein settings null hain, toh default object bhej do
            if (settings == null) {
                settings = new agencyNotificationSettings(); 
            }
            return ResponseEntity.ok(settings);
        }
        return ResponseEntity.status(404).body(Map.of("message", "Agency not found"));
    }

    // =======================================================
    // 2. DTO Class - Frontend se data pakadne ke liye
    // =======================================================
    public static class NotifRequest {
        private String email;
        private agencyNotificationSettings settings;
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public agencyNotificationSettings getSettings() { return settings; }
        public void setSettings(agencyNotificationSettings settings) { this.settings = settings; }
    }

    // =======================================================
    // 3. POST API - Notifications Save Karne Ke Liye
    // =======================================================
    @PostMapping("/notifications/update")
    public ResponseEntity<?> updateNotificationSettings(@RequestBody NotifRequest requestData) {
        Optional<adminAddAgenciesModel> agencyOpt = agencyService.findByEmail(requestData.getEmail());
        
        if (agencyOpt.isPresent()) {
            adminAddAgenciesModel agency = agencyOpt.get();
            
            // Pura notification settings ek hi baar me replace kar diya
            agency.setNotificationSettings(requestData.getSettings());
            
            agencyService.saveAgency(agency); // Save into Database
            return ResponseEntity.ok(Map.of("message", "Notifications updated successfully!"));
        }
        return ResponseEntity.status(404).body(Map.of("message", "Agency not found"));
    }
    
}