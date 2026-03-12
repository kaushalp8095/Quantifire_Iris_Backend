package com.project.agency;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate; 

import com.project.common.models.adminAddAgenciesModel;
import com.project.common.models.agencyLoginHistory;
import com.project.common.service.AgencyService;
import com.project.common.service.EmailService;
import com.project.agency.dto.agencyLoginRequest;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/agency")
@CrossOrigin(origins = "*") 
public class agencyLoginController {

    @Autowired
    private AgencyService agencyService; 
    
    @Autowired
    private EmailService emailService;

    // ==========================================
    // HELPER 1: USER AGENT PARSER (Chrome, Windows, etc.)
    // ==========================================
    private String parseUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) return "Unknown Device";
        
        String browser = "Web Browser";
        if (userAgent.contains("Edg/")) browser = "Edge";
        else if (userAgent.contains("Chrome/")) browser = "Chrome";
        else if (userAgent.contains("Firefox/")) browser = "Firefox";
        else if (userAgent.contains("Safari/") && !userAgent.contains("Chrome/")) browser = "Safari";

        String os = "Unknown OS";
        if (userAgent.contains("Windows NT 10.0")) os = "Windows 10/11";
        else if (userAgent.contains("Windows NT")) os = "Windows";
        else if (userAgent.contains("Android")) os = "Android";
        else if (userAgent.contains("iPhone")) os = "iOS";
        else if (userAgent.contains("Mac")) os = "MacOS";
        else if (userAgent.contains("Linux")) os = "Linux";
        
        return browser + " on " + os;
    }

    // ==========================================
    // HELPER 2: DYNAMIC LOCATION (IP-API)
    // ==========================================
    @SuppressWarnings("unchecked")
    private String fetchLocationFromIp(String ip) {
        // Localhost checking
        if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")) {
            return "Localhost (Dev)";
        }
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "http://ip-api.com/json/" + ip;
            
            // API call to get city and country
            Map<String, Object> apiResponse = restTemplate.getForObject(url, Map.class);
            
            if (apiResponse != null && "success".equals(apiResponse.get("status"))) {
                return apiResponse.get("city") + ", " + apiResponse.get("country");
            }
        } catch (Exception e) {
            System.err.println("Location API Error: " + e.getMessage());
        }
        return "India"; // Default fallback
    }

    // ==========================================
    // MAIN LOGIN API
    // ==========================================
    @PostMapping("/login")
    public ResponseEntity<?> loginAgency(@RequestBody agencyLoginRequest loginRequest, 
                                         HttpServletResponse response,
                                         HttpServletRequest request) {
        
        Map<String, String> respMap = new HashMap<>();
        Optional<adminAddAgenciesModel> agencyOpt = agencyService.findByEmail(loginRequest.getEmail());

        if (agencyOpt.isPresent() && agencyOpt.get().getPassword().equals(loginRequest.getPassword())) {
            
            adminAddAgenciesModel agency = agencyOpt.get();

            // 1. Cookies Setup
            setupCookies(response, agency.getEmail());

            // 2. Login History Tracking (Try-Catch taaki login fail na ho)
            try {
                agencyLoginHistory loginLog = new agencyLoginHistory();
                loginLog.setAgencyEmail(agency.getEmail()); 
                loginLog.setLoginTime(LocalDateTime.now());

                // IP Address Fetching
                String ipStr = request.getHeader("X-Forwarded-For");
                if (ipStr == null || ipStr.isEmpty() || "unknown".equalsIgnoreCase(ipStr)) {
                    ipStr = request.getRemoteAddr();
                }
                loginLog.setIpAddress(ipStr);

                // Set Clean Device Info
                loginLog.setDeviceInfo(parseUserAgent(request.getHeader("User-Agent")));

                // Set Dynamic Location
                loginLog.setLocation(fetchLocationFromIp(ipStr)); 

                // Save to Dual DB
                agencyService.saveLoginHistory(loginLog);
                
             // 🔴 NAYA LOGIC: Agar Login Alert ON hai, toh Email bhejo
                if (agency.isLoginAlertsEnabled()) {
                    // Run in a separate thread so login is not delayed
                    new Thread(() -> {
                        emailService.sendLoginAlertEmail(
                            agency.getEmail(), 
                            loginLog.getDeviceInfo(), 
                            loginLog.getLocation(), 
                            loginLog.getIpAddress()
                        );
                    }).start();
                }
                
            } catch (Exception e) {
                System.err.println("Login History Logging Failed: " + e.getMessage());
            }

            // 3. Final Response
            respMap.put("status", "success");
            respMap.put("agencyId", agency.getId().toString());
            respMap.put("message", "Login Successful");
            respMap.put("agencyName", agency.getAgencyName()); 
            
            return ResponseEntity.ok(respMap);
        } 
        else {
            respMap.put("status", "error");
            respMap.put("message", "Invalid Email or Password"); 
            return ResponseEntity.status(401).body(respMap);
        }
    }

    // HELPER: Cookie Management
    private void setupCookies(HttpServletResponse response, String email) {
        Cookie emailCookie = new Cookie("agencyEmail", email);
        emailCookie.setHttpOnly(true); 
        emailCookie.setPath("/"); 
        emailCookie.setMaxAge(24 * 60 * 60);
        response.addCookie(emailCookie);

        Cookie loginCookie = new Cookie("isAgencyLoggedIn", "true");
        loginCookie.setHttpOnly(false); 
        loginCookie.setPath("/"); 
        loginCookie.setMaxAge(24 * 60 * 60);
        response.addCookie(loginCookie);
    }
}