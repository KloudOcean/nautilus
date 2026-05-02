package com.kloudocean.nautilus.provider;

public class ProviderException extends RuntimeException {

    public enum Kind {
        RATE_LIMIT,
        TIMEOUT,
        AUTH,
        UPSTREAM_ERROR,
        BAD_REQUEST,
        UNAVAILABLE
    }

    private final Kind kind;
    private final String providerName;

    public ProviderException(Kind kind, String providerName, String message) {
        this(kind, providerName, message, null);
    }

    public ProviderException(Kind kind, String providerName, String message, Throwable cause) {
        super(formatMessage(kind, providerName, message), cause);
        this.kind = kind;
        this.providerName = providerName;
    }

    public Kind kind() {
        return kind;
    }

    public String providerName() {
        return providerName;
    }

    public boolean isRetryable() {
        return switch (kind) {
            case RATE_LIMIT, TIMEOUT, UPSTREAM_ERROR, UNAVAILABLE -> true;
            case AUTH, BAD_REQUEST -> false;
        };
    }

    private static String formatMessage(Kind kind, String provider, String message) {
        return "[" + provider + "/" + kind + "] " + message;
    }
}
