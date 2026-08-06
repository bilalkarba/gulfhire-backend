package com.gulfhire.chat.service;

import com.gulfhire.chat.dto.PresenceEvent;
import com.gulfhire.chat.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ChatPresenceService {

    private final Set<UUID> onlineUsers = ConcurrentHashMap.newKeySet();
    private final ConversationRepository conversationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public void userConnected(UUID userId) {
        if (onlineUsers.add(userId)) {
            broadcastPresence(userId);
        }
    }

    public void userDisconnected(UUID userId) {
        if (onlineUsers.remove(userId)) {
            broadcastPresence(userId);
        }
    }

    /** Informs every conversation of the changed user about both participants' online state. */
    private void broadcastPresence(UUID changedUserId) {
        conversationRepository.findByWorkerIdOrCompanyIdOrderByCreatedAtDesc(changedUserId, changedUserId)
                .forEach(conversation -> {
                    List<UUID> participantIds = List.of(
                            conversation.getWorker().getId(),
                            conversation.getCompany().getId());
                    List<UUID> online = participantIds.stream().filter(onlineUsers::contains).toList();
                    messagingTemplate.convertAndSend("/topic/presence/" + conversation.getId(),
                            new PresenceEvent(conversation.getId(), online));
                });
    }
}
