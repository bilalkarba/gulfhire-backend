package com.gulfhire.notification.controller;

import com.gulfhire.notification.dto.NotificationResponse;
import com.gulfhire.notification.service.NotificationService;
import com.gulfhire.user.entity.User;
import com.gulfhire.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /** Paginated list of the current user's notifications (newest first). */
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(notificationService.getMyNotifications(user.getId(), pageable));
    }

    /** All unread notifications of the current user. */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(notificationService.getUnreadNotifications(user.getId()));
    }

    /** Unread count — the number shown on the notification bell badge. */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(user.getId())));
    }

    /** Marks a single notification as read. */
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(notificationService.markAsRead(id, user.getId()));
    }

    /** Marks all notifications of the current user as read. */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(Map.of("updated", notificationService.markAllAsRead(user.getId())));
    }

    /** Deletes a single notification. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        User user = getCurrentUser(userDetails);
        notificationService.deleteNotification(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException(
                        "User not found with email: " + userDetails.getUsername()));
    }
}
