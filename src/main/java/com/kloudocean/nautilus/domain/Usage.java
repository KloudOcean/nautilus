package com.kloudocean.nautilus.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Usage(
        @JsonProperty("prompt_tokens") int promptTokens,
        @JsonProperty("completion_tokens") int completionTokens,
        @JsonProperty("total_tokens") int totalTokens) {

    public static Usage of(int prompt, int completion) {
        return new Usage(prompt, completion, prompt + completion);
    }

    public static Usage zero() {
        return new Usage(0, 0, 0);
    }
}
