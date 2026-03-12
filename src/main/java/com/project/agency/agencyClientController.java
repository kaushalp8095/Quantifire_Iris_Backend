package com.project.agency;

import com.project.common.models.agencyAddClientModel;
import com.project.common.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agency/clients")
public class agencyClientController {

    @Autowired
    private ClientService clientService;

    // --- Add Client ---
    @PostMapping("/add")
    public ResponseEntity<?> addClient(@RequestBody agencyAddClientModel client) {
        try {
            clientService.saveClient(client);
            return ResponseEntity.ok("Client Added Successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // --- Get All Clients (Already has agencyId) ---
    @GetMapping("/all")
    public List<Map<String, Object>> getAllClients(@RequestParam Long agencyId) {
        return clientService.getClientsWithPerformanceMetricsByAgency(agencyId);
    }

    // --- Delete Client (Added Security) ---
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteClient(@PathVariable Long id, @RequestHeader("X-Agency-Id") Long agencyId) {
        boolean isDeleted = clientService.deleteByIdAndAgency(id, agencyId);
        if (isDeleted) {
            return ResponseEntity.ok("Deleted Successfully");
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized or Client Not Found");
        }
    }
    
    // --- Get Single Client ---
    @GetMapping("/{id}")
    public ResponseEntity<agencyAddClientModel> getClientById(@PathVariable Long id) {
        return clientService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Update Client (Added Security) ---
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateClient(
            @PathVariable Long id, 
            @RequestBody agencyAddClientModel updatedData,
            @RequestHeader("X-Agency-Id") Long agencyId) {
        
        return clientService.findById(id).map(client -> {
            // Verify ownership
            if (!client.getAgencyId().equals(agencyId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized update attempt");
            }
            
            client.setAgencyName(updatedData.getAgencyName());
            client.setClientName(updatedData.getClientName());
            client.setEmail(updatedData.getEmail());
            client.setContactNumber(updatedData.getContactNumber());
            
            clientService.saveClient(client);
            return ResponseEntity.ok("Client Updated Successfully!");
        }).orElse(ResponseEntity.status(404).body("Client Not Found"));
    }
}