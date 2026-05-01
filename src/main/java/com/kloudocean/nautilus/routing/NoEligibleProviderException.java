package com.kloudocean.nautilus.routing;

import java.util.List;

/**
 * Raised when no provider can serve the request — either nothing matched the model
 * up front, or every eligible provider was tried and failed with retryable errors.
 *
 * The exception preserves which providers were configured/attempted so the error
 * advice can produce an actionable HTTP 503 message.
 */
public class NoEligibleProviderException extends RuntimeException {

    private final String model;
    private final List<String> consideredOrAttempted;

    public NoEligibleProviderException(String model, List<String> considered) {
        this(model, considered, null);
    }

    public NoEligibleProviderException(String model, List<String> consideredOrAttempted, Throwable cause) {
        super(buildMessage(model, consideredOrAttempted, cause), cause);
        this.model = model;
        this.consideredOrAttempted = List.copyOf(consideredOrAttempted);
    }

    public String model() {
        return model;
    }

    public List<String> consideredOrAttempted() {
        return consideredOrAttempted;
    }

    private static String buildMessage(String model, List<String> providers, Throwable cause) {
        StringBuilder sb = new StringBuilder("No eligible provider for model='")
                .append(model).append("'.");
        if (cause != null) {
            sb.append(" All ").append(providers.size())
                    .append(" attempted providers failed: ").append(providers).append('.');
        } else {
            sb.append(" Considered: ").append(providers).append('.');
        }
        return sb.toString();
    }
}
