package com.kloudocean.nautilus.routing;

import com.kloudocean.nautilus.config.NautilusProperties;
import com.kloudocean.nautilus.domain.ChatRequest;
import com.kloudocean.nautilus.domain.ChatResponse;
import com.kloudocean.nautilus.provider.LlmProvider;
import com.kloudocean.nautilus.provider.ProviderException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Orchestrator: filters configured providers down to the eligible set
 * (healthy + supports the model + enabled), hands that set to the active
 * {@link RoutingStrategy}, and invokes the picked provider.
 *
 * Fallback behaviour: if the picked provider throws a retryable
 * {@link ProviderException}, the resolver removes it from the eligible set
 * and retries with the next pick. This keeps the strategy's "first pick"
 * semantics intact while still yielding resilient end-to-end behaviour.
 */
@Component
public class RouteResolver {

    private static final Logger log = LoggerFactory.getLogger(RouteResolver.class);

    private final NautilusProperties properties;
    private final ProviderRegistry registry;
    private final Map<String, RoutingStrategy> strategiesByName;

    public RouteResolver(NautilusProperties properties,
                         ProviderRegistry registry,
                         List<RoutingStrategy> strategies) {
        this.properties = properties;
        this.registry = registry;
        this.strategiesByName = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(RoutingStrategy::name, s -> s));
    }

    public ChatResponse route(ChatRequest request) {
        RoutingStrategy strategy = strategy();
        List<LlmProvider> attempted = new java.util.ArrayList<>();
        List<LlmProvider> remaining = new java.util.ArrayList<>(eligible(request));

        if (remaining.isEmpty()) {
            throw new NoEligibleProviderException(request.model(), enabledNames());
        }

        ProviderException lastError = null;
        while (!remaining.isEmpty()) {
            LlmProvider pick = strategy.pick(request, remaining);
            if (pick == null) break;
            attempted.add(pick);
            remaining.remove(pick);
            try {
                log.debug("routing model={} -> provider={} strategy={}",
                        request.model(), pick.name(), strategy.name());
                return pick.chat(request);
            } catch (ProviderException e) {
                log.warn("provider={} failed kind={} retryable={} attempted={}/{}",
                        pick.name(), e.kind(), e.isRetryable(),
                        attempted.size(), attempted.size() + remaining.size());
                lastError = e;
                if (!e.isRetryable() || remaining.isEmpty()) {
                    throw e;
                }
            }
        }

        // Exhausted all eligible providers with retryable errors.
        throw new NoEligibleProviderException(
                request.model(),
                attempted.stream().map(LlmProvider::name).toList(),
                lastError);
    }

    private RoutingStrategy strategy() {
        String name = properties.getRouting().getStrategy();
        RoutingStrategy s = strategiesByName.get(name);
        if (s == null) {
            throw new IllegalStateException(
                    "No RoutingStrategy bean for nautilus.routing.strategy='" + name
                            + "'. Registered strategies: "
                            + strategiesByName.keySet().stream().sorted().toList());
        }
        return s;
    }

    /**
     * Eligible = configured + enabled + bean-registered + healthy + supports(model),
     * sorted by configured priority ascending.
     */
    private List<LlmProvider> eligible(ChatRequest request) {
        return properties.getRouting().getProviders().stream()
                .filter(NautilusProperties.Provider::isEnabled)
                .sorted(Comparator.comparingInt(NautilusProperties.Provider::getPriority))
                .map(p -> registry.find(p.getName()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .filter(p -> p.health().isUsable())
                .filter(p -> p.supports(request.model()))
                .toList();
    }

    private List<String> enabledNames() {
        return properties.getRouting().getProviders().stream()
                .filter(NautilusProperties.Provider::isEnabled)
                .map(NautilusProperties.Provider::getName)
                .toList();
    }
}
