package com.gulfhire.chat.controller;

import com.gulfhire.chat.dto.ConversationResponse;
import com.gulfhire.chat.dto.CreateConversationRequest;
import com.gulfhire.chat.dto.MessageResponse;
import com.gulfhire.chat.dto.MessageUpdateEvent;
import com.gulfhire.chat.dto.PresenceStatusResponse;
import com.gulfhire.chat.dto.ReadReceiptEvent;
import com.gulfhire.chat.dto.SendMessageRequest;
import com.gulfhire.chat.dto.UpdateMessageRequest;
import com.gulfhire.chat.service.ChatService;
import com.gulfhire.chat.service.PresenceService;
import com.gulfhire.storage.dto.UploadResponse;
import com.gulfhire.storage.service.CloudinaryService;
import com.gulfhire.storage.util.FileTypeUtils;
import com.gulfhire.user.entity.User;
import com.gulfhire.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private static final String CHAT_ATTACHMENTS_FOLDER = "chat-attachments";
    private static final long MAX_CHAT_ATTACHMENT_BYTES = 20L * 1024 * 1024;

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final CloudinaryService cloudinaryService;
    private final PresenceService presenceService;

    @PostMapping("/conversations")
    @PreAuthorize("hasAnyRole('WORKER', 'COMPANY')")
    public ResponseEntity<ConversationResponse> createConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateConversationRequest request) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.createConversation(user, request));
    }

    @GetMapping("/conversations/my")
    @PreAuthorize("hasAnyRole('WORKER', 'COMPANY')")
    public ResponseEntity<List<ConversationResponse>> getMyConversations(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(chatService.getMyConversations(user.getId()));
    }

    @GetMapping("/conversations/{id}")
    @PreAuthorize("hasAnyRole('WORKER', 'COMPANY')")
    public ResponseEntity<ConversationResponse> getConversation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        User user = getCurrentUser(userDetails);
        return ResponseEntity.ok(chatService.getConversation(id, user.getId()));
    }

    @GetMapping("/conversations/{id}/messages")
    @PreAuthorize("hasAnyRole('WORKER', 'COMPANY')")
    public ResponseEntity<List<MessageResponse>> getMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        User user = getCurrentUser(userDetails);
        List<MessageResponse> messages = chatService.getMessages(id, user.getId());
        // Notify the other participant that these messages were read.
        messagingTemplate.convertAndSend("/topic/conversation/" + id,
                new ReadReceiptEvent(id, user.getId(), Instant.now()));
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/conversations/{id}/messages")
    @PreAuthorize("hasAnyRole('WORKER', 'COMPANY')")
    public ResponseEntity<MessageResponse> sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequest request) {
        User user = getCurrentUser(userDetails);
        MessageResponse saved = chatService.sendMessage(id, user.getId(), request);
        // Persisted and broadcast instantly to everyone subscribed to this conversation.
        messagingTemplate.convertAndSend("/topic/conversation/" + id, saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Uploads a chat attachment (PDF, DOCX, or image) to Cloudinary. The
     * returned URL is passed back in {@link SendMessageRequest#getAttachmentUrl()}
     * when sending the message.
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('WORKER', 'COMPANY')")
    public ResponseEntity<UploadResponse> uploadAttachment(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        validateAttachment(file);
        return ResponseEntity.ok(cloudinaryService.uploadFile(file, CHAT_ATTACHMENTS_FOLDER));
    }

    /**
     * Current presence status (online + last seen) of a user. Used by the chat
     * UI to render the peer's status before/between WebSocket events.
     */
    @GetMapping("/users/{id}/presence")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PresenceStatusResponse> getUserPresence(@PathVariable UUID id) {
        userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
        boolean online = presenceService.isOnline(id);
        return ResponseEntity.ok(
                new PresenceStatusResponse(id, online, online ? null : presenceService.getLastSeen(id)));
    }

    /** Edits a message (sender only). Broadcasts a MessageUpdateEvent to the conversation. */
    @PutMapping("/messages/{id}")
    @PreAuthorize("hasAnyRole('WORKER', 'COMPANY')")
    public ResponseEntity<MessageResponse> editMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMessageRequest request) {
        User user = getCurrentUser(userDetails);
        MessageResponse updated = chatService.editMessage(id, user.getId(), request);
        messagingTemplate.convertAndSend("/topic/conversation/" + updated.getConversationId(),
                new MessageUpdateEvent(updated.getConversationId(), updated.getId(),
                        MessageUpdateEvent.Type.EDITED, updated));
        return ResponseEntity.ok(updated);
    }

    /** Soft-deletes a message (sender only). Broadcasts a MessageUpdateEvent to the conversation. */
    @DeleteMapping("/messages/{id}")
    @PreAuthorize("hasAnyRole('WORKER', 'COMPANY')")
    public ResponseEntity<MessageResponse> deleteMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        User user = getCurrentUser(userDetails);
        MessageResponse deleted = chatService.deleteMessage(id, user.getId());
        messagingTemplate.convertAndSend("/topic/conversation/" + deleted.getConversationId(),
                new MessageUpdateEvent(deleted.getConversationId(), deleted.getId(),
                        MessageUpdateEvent.Type.DELETED, deleted));
        return ResponseEntity.ok(deleted);
    }

    private void validateAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required and must not be empty");
        }
        String extension = FileTypeUtils.getExtension(file.getOriginalFilename());
        if (extension == null || !FileTypeUtils.CHAT_ATTACHMENT_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "Invalid file type. Allowed types: " + String.join(", ", FileTypeUtils.CHAT_ATTACHMENT_EXTENSIONS));
        }
        if (file.getSize() > MAX_CHAT_ATTACHMENT_BYTES) {
            throw new IllegalArgumentException("File must not exceed 20 MB");
        }
    }

    @PutMapping("/messages/{id}/read")
    @PreAuthorize("hasAnyRole('WORKER', 'COMPANY')")
    public ResponseEntity<MessageResponse> markRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        User user = getCurrentUser(userDetails);
        MessageResponse message = chatService.markMessageRead(id, user.getId());
        // Only broadcast a receipt for messages sent by someone else — marking
        // your own message read is a no-op and would be noise to the peer.
        if (!message.getSenderId().equals(user.getId())) {
            messagingTemplate.convertAndSend("/topic/conversation/" + message.getConversationId(),
                    new ReadReceiptEvent(message.getConversationId(), user.getId(), Instant.now()));
        }
        return ResponseEntity.ok(message);
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + userDetails.getUsername()));
    }
}
