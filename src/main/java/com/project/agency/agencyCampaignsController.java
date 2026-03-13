package com.project.agency;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.models.agencyAddCampaignModel;
import com.project.common.service.CampaignService;
import com.project.common.service.SupabaseStorageService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/agency/campaigns")
@CrossOrigin(origins = "*") // Live deployment ke liye zaroori
public class agencyCampaignsController {

    @Autowired private CampaignService campaignService;
    @Autowired private SupabaseStorageService storageService;

    // ==========================================
    // 1. ADD NEW CAMPAIGN
    // ==========================================
    @PostMapping("/add")
    public ResponseEntity<?> addCampaign(
            @RequestParam("campaignData") String campaignJson, 
            @RequestParam(value = "image", required = false) MultipartFile file 
    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            agencyAddCampaignModel campaign = mapper.readValue(campaignJson, agencyAddCampaignModel.class);

            if (campaign.getClientId() == null) {
                return ResponseEntity.status(400).body(Map.of("error", "Client ID is required"));
            }

            // --- 🟢 SUPABASE S3 UPLOAD ---
            if (file != null && !file.isEmpty()) {
                String publicImageUrl = storageService.uploadFile(file, "campaign-creatives");
                campaign.setCreativePath(publicImageUrl); 
            }

            // Target Location Formatting
            if (campaign.getTargetLocation() != null) {
                String loc = campaign.getTargetLocation().trim();
                campaign.setTargetLocation(loc.substring(0, 1).toUpperCase() + loc.substring(1).toLowerCase());
            }

            // Conversion Rate Calculation
            calculateConversion(campaign);

            campaignService.saveOrUpdate(campaign);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Campaign Added Successfully!"));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // 2. UPDATE CAMPAIGN
    // ==========================================
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateCampaign(
            @PathVariable Long id,
            @RequestParam("campaignData") String campaignJson,
            @RequestParam(value = "image", required = false) MultipartFile file,
            @RequestHeader("X-Agency-Id") Long agencyId
    ) {
        try {
            Optional<agencyAddCampaignModel> existingOpt = campaignService.getByIdSecurely(id, agencyId);
            if (existingOpt.isEmpty()) return ResponseEntity.status(403).body("Unauthorized");

            agencyAddCampaignModel existingCamp = existingOpt.get();
            ObjectMapper mapper = new ObjectMapper();
            agencyAddCampaignModel newData = mapper.readValue(campaignJson, agencyAddCampaignModel.class);

            // Mapping New Data to Existing Entity
            existingCamp.setCampaignName(newData.getCampaignName());
            existingCamp.setClientId(newData.getClientId());
            existingCamp.setClientName(newData.getClientName());
            existingCamp.setBudget(newData.getBudget());
            existingCamp.setStartDate(newData.getStartDate());
            existingCamp.setEndDate(newData.getEndDate());
            existingCamp.setLeads(newData.getLeads());
            existingCamp.setTotalConversions(newData.getTotalConversions());
            existingCamp.setStatus(newData.getStatus());
            existingCamp.setAdPlatform(newData.getAdPlatform());
            existingCamp.setTargetLocation(newData.getTargetLocation());

            calculateConversion(existingCamp);

            // --- 🟢 SUPABASE S3 IMAGE UPDATE ---
            if (file != null && !file.isEmpty()) {
                String newUrl = storageService.uploadFile(file, "campaign-creatives");
                existingCamp.setCreativePath(newUrl);
            }

            campaignService.saveOrUpdate(existingCamp);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Updated Successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // 3. FETCH ALL (With Date Filters)
    // ==========================================
    @GetMapping("/all")
    public ResponseEntity<?> getAllCampaigns(
            @RequestParam(defaultValue = "all") String filter, 
            @RequestHeader("X-Agency-Id") Long agencyId)  {
        
        List<agencyAddCampaignModel> allCampaigns = campaignService.getCampaignsByAgency(agencyId);
        
        if (filter.equals("all")) return ResponseEntity.ok(allCampaigns);

        LocalDate today = LocalDate.now();
        LocalDate cutoffDate = filter.equals("7days") ? today.minusDays(7) :
                             filter.equals("1month") ? today.minusMonths(1) :
                             filter.equals("3months") ? today.minusMonths(3) : null;

        if (cutoffDate == null) return ResponseEntity.ok(allCampaigns);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM, yyyy", Locale.ENGLISH);

        List<agencyAddCampaignModel> filtered = allCampaigns.stream().filter(camp -> {
            try {
                if (camp.getStartDate() == null || camp.getStartDate().isEmpty()) return false;
                LocalDate start = LocalDate.parse(camp.getStartDate(), formatter);
                return (start.isAfter(cutoffDate) || start.isEqual(cutoffDate)) && !start.isAfter(today);
            } catch (Exception e) { return false; }
        }).collect(Collectors.toList());

        return ResponseEntity.ok(filtered);
    }

    // ==========================================
    // 4. GET SINGLE, DELETE & HELPERS
    // ==========================================
    @GetMapping("/{id}")
    public ResponseEntity<?> getCampaignById(@PathVariable Long id, @RequestHeader("X-Agency-Id") Long agencyId) {
        return campaignService.getByIdSecurely(id, agencyId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).build());
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCampaign(@PathVariable Long id, @RequestHeader("X-Agency-Id") Long agencyId) {
        if (campaignService.deleteSecurely(id, agencyId)) {
            return ResponseEntity.ok(Map.of("message", "Deleted Successfully"));
        }
        return ResponseEntity.status(403).body(Map.of("error", "Unauthorized"));
    }

    private void calculateConversion(agencyAddCampaignModel campaign) {
        if (campaign.getLeads() != null && campaign.getTotalConversions() != null && campaign.getLeads() > 0) {
            double rate = ((double) campaign.getTotalConversions() / campaign.getLeads()) * 100;
            campaign.setConversionRate(Math.round(rate * 100.0) / 100.0);
        } else {
            campaign.setConversionRate(0.0);
        }
    }
}