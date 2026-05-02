package com.kloudocean.nautilus.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessage(
        @NotNull Role role,
        @NotNull String content,
        String name) {

    public static ChatMessage system(String content) {
        return new ChatMessage(Role.SYSTEM, content, null);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(Role.USER, content, null);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(Role.ASSISTANT, content, null);
    }
}
