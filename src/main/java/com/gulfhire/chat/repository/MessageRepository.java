package com.gulfhire.chat.repository;

import com.gulfhire.chat.entity.Message;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    @EntityGraph(attributePaths = "sender")
    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    Optional<Message> findTopByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    @Query("""
            select count(m) from Message m
            where m.conversation.id = :conversationId and m.isRead = false and m.sender.id <> :userId
            """)
    long countUnread(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);

    @Modifying
    @Query("""
            update Message m set m.isRead = true
            where m.conversation.id = :conversationId and m.sender.id <> :userId and m.isRead = false
            """)
    int markAllAsRead(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);
}
