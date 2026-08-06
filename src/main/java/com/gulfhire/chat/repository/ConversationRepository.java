package com.gulfhire.chat.repository;

import com.gulfhire.chat.entity.Conversation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /** One conversation per worker + company + job. */
    Optional<Conversation> findByWorkerIdAndCompanyIdAndJobId(UUID workerId, UUID companyId, UUID jobId);

    @EntityGraph(attributePaths = {"worker", "company", "job", "job.company"})
    List<Conversation> findByWorkerIdOrCompanyIdOrderByCreatedAtDesc(UUID workerId, UUID companyId);

    @Query("""
            select case when count(c) > 0 then true else false end
            from Conversation c
            where c.id = :id and (c.worker.id = :userId or c.company.id = :userId)
            """)
    boolean existsParticipant(@Param("id") UUID id, @Param("userId") UUID userId);
}
