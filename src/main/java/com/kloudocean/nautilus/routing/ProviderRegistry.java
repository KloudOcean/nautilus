package com.kloudocean.nautilus.routing;

import com.kloudocean.nautilus.provider.LlmProvider;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Indexes every {@link LlmProvider} bean by its {@link LlmProvider#name()}.
 * Built once at startup; lookups are O(1).
 */
@Component
public class ProviderRegistry {

    private final Map<String, LlmProvider> byName;

    public ProviderRegistry(List<LlmProvider> providers) {
        this.byName = providers.stream()
                .collect(Collectors.toUnmodifiableMap(LlmProvider::name, p -> p));
    }

    public Optional<LlmProvider> find(String name) {
        return Optional.ofNullable(byName.get(name));
    }

    public LlmProvider require(String name) {
        return find(name).orElseThrow(() ->
                new IllegalStateException("No LlmProvider bean registered with name: " + name));
    }

    public Map<String, LlmProvider> all() {
        return byName;
    }
}
