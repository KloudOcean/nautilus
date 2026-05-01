package com.kloudocean.nautilus.routing;

import com.kloudocean.nautilus.domain.ChatRequest;
import com.kloudocean.nautilus.provider.LlmProvider;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Picks the first eligible provider in declaration order.
 *
 * The list passed in is already sorted by priority and filtered to "healthy +
 * supports the requested model" by {@link RouteResolver}, so this strategy's
 * job is simply: take the head.
 *
 * Stateless and trivially thread-safe.
 */
@Component
public class PriorityRoutingStrategy implements RoutingStrategy {

    public static final String NAME = "priority";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public LlmProvider pick(ChatRequest request, List<LlmProvider> eligible) {
        return eligible.isEmpty() ? null : eligible.get(0);
    }
}
