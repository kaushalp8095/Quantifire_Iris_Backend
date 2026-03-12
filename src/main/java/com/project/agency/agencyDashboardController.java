package com.project.agency;

import com.project.common.models.agencyAddCampaignModel;
import com.project.common.models.agencyReportCountModel;
import com.project.common.repository.jpa.agencyAddCampaignRepository;
import com.project.common.repository.jpa.agencyClientRepository;
import com.project.common.repository.jpa.agencyReportCountRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/agency/dashboard")
public class agencyDashboardController {

    @Autowired
    private agencyAddCampaignRepository campaignRepo;

    @Autowired
    private agencyClientRepository clientRepo;
    
    @Autowired
    private agencyReportCountRepository reportCountRepo;

    @GetMapping("/summary")
    public ResponseEntity<?> getDashboardSummary(@RequestHeader("X-Agency-Id") Long agencyId) { 
        Map<String, Object> response = new HashMap<>();

        // 1. Client Metrics 
        List<Map<String, Object>> clientMetrics = clientRepo.getClientsWithPerformanceMetricsByAgency(agencyId);
        response.put("clientInsights", clientMetrics != null ? clientMetrics : new ArrayList<>());

        // 2. Stats Calculation (Fix: count() ki jagah clientMetrics ki size use karein)
        response.put("totalClients", clientMetrics != null ? clientMetrics.size() : 0); 
        
        long totalLeads = clientMetrics.stream()
            .mapToLong(m -> m.get("leads") != null ? ((Number) m.get("leads")).longValue() : 0L).sum();
        response.put("totalLeads", totalLeads);

        long activeCampaigns = clientMetrics.stream()
            .mapToLong(m -> m.get("activeCampaignsCount") != null ? ((Number) m.get("activeCampaignsCount")).longValue() : 0L).sum();
        response.put("totalCampaigns", activeCampaigns);

        // 3. Leads by Source Logic
        Map<String, Long> sourceStats = new HashMap<>();
        sourceStats.put("Google Ads", 0L);
        sourceStats.put("Facebook", 0L);
        sourceStats.put("Instagram", 0L);
        sourceStats.put("Whatsapp", 0L);

        // FIX: findAll() ki jagah findByAgencyId() use kiya
        List<agencyAddCampaignModel> allCampaigns = campaignRepo.findByAgencyId(agencyId); 
        
        allCampaigns.forEach(c -> {
            String platform = c.getAdPlatform();
            if (platform != null) {
                String lowerPlatform = platform.toLowerCase();
                int leads = (c.getLeads() != null) ? c.getLeads() : 0;

                if (lowerPlatform.contains("google")) {
                    sourceStats.put("Google Ads", sourceStats.get("Google Ads") + leads);
                } else if (lowerPlatform.contains("meta") || lowerPlatform.contains("facebook")) {
                    sourceStats.put("Facebook", sourceStats.get("Facebook") + leads);
                } else if (lowerPlatform.contains("instagram")) {
                    sourceStats.put("Instagram", sourceStats.get("Instagram") + leads);
                } else if (lowerPlatform.contains("whatsapp")) {
                    sourceStats.put("Whatsapp", sourceStats.get("Whatsapp") + leads);
                }
            }
        });
        
        response.put("sourceStats", sourceStats);
        response.put("allCampaignsList", allCampaigns); 

        // 4. Map Data 
        List<Map<String, Object>> mapPoints = new ArrayList<>();
        allCampaigns.stream()
            .filter(c -> c.getTargetLocation() != null && !c.getTargetLocation().isEmpty())
            .forEach(c -> {
                Map<String, Object> point = new HashMap<>();
                point.put("name", c.getCampaignName());
                point.put("location", c.getTargetLocation());
                point.put("status", c.getStatus()); 
                mapPoints.add(point);
            });
            
        response.put("mapPoints", mapPoints);
        
        agencyReportCountModel stats = reportCountRepo.findByAgencyId(agencyId);
        long downloadCount = (stats != null && stats.getReportDownloadCount() != null) 
                             ? stats.getReportDownloadCount() : 0L;
        
        // Response mein "totalReportDownloaded" key ke sath bhejein
        response.put("totalReportDownloaded", downloadCount);

        return ResponseEntity.ok(response);
    }
}