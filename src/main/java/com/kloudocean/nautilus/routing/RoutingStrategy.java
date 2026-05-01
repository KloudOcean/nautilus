package com.kloudocean.nautilus.routing;

import com.kloudocean.nautilus.domain.ChatRequest;
import com.kloudocean.nautilus.provider.LlmProvider;
import java.util.List;

/**
 * SPI for routing strategies (priority, round-robin, random, cost-aware, latency-aware).
 *
 * Each strategy is a Spring bean. The strategy selected at runtime is determined by
 * the {@code nautilus.routing.strategy} property.
 *
 * Implementations must be thread-safe. Strategies that hold per-instance state
 * (e.g. round-robin cursor) should document multi-instance behaviour in their
 * Javadoc — see docs/guides/routing-strategies.md.
 */
public interface RoutingStrategy {

    /**
     * Stable identifier matched against {@code nautilus.routing.strategy}.
     */
    String name();

    /**
     * Pick a provider for the given request from the eligible set.
     * Returns null if no eligible provider exists; the caller decides
     * whether to surface a 503 or attempt an alternative path.
     */
    LlmProvider pick(ChatRequest request, List<LlmProvider> eligible);
}
