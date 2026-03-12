package com.project.agency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.project.common.models.agencyAddCampaignModel;
import com.project.common.service.LocationService;

@RestController
@RequestMapping("/api/agency/locations")
public class agencyLocationController {

    @Autowired
    private LocationService locationService;

    @GetMapping("/all")
    public ResponseEntity<?> getAllLocationAnalytics(@RequestHeader("X-Agency-Id") Long agencyId) {
        
        List<Map<String, Object>> rawData = locationService.getAllLocationAnalytics(agencyId);
        List<Map<String, Object>> formattedData = new ArrayList<>();

        for (Map<String, Object> row : rawData) {
            Map<String, Object> map = new HashMap<>(row);
            
            Number leadsNum = (Number) map.getOrDefault("leads", 0);
            long leads = (leadsNum != null) ? leadsNum.longValue() : 0;
            
            String activity;
            if (leads > 100) activity = "High";
            else if (leads >= 50) activity = "Medium";
            else activity = "Low";

            map.put("activity", activity);
            
            Number rateNum = (Number) map.get("conversionRate");
            double rate = (rateNum != null) ? rateNum.doubleValue() : 0.0;
            map.put("conversionRate", Math.round(rate * 100.0) / 100.0);

            formattedData.add(map);
        }
        return ResponseEntity.ok(formattedData);
    }
    
    @GetMapping("/details")
    public ResponseEntity<?> getLocationDetails(
            @RequestParam String name, 
            @RequestHeader("X-Agency-Id") Long agencyId) {
        
        List<agencyAddCampaignModel> campaigns = locationService.findCampaignsByLocation(name, agencyId);
        
        if (campaigns.isEmpty()) {
            return ResponseEntity.status(404).body("No data found for location: " + name);
        }

        long totalLeads = campaigns.stream().mapToLong(c -> c.getLeads() != null ? c.getLeads() : 0).sum();
        long totalConversions = campaigns.stream().mapToLong(c -> c.getTotalConversions() != null ? c.getTotalConversions() : 0).sum();
        double avgConvRate = campaigns.stream()
                .mapToDouble(c -> c.getConversionRate() != null ? c.getConversionRate() : 0.0)
                .average().orElse(0.0);

        Map<String, Object> response = new HashMap<>();
        response.put("locationName", name);
        response.put("leads", totalLeads);
        response.put("conversions", totalConversions);
        response.put("conversionRate", Math.round(avgConvRate * 100.0) / 100.0);
        response.put("campaigns", campaigns); 

        return ResponseEntity.ok(response);
    }
}