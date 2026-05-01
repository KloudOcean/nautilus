package com.kloudocean.nautilus.provider;

public enum ProviderHealth {
    UP,
    DEGRADED,
    DOWN;

    public boolean isUsable() {
        return this != DOWN;
    }
}
