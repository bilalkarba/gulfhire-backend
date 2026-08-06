package com.gulfhire.chat.controller;

import com.gulfhire.chat.dto.ConversationResponse;
import com.gulfhire.chat.dto.CreateConversationRequest;
import com.gulfhire.chat.dto.MessageResponse;
import com.gulfhire.chat.dto.ReadReceiptEvent;
import com.gulfhire.chat.dto.SendMessageRequest;
import com.gulfhire.chat.service.ChatService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

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
                new ReadReceiptEvent(id, user.getId(), LocalDateTime.now()));
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
                    new ReadReceiptEvent(message.getConversationId(), user.getId(), LocalDateTime.now()));
        }
        return ResponseEntity.ok(message);
    }

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + userDetails.getUsername()));
    }
}
