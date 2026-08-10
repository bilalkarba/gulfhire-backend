package com.gulfhire.notification.service;

import com.gulfhire.notification.dto.NotificationResponse;
import com.gulfhire.notification.entity.Notification;
import com.gulfhire.notification.entity.NotificationType;
import com.gulfhire.notification.mapper.NotificationMapper;
import com.gulfhire.notification.repository.NotificationRepository;
import com.gulfhire.user.entity.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private static final String NOTIFICATION_TOPIC = "/topic/notifications/";

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Persists a notification for the recipient and pushes it instantly over
     * WebSocket to {@code /topic/notifications/{userId}}.
     */
    public NotificationResponse createNotification(User recipient, String title, String message, NotificationType type) {
        Notification notification = Notification.builder()
                .user(recipient)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .build();
        notification = notificationRepository.save(notification);

        NotificationResponse response = notificationMapper.toResponse(notification);
        messagingTemplate.convertAndSend(NOTIFICATION_TOPIC + recipient.getId(), response);
        return response;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserId(userId, defaultSort(pageable))
                .map(notificationMapper::toResponse);
    }

    /** Newest notifications first unless the caller explicitly requested a sort. */
    private Pageable defaultSort(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /** Marks a single notification as read (ownership enforced). */
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = getOwnedNotification(notificationId, userId);
        notification.setIsRead(true);
        notification = notificationRepository.save(notification);
        return notificationMapper.toResponse(notification);
    }

    /** Marks every unread notification of the user as read. Returns the number updated. */
    public int markAllAsRead(UUID userId) {
        return notificationRepository.markAllAsRead(userId);
    }

    /** Deletes a notification (ownership enforced). */
    public void deleteNotification(UUID notificationId, UUID userId) {
        Notification notification = getOwnedNotification(notificationId, userId);
        notificationRepository.delete(notification);
    }

    private Notification getOwnedNotification(UUID notificationId, UUID userId) {
        return notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Notification not found with id: " + notificationId));
    }
}
