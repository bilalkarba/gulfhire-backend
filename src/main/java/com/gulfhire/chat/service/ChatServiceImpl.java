package com.gulfhire.chat.service;

import com.gulfhire.application.entity.ApplicationStatus;
import com.gulfhire.application.repository.JobApplicationRepository;
import com.gulfhire.chat.dto.ConversationResponse;
import com.gulfhire.chat.dto.CreateConversationRequest;
import com.gulfhire.chat.dto.MessageResponse;
import com.gulfhire.chat.dto.SendMessageRequest;
import com.gulfhire.chat.entity.Conversation;
import com.gulfhire.chat.entity.Message;
import com.gulfhire.chat.mapper.ChatMapper;
import com.gulfhire.chat.repository.ConversationRepository;
import com.gulfhire.chat.repository.MessageRepository;
import com.gulfhire.common.constants.Role;
import com.gulfhire.job.entity.Job;
import com.gulfhire.job.repository.JobRepository;
import com.gulfhire.user.entity.User;
import com.gulfhire.user.repository.UserRepository;
import com.gulfhire.worker.entity.Worker;
import com.gulfhire.worker.repository.WorkerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final WorkerRepository workerRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final ChatMapper chatMapper;

    @Override
    public ConversationResponse createConversation(User currentUser, CreateConversationRequest request) {
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new EntityNotFoundException("Job not found with id: " + request.getJobId()));

        User workerUser;
        User companyUser;

        if (currentUser.getRole() == Role.WORKER) {
            workerUser = currentUser;
            companyUser = job.getCompany().getUser();
        } else {
            // COMPANY: must own the job and specify the worker (by worker profile id,
            // which is what the applications API exposes to the frontend).
            if (job.getCompany() == null || !job.getCompany().getUser().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("You can only start conversations for your own jobs");
            }
            if (request.getWorkerId() == null) {
                throw new IllegalArgumentException("Worker is required to start a conversation");
            }
            Worker workerProfile = workerRepository.findById(request.getWorkerId())
                    .orElseThrow(() -> new EntityNotFoundException("Worker not found with id: " + request.getWorkerId()));
            workerUser = workerProfile.getUser();
            companyUser = currentUser;
        }

        // Business rule: chat is only unlocked once the application is ACCEPTED.
        UUID workerProfileId = getWorkerProfileId(workerUser);
        boolean accepted = jobApplicationRepository.existsByWorkerIdAndJobIdAndStatus(
                workerProfileId, job.getId(), ApplicationStatus.ACCEPTED);
        if (!accepted) {
            throw new AccessDeniedException(
                    "A conversation can only be started after the application has been accepted");
        }

        Conversation conversation = conversationRepository
                .findByWorkerIdAndCompanyIdAndJobId(workerUser.getId(), companyUser.getId(), job.getId())
                .orElseGet(() -> conversationRepository.save(Conversation.builder()
                        .worker(workerUser)
                        .company(companyUser)
                        .job(job)
                        .build()));

        return toConversationResponse(conversation, currentUser.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> getMyConversations(UUID userId) {
        return conversationRepository
                .findByWorkerIdOrCompanyIdOrderByCreatedAtDesc(userId, userId)
                .stream()
                .map(c -> toConversationResponse(c, userId))
                .sorted(Comparator
                        .comparing(ConversationResponse::getLastMessageAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ConversationResponse::getCreatedAt, Comparator.reverseOrder()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID conversationId, UUID userId) {
        Conversation conversation = getConversationForUser(conversationId, userId);
        return toConversationResponse(conversation, userId);
    }

    @Override
    @Transactional
    public List<MessageResponse> getMessages(UUID conversationId, UUID userId) {
        Conversation conversation = getConversationForUser(conversationId, userId);
        // Fetching the thread doubles as a read receipt: mark incoming as read.
        // NOTE: this transaction must be read-write — PostgreSQL rejects the bulk
        // UPDATE inside a read-only transaction ("cannot execute UPDATE in a
        // read-only transaction"), which 500'd and made conversation history load
        // as empty. Only live STOMP messages ever appeared on screen.
        messageRepository.markAllAsRead(conversation.getId(), userId);
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId()).stream()
                .map(chatMapper::toMessageResponse)
                .toList();
    }

    @Override
    public MessageResponse sendMessage(UUID conversationId, UUID userId, SendMessageRequest request) {
        Conversation conversation = getConversationForUser(conversationId, userId);
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getContent().trim())
                .isRead(false)
                .build();
        message = messageRepository.save(message);
        return chatMapper.toMessageResponse(message);
    }

    @Override
    public MessageResponse markMessageRead(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with id: " + messageId));

        if (!isParticipant(message.getConversation().getId(), userId)) {
            throw new AccessDeniedException("You are not a participant of this conversation");
        }
        if (message.getSender().getId().equals(userId)) {
            // You cannot mark your own messages as read.
            return chatMapper.toMessageResponse(message);
        }
        message.setIsRead(true);
        message = messageRepository.save(message);
        return chatMapper.toMessageResponse(message);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isParticipant(UUID conversationId, UUID userId) {
        return conversationRepository.existsParticipant(conversationId, userId);
    }

    /** Loads a conversation and enforces that the user is one of its participants. */
    private Conversation getConversationForUser(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation not found with id: " + conversationId));
        if (!isParticipant(conversation.getId(), userId)) {
            throw new AccessDeniedException("You are not a participant of this conversation");
        }
        return conversation;
    }

    private ConversationResponse toConversationResponse(Conversation conversation, UUID viewerId) {
        String lastMessage = null;
        java.time.LocalDateTime lastMessageAt = null;
        Message last = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(conversation.getId()).orElse(null);
        if (last != null) {
            lastMessage = last.getContent();
            lastMessageAt = last.getCreatedAt();
        }

        long unreadCount = messageRepository.countUnread(conversation.getId(), viewerId);
        String workerAvatarUrl = workerRepository.findByUserId(conversation.getWorker().getId())
                .map(Worker::getProfilePictureUrl)
                .orElse(null);
        String companyLogoUrl = conversation.getJob().getCompany() != null
                ? conversation.getJob().getCompany().getLogoUrl()
                : null;

        return chatMapper.toConversationResponse(
                conversation, lastMessage, lastMessageAt, unreadCount, workerAvatarUrl, companyLogoUrl);
    }

    private UUID getWorkerProfileId(User workerUser) {
        return workerRepository.findByUserId(workerUser.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Worker profile not found for user: " + workerUser.getEmail()))
                .getId();
    }
}
