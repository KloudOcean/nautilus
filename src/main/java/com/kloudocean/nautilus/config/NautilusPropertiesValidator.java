package com.kloudocean.nautilus.config;

import com.kloudocean.nautilus.routing.ProviderRegistry;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Cross-field validation that Bean Validation cannot express:
 * - every configured provider name must resolve to a registered {@code LlmProvider} bean
 * - provider names must be unique within the routing config
 * - weights must be non-negative
 *
 * Runs at {@code ApplicationReadyEvent} so all beans are present, and throws
 * a precise {@link IllegalStateException} that fails the boot if anything is off.
 *
 * Closes the user-facing portion of issue #5: clear startup errors when
 * application.yml is misconfigured.
 */
@Component
public class NautilusPropertiesValidator {

    private final NautilusProperties properties;
    private final ProviderRegistry registry;

    public NautilusPropertiesValidator(NautilusProperties properties, ProviderRegistry registry) {
        this.properties = properties;
        this.registry = registry;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        List<NautilusProperties.Provider> providers = properties.getRouting().getProviders();

        Set<String> seen = new LinkedHashSet<>();
        for (NautilusProperties.Provider p : providers) {
            if (!seen.add(p.getName())) {
                throw new IllegalStateException(
                        "Duplicate provider name in nautilus.routing.providers: '" + p.getName() + "'. "
                                + "Each provider entry must have a unique name.");
            }
            if (p.getWeight() < 0) {
                throw new IllegalStateException(
                        "Negative weight for provider '" + p.getName() + "' in nautilus.routing.providers. "
                                + "Weights must be >= 0.");
            }
            if (registry.find(p.getName()).isEmpty()) {
                String registered = registry.all().keySet().stream().sorted().collect(Collectors.joining(", "));
                throw new IllegalStateException(
                        "Provider '" + p.getName() + "' configured under nautilus.routing.providers but no "
                                + "LlmProvider bean is registered for it. Registered providers: ["
                                + (registered.isEmpty() ? "(none)" : registered) + "]. "
                                + "Either remove the entry, or enable/implement the matching provider.");
            }
        }

        long enabledCount = providers.stream().filter(NautilusProperties.Provider::isEnabled).count();
        if (enabledCount == 0) {
            throw new IllegalStateException(
                    "No enabled providers in nautilus.routing.providers. "
                            + "At least one provider must have 'enabled: true'.");
        }
    }
}
