package com.project.agency;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate; 

import com.project.common.models.adminAddAgenciesModel;
import com.project.common.models.agencyLoginHistory;
import com.project.common.service.AgencyService;
import com.project.common.service.EmailService;
import com.project.agency.dto.agencyLoginRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/agency")
// ✅ Fully Secure CORS Configuration (Local + Vercel)
@CrossOrigin(origins = {"http://127.0.0.1:5500", "https://quantifire-iris-frontend.vercel.app"}, allowCredentials = "true") 
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

        // Email ko Case-Insensitive banane ke liye trim aur lowercase karein
        String inputEmail = (loginRequest.getEmail() != null) ? loginRequest.getEmail().toLowerCase().trim() : "";
        String inputPassword = (loginRequest.getPassword() != null) ? loginRequest.getPassword().trim() : "";

        Optional<adminAddAgenciesModel> agencyOpt = agencyService.findByEmail(inputEmail);

        if (agencyOpt.isPresent()) {
            adminAddAgenciesModel agency = agencyOpt.get();
            
            // Password Match check
            if (agency.getPassword() != null && agency.getPassword().equals(inputPassword)) {

                // 1. Cookies Setup (Cross-Domain Secure Cookies)
            	setupCookies(response, agency.getId().toString());

                // 2. Login History Tracking
                try {
                    agencyLoginHistory loginLog = new agencyLoginHistory();
                    loginLog.setAgencyEmail(agency.getEmail()); 
                    loginLog.setLoginTime(LocalDateTime.now());

                    // IP Address Fetching (Handle Render/Proxy)
                    String ipStr = request.getHeader("X-Forwarded-For");
                    if (ipStr == null || ipStr.isEmpty() || "unknown".equalsIgnoreCase(ipStr)) {
                        ipStr = request.getRemoteAddr();
                    }
                    loginLog.setIpAddress(ipStr);

                    // Set Device & Location info
                    loginLog.setDeviceInfo(parseUserAgent(request.getHeader("User-Agent")));
                    loginLog.setLocation(fetchLocationFromIp(ipStr)); 

                    // Save to Dual DB (SQL + Mongo Backup)
                    agencyService.saveLoginHistory(loginLog);
                    
                    // 3. Login Alert Logic (Safe Threading)
                    if (agency.isLoginAlertsEnabled()) {
                        String finalIp = ipStr;
                        new Thread(() -> {
                            try {
                                emailService.sendLoginAlertEmail(
                                    agency.getEmail(), 
                                    loginLog.getDeviceInfo(), 
                                    loginLog.getLocation(), 
                                    finalIp
                                );
                                System.out.println("✅ Login Alert Sent to: " + agency.getEmail());
                            } catch (Exception ex) {
                                System.err.println("❌ Email Alert Thread Error: " + ex.getMessage());
                            }
                        }).start();
                    }
                    
                } catch (Exception e) {
                    System.err.println("❌ Login History Logging Failed: " + e.getMessage());
                }

                // 4. Final Success Response
                respMap.put("status", "success");
                respMap.put("agencyId", agency.getId().toString());
                respMap.put("message", "Login Successful");
                respMap.put("agencyName", agency.getAgencyName()); 
                respMap.put("agencyEmail", agency.getEmail());
                
                return ResponseEntity.ok(respMap);
            }
        } 

        // Login Failed response
        respMap.put("status", "error");
        respMap.put("message", "Invalid Email or Password"); 
        return ResponseEntity.status(401).body(respMap);
    }

    // ==========================================
    // HELPER 3: COOKIE MANAGEMENT (Vercel + Render Secure)
    // ==========================================
    private void setupCookies(HttpServletResponse response, String agencyId) {
        // Sirf 1 Secure Cookie banaiye
        ResponseCookie sessionCookie = ResponseCookie.from("agency_session", agencyId)
                .httpOnly(true)    // Hacker se safe
                .secure(true)      // HTTPS ke liye zaroori (Vercel/Render)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("None")  // Cross-domain ke liye
                .build();
                
        // Header mein set karein
        response.setHeader(HttpHeaders.SET_COOKIE, sessionCookie.toString());
    }
}