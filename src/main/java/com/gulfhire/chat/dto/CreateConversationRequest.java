package com.gulfhire.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateConversationRequest {

    @NotNull(message = "Job id is required")
    private UUID jobId;

    /** Required when a COMPANY initiates the conversation with a specific worker. */
    private UUID workerId;
}
