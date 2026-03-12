package com.project.agency; 

import com.project.common.service.agencyNotificationService;
import com.project.common.dto.agencyNotificationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/top-notifications")
public class agencyTopNotificationController {

    @Autowired
    private agencyNotificationService notifService;

    @GetMapping("/get")
    public ResponseEntity<?> getNotifications(@RequestParam String email) {
        long unreadCount = notifService.getUnreadCount(email);
        List<agencyNotificationDTO> notifs = notifService.getNotificationsForAgency(email);
        
        return ResponseEntity.ok(Map.of(
                "unreadCount", unreadCount,
                "notifications", notifs
        ));
    }

    @PostMapping("/mark-read")
    public ResponseEntity<?> markAllAsRead(@RequestParam String email) {
        notifService.markAllAsRead(email);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    @GetMapping("/create-test")
    public ResponseEntity<?> createTestNotif(
            @RequestParam String email, 
            @RequestParam String type, 
            @RequestParam String title, 
            @RequestParam String msg) {
        
        notifService.createNotification(email, type, title, msg);
        return ResponseEntity.ok(Map.of("message", "Test Notification Generated in Supabase & MongoDB!"));
    }
}