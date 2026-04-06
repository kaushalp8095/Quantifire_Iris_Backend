package com.project.agency;

import java.util.List;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.common.models.agencyAddCampaignModel;
import com.project.common.service.ReportService;

@RestController
@RequestMapping("/api/agency/reports")
public class agencyReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/data")
    public ResponseEntity<List<agencyAddCampaignModel>> getReportsData(
            @RequestHeader("X-Agency-Id") Long agencyId) {
        
        List<agencyAddCampaignModel> data = reportService.getAllReportsData(agencyId);
        return ResponseEntity.ok(data);
    }
    
    @PostMapping("/track-download")
    public ResponseEntity<?> trackDownload(@RequestHeader("X-Agency-Id") Long agencyId) {
        try {
            // Service ke naye method ko call karein
            reportService.incrementDownloadCount(agencyId);
            return ResponseEntity.ok(Map.of("message", "Download tracked successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error tracking download: " + e.getMessage());
        }
    }
}