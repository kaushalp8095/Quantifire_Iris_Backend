package com.project.agency;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.models.agencyAddCampaignModel;
import com.project.common.service.CampaignService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

@RestController
@RequestMapping("/api/agency/campaigns")
public class agencyCampaignsController {

	@Autowired
    private CampaignService campaignService;

    private static final String UPLOAD_DIR = "uploads/";

    @PostMapping("/add")
    public ResponseEntity<?> addCampaign(
            @RequestParam("campaignData") String campaignJson, 
            @RequestParam(value = "image", required = false) MultipartFile file 
    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            agencyAddCampaignModel campaign = mapper.readValue(campaignJson, agencyAddCampaignModel.class);

            // --- DEBUG & FIX: Client ID Check ---
            System.out.println("Checking Client ID for New Campaign: " + campaign.getClientId());
            if (campaign.getClientId() == null) {
                return ResponseEntity.status(400).body("Error: Client ID is missing in the request.");
            }
            // ------------------------------------

            // Baaki conversion logic...
            if (campaign.getLeads() != null && campaign.getTotalConversions() != null && campaign.getLeads() > 0) {
                double rate = ((double) campaign.getTotalConversions() / campaign.getLeads()) * 100;
                campaign.setConversionRate(Math.round(rate * 100.0) / 100.0);
            }

            // Image Handling logic...
            if (file != null && !file.isEmpty()) {
                File directory = new File(UPLOAD_DIR);
                if (!directory.exists()) directory.mkdirs();
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Files.write(Paths.get(UPLOAD_DIR + fileName), file.getBytes());
                campaign.setCreativePath(fileName);
            } 
            
            // Location formatting...
            String location = campaign.getTargetLocation().trim();
            if (!location.isEmpty()) {
                location = location.substring(0, 1).toUpperCase() + location.substring(1).toLowerCase();
                campaign.setTargetLocation(location);
            }

            campaignService.saveOrUpdate(campaign);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Campaign Added Successfully!"));

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllCampaigns(@RequestParam(defaultValue = "all") String filter, @RequestHeader("X-Agency-Id") Long agencyId)  {
    	List<agencyAddCampaignModel> allCampaigns = campaignService.getCampaignsByAgency(agencyId);
        List<agencyAddCampaignModel> filteredList = new ArrayList<>();
        

        LocalDate today = LocalDate.now();
        LocalDate cutoffDate = null;

        if (filter.equals("7days")) {
            cutoffDate = today.minusDays(7);
        } else if (filter.equals("1month")) {
            cutoffDate = today.minusMonths(1);
        } else if (filter.equals("3months")) {
            cutoffDate = today.minusMonths(3);
        }

        if (filter.equals("all")) {
            
            filteredList = allCampaigns;
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM, yyyy", Locale.ENGLISH);

            for (agencyAddCampaignModel camp : allCampaigns) {
                try {
                    if (camp.getStartDate() != null && !camp.getStartDate().isEmpty()) {
                        
                        LocalDate startDate = LocalDate.parse(camp.getStartDate(), formatter);
                        
                       
                        boolean isAfterCutoff = startDate.isAfter(cutoffDate) || startDate.isEqual(cutoffDate);
                        
                        boolean isNotFuture = startDate.isBefore(today) || startDate.isEqual(today);

                        if (isAfterCutoff && isNotFuture) {
                            filteredList.add(camp);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Date Error: " + e.getMessage());
                }
            }
        }

        return ResponseEntity.ok(filteredList);
    }
    
   //  GET SINGLE CAMPAIGN (Edit Page)
    @GetMapping("/{id}")
    public ResponseEntity<?> getCampaignById(
            @PathVariable Long id, 
            @RequestHeader(value = "X-Agency-Id", required = false) Long agencyId) {
        
        // Check agar header miss ho gaya ho
        if (agencyId == null) {
            return ResponseEntity.status(400).body("Error: X-Agency-Id header is missing");
        }

        // Service se secure data fetch karo
        Optional<agencyAddCampaignModel> campaignOpt = campaignService.getByIdSecurely(id, agencyId);

        if (campaignOpt.isPresent()) {
            return ResponseEntity.ok(campaignOpt.get());
        } else {
            // Agar data nahi mila ya agencyId galat hai
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Campaign not found or Access Denied");
            return ResponseEntity.status(403).body(errorResponse);
        }
    }
    
    // UPDATE CAMPAIGN 
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateCampaign(
            @PathVariable Long id,
            @RequestParam("campaignData") String campaignJson,
            @RequestParam(value = "image", required = false) MultipartFile file,
            @RequestHeader("X-Agency-Id") Long agencyId // Header yahan bhi zaroori hai security ke liye
    ) {
        try {
            // Sirf wahi campaign update ho jo is agency ki ho
            Optional<agencyAddCampaignModel> existingOpt = campaignService.getByIdSecurely(id, agencyId);
            
            if (existingOpt.isEmpty()) {
                return ResponseEntity.status(403).body("Unauthorized or Campaign not found");
            }

            agencyAddCampaignModel existingCamp = existingOpt.get();
            ObjectMapper mapper = new ObjectMapper();
            agencyAddCampaignModel newData = mapper.readValue(campaignJson, agencyAddCampaignModel.class);

            // Fields Update (Direct object update)
            existingCamp.setCampaignName(newData.getCampaignName());
            existingCamp.setClientId(newData.getClientId());
            existingCamp.setClientName(newData.getClientName());
            existingCamp.setTargetLocation(newData.getTargetLocation());
            existingCamp.setStartDate(newData.getStartDate());
            existingCamp.setEndDate(newData.getEndDate());
            existingCamp.setBudget(newData.getBudget());
            existingCamp.setLeads(newData.getLeads());
            existingCamp.setTotalConversions(newData.getTotalConversions());
            existingCamp.setGender(newData.getGender());
            existingCamp.setAgeRange(newData.getAgeRange());
            existingCamp.setStatus(newData.getStatus());
            existingCamp.setGeoData(newData.getGeoData());
            existingCamp.setAdPlatform(newData.getAdPlatform());

            // Conversion Rate Logic
            if (existingCamp.getLeads() != null && existingCamp.getTotalConversions() != null && existingCamp.getLeads() > 0) {
                double rate = ((double) existingCamp.getTotalConversions() / existingCamp.getLeads()) * 100;
                existingCamp.setConversionRate(Math.round(rate * 100.0) / 100.0);
            }

            // Image Update Logic
            if (file != null && !file.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Files.write(Paths.get(UPLOAD_DIR + fileName), file.getBytes());
                existingCamp.setCreativePath(fileName);
            }

            // Service call to sync both DBs
            campaignService.saveOrUpdate(existingCamp);
            
            return ResponseEntity.ok(Map.of("message", "Updated Successfully", "status", "success"));

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
    
  // DELETE API
 // DELETE API Updated
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCampaign(
            @PathVariable Long id, 
            @RequestHeader("X-Agency-Id") Long agencyId) { // Header se ID lo
        try {
            boolean isDeleted = campaignService.deleteSecurely(id, agencyId);
            
            if (isDeleted) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Campaign Deleted Successfully");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(403).body("Unauthorized: You don't have permission to delete this campaign.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting campaign: " + e.getMessage());
        }
    }
    
    @GetMapping("/client/{clientName}")
    public ResponseEntity<List<agencyAddCampaignModel>> getCampaignsByClient(@PathVariable String clientName) {
        return ResponseEntity.ok(campaignService.getByClientName(clientName));
    }
}