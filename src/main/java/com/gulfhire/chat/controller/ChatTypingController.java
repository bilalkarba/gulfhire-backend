package com.gulfhire.chat.controller;

import com.gulfhire.chat.dto.TypingEvent;
import com.gulfhire.chat.dto.TypingPayload;
import com.gulfhire.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatTypingController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @MessageMapping("/chat/{conversationId}/typing")
    public void typing(@DestinationVariable UUID conversationId,
                       @Payload TypingPayload payload,
                       Principal principal) {
        UUID userId = UUID.fromString(principal.getName());
        if (!chatService.isParticipant(conversationId, userId)) {
            throw new AccessDeniedException("You are not a participant of this conversation");
        }
        messagingTemplate.convertAndSend("/topic/conversation/" + conversationId + "/typing",
                new TypingEvent(conversationId, userId, payload.typing()));
    }
}
