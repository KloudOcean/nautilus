package com.kloudocean.nautilus.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Choice(
        int index,
        ChatMessage message,
        @JsonProperty("finish_reason") String finishReason) {

    public static Choice of(int index, ChatMessage message) {
        return new Choice(index, message, "stop");
    }
}
