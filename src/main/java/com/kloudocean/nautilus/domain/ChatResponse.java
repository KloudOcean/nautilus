package com.kloudocean.nautilus.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatResponse(
        String id,
        String object,
        long created,
        String model,
        String provider,
        List<Choice> choices,
        Usage usage) {

    public static ChatResponse of(String model, String provider, ChatMessage message, Usage usage) {
        return new ChatResponse(
                "chatcmpl-" + UUID.randomUUID(),
                "chat.completion",
                Instant.now().getEpochSecond(),
                model,
                provider,
                List.of(Choice.of(0, message)),
                usage);
    }
}
