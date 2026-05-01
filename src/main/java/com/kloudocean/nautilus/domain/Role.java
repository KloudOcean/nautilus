package com.kloudocean.nautilus.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL;

    @JsonValue
    public String wire() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static Role from(String wire) {
        return valueOf(wire.toUpperCase());
    }
}
