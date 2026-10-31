package com.brightloop.sms.controller;

import com.brightloop.sms.dao.NotificationDao;
import com.brightloop.sms.dao.UserDao;
import com.brightloop.sms.model.Notification;
import com.brightloop.sms.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "In-app system notifications and renewal/payment alerts")
public class NotificationController {

    private final NotificationDao notificationDao;
    private final UserDao userDao;

    public NotificationController(NotificationDao notificationDao, UserDao userDao) {
        this.notificationDao = notificationDao;
        this.userDao = userDao;
    }

    @GetMapping
    @Operation(summary = "Get user notifications")
    public ResponseEntity<List<Notification>> getNotifications(Authentication authentication) {
        User user = getUser(authentication);
        if (user == null) {
            return ResponseEntity.ok(List.of());
        }
        if ("admin".equalsIgnoreCase(user.getRole()) || "staff".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.ok(notificationDao.findAll());
        }
        return ResponseEntity.ok(notificationDao.findByUserId(user.getId()));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark single notification as read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        notificationDao.markAsRead(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mark-all-read")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Void> markAllRead(Authentication authentication) {
        User user = getUser(authentication);
        if (user != null) {
            notificationDao.markAllAsRead(user.getId());
        }
        return ResponseEntity.noContent().build();
    }

    private User getUser(Authentication auth) {
        if (auth == null) return null;
        return userDao.findByEmail(auth.getName()).orElse(null);
    }
}
