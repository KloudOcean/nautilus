package com.kloudocean.nautilus.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * OpenAI-compatible chat completion request. The wire format is intentionally
 * identical to OpenAI's so existing clients work without code changes.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatRequest(
        @NotNull String model,
        @NotEmpty @Valid List<ChatMessage> messages,
        Double temperature,
        @JsonProperty("max_tokens") Integer maxTokens,
        @JsonProperty("top_p") Double topP,
        Boolean stream,
        String user) {

    public ChatRequest {
        if (messages != null) {
            messages = List.copyOf(messages);
        }
    }

    public boolean isStreaming() {
        return Boolean.TRUE.equals(stream);
    }
}
