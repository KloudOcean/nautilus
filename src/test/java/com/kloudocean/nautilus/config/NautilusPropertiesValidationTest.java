package com.kloudocean.nautilus.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kloudocean.nautilus.provider.LlmProvider;
import com.kloudocean.nautilus.provider.MockProvider;
import com.kloudocean.nautilus.routing.ProviderRegistry;
import java.util.List;
import org.junit.jupiter.api.Test;

class NautilusPropertiesValidationTest {

    @Test
    void rejects_unknown_provider_name() {
        NautilusProperties props = props(provider("does-not-exist", 1, true));
        ProviderRegistry registry = new ProviderRegistry(List.<LlmProvider>of(new MockProvider()));

        assertThatThrownBy(() -> new NautilusPropertiesValidator(props, registry).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does-not-exist")
                .hasMessageContaining("Registered providers");
    }

    @Test
    void rejects_duplicate_provider_names() {
        NautilusProperties props = props(provider("mock", 1, true), provider("mock", 2, true));
        ProviderRegistry registry = new ProviderRegistry(List.<LlmProvider>of(new MockProvider()));

        assertThatThrownBy(() -> new NautilusPropertiesValidator(props, registry).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate provider name");
    }

    @Test
    void rejects_negative_weight() {
        NautilusProperties.Provider p = provider("mock", 1, true);
        p.setWeight(-0.5);
        NautilusProperties props = props(p);
        ProviderRegistry registry = new ProviderRegistry(List.<LlmProvider>of(new MockProvider()));

        assertThatThrownBy(() -> new NautilusPropertiesValidator(props, registry).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Negative weight");
    }

    @Test
    void rejects_when_all_providers_disabled() {
        NautilusProperties props = props(provider("mock", 1, false));
        ProviderRegistry registry = new ProviderRegistry(List.<LlmProvider>of(new MockProvider()));

        assertThatThrownBy(() -> new NautilusPropertiesValidator(props, registry).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No enabled providers");
    }

    @Test
    void valid_configuration_passes() {
        NautilusProperties props = props(provider("mock", 1, true));
        ProviderRegistry registry = new ProviderRegistry(List.<LlmProvider>of(new MockProvider()));

        // No exception — clean validation pass.
        new NautilusPropertiesValidator(props, registry).validate();
        assertThat(props.getRouting().getProviders()).hasSize(1);
    }

    // ----- helpers -----

    private static NautilusProperties props(NautilusProperties.Provider... providers) {
        NautilusProperties p = new NautilusProperties();
        p.getRouting().setStrategy("priority");
        p.getRouting().setProviders(new java.util.ArrayList<>(java.util.Arrays.asList(providers)));
        return p;
    }

    private static NautilusProperties.Provider provider(String name, int priority, boolean enabled) {
        NautilusProperties.Provider p = new NautilusProperties.Provider();
        p.setName(name);
        p.setPriority(priority);
        p.setEnabled(enabled);
        return p;
    }
}
