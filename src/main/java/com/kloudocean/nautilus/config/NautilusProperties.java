package com.kloudocean.nautilus.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Root configuration for Nautilus, bound from {@code nautilus.*}.
 * Validated at startup — see {@link NautilusPropertiesValidator}.
 */
@Validated
@ConfigurationProperties(prefix = "nautilus")
public class NautilusProperties {

    @Valid
    @NotNull
    private Routing routing = new Routing();

    public Routing getRouting() {
        return routing;
    }

    public void setRouting(Routing routing) {
        this.routing = routing;
    }

    public static class Routing {

        /** Strategy name — must match a registered {@code RoutingStrategy} bean. */
        @NotBlank
        @Pattern(regexp = "[a-z][a-z0-9-]*",
                message = "must be a lowercase identifier (e.g. 'priority', 'round-robin')")
        private String strategy = "priority";

        /** Provider entries in priority/declaration order. */
        @NotEmpty(message = "at least one provider must be configured under nautilus.routing.providers")
        @Valid
        private List<Provider> providers = new ArrayList<>();

        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }

        public List<Provider> getProviders() { return providers; }
        public void setProviders(List<Provider> providers) { this.providers = providers; }
    }

    public static class Provider {

        /** Provider name — must match the {@code name()} of a registered {@code LlmProvider} bean. */
        @NotBlank
        private String name;

        /** Lower number = higher priority. Used by the priority strategy. */
        private int priority = Integer.MAX_VALUE;

        /** Optional weight for weighted strategies (random). Negative values rejected. */
        private double weight = 1.0;

        /** Whether this entry is active. Disabled entries are skipped during routing. */
        private boolean enabled = true;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }

        public double getWeight() { return weight; }
        public void setWeight(double weight) { this.weight = weight; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
