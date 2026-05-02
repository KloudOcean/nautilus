package com.kloudocean.nautilus.provider;

import com.kloudocean.nautilus.domain.ChatRequest;
import com.kloudocean.nautilus.domain.ChatResponse;

/**
 * SPI for LLM providers. Each provider is a Spring bean discovered by name
 * via {@link com.kloudocean.nautilus.routing.ProviderRegistry}.
 *
 * Implementations must be thread-safe — a single bean serves concurrent requests.
 */
public interface LlmProvider {

    /**
     * Stable identifier used in routing config (e.g. "claude", "openai", "ollama").
     * Lowercase, no whitespace, must match the key under {@code nautilus.routing.providers[]}.
     */
    String name();

    /**
     * Whether this provider can handle the requested model id.
     * Routing skips providers that return {@code false}.
     */
    boolean supports(String model);

    /**
     * Synchronous chat completion. Implementations throw {@link ProviderException}
     * for typed failures so the orchestrator can decide whether to retry / fallback.
     */
    ChatResponse chat(ChatRequest request);

    /**
     * Cheap, fast health check. Default returns UP — providers that need an
     * actual probe (auth, latency budget) override this.
     */
    default ProviderHealth health() {
        return ProviderHealth.UP;
    }
}
