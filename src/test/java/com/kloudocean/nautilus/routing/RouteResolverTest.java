package com.kloudocean.nautilus.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kloudocean.nautilus.config.NautilusProperties;
import com.kloudocean.nautilus.domain.ChatMessage;
import com.kloudocean.nautilus.domain.ChatRequest;
import com.kloudocean.nautilus.domain.ChatResponse;
import com.kloudocean.nautilus.domain.Usage;
import com.kloudocean.nautilus.provider.LlmProvider;
import com.kloudocean.nautilus.provider.ProviderException;
import com.kloudocean.nautilus.provider.ProviderHealth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RouteResolverTest {

    @Test
    void picks_higher_priority_provider() {
        FakeProvider primary = new FakeProvider("primary");
        FakeProvider secondary = new FakeProvider("secondary");
        RouteResolver resolver = resolver(props("primary", "secondary"), List.of(primary, secondary));

        ChatResponse r = resolver.route(request("auto"));

        assertThat(r.provider()).isEqualTo("primary");
        assertThat(primary.calls).isEqualTo(1);
        assertThat(secondary.calls).isEqualTo(0);
    }

    @Test
    void falls_back_to_next_on_retryable_error() {
        FakeProvider flaky = new FakeProvider("primary").throwOnce(
                new ProviderException(ProviderException.Kind.RATE_LIMIT, "primary", "429"));
        FakeProvider backup = new FakeProvider("secondary");
        RouteResolver resolver = resolver(props("primary", "secondary"), List.of(flaky, backup));

        ChatResponse r = resolver.route(request("auto"));

        assertThat(r.provider()).isEqualTo("secondary");
        assertThat(flaky.calls).isEqualTo(1);
        assertThat(backup.calls).isEqualTo(1);
    }

    @Test
    void does_not_fall_back_on_non_retryable_error() {
        FakeProvider primary = new FakeProvider("primary").throwOnce(
                new ProviderException(ProviderException.Kind.AUTH, "primary", "bad key"));
        FakeProvider backup = new FakeProvider("secondary");
        RouteResolver resolver = resolver(props("primary", "secondary"), List.of(primary, backup));

        assertThatThrownBy(() -> resolver.route(request("auto")))
                .isInstanceOf(ProviderException.class)
                .extracting("kind").isEqualTo(ProviderException.Kind.AUTH);
        assertThat(backup.calls).isEqualTo(0);
    }

    @Test
    void rejects_when_no_provider_supports_model() {
        FakeProvider only = new FakeProvider("only").supportsPattern("gpt-*");
        RouteResolver resolver = resolver(props("only"), List.of(only));

        assertThatThrownBy(() -> resolver.route(request("claude-opus")))
                .isInstanceOf(NoEligibleProviderException.class);
    }

    @Test
    void skips_unhealthy_providers() {
        FakeProvider down = new FakeProvider("primary").health(ProviderHealth.DOWN);
        FakeProvider up = new FakeProvider("secondary");
        RouteResolver resolver = resolver(props("primary", "secondary"), List.of(down, up));

        ChatResponse r = resolver.route(request("auto"));

        assertThat(r.provider()).isEqualTo("secondary");
        assertThat(down.calls).isEqualTo(0);
    }

    @Test
    void rejects_unknown_strategy() {
        NautilusProperties p = props("primary");
        p.getRouting().setStrategy("does-not-exist");
        RouteResolver resolver = resolver(p, List.of(new FakeProvider("primary")));

        assertThatThrownBy(() -> resolver.route(request("auto")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does-not-exist");
    }

    // ----- helpers -----

    private static RouteResolver resolver(NautilusProperties props, List<LlmProvider> providers) {
        ProviderRegistry registry = new ProviderRegistry(providers);
        return new RouteResolver(props, registry, List.of(new PriorityRoutingStrategy()));
    }

    private static NautilusProperties props(String... providerNames) {
        NautilusProperties p = new NautilusProperties();
        p.getRouting().setStrategy("priority");
        List<NautilusProperties.Provider> entries = new ArrayList<>();
        for (int i = 0; i < providerNames.length; i++) {
            NautilusProperties.Provider e = new NautilusProperties.Provider();
            e.setName(providerNames[i]);
            e.setPriority(i);
            e.setEnabled(true);
            entries.add(e);
        }
        p.getRouting().setProviders(entries);
        return p;
    }

    private static ChatRequest request(String model) {
        return new ChatRequest(model, List.of(ChatMessage.user("hi")), null, null, null, null, null);
    }

    /** Test double for LlmProvider with controllable health, support, and one-shot failure. */
    static class FakeProvider implements LlmProvider {
        final String name;
        ProviderHealth health = ProviderHealth.UP;
        String supportPattern = "*";
        ProviderException nextThrow;
        int calls;

        FakeProvider(String name) { this.name = name; }

        FakeProvider supportsPattern(String pattern) { this.supportPattern = pattern; return this; }
        FakeProvider health(ProviderHealth h) { this.health = h; return this; }
        FakeProvider throwOnce(ProviderException e) { this.nextThrow = e; return this; }

        @Override public String name() { return name; }
        @Override public ProviderHealth health() { return health; }

        @Override
        public boolean supports(String model) {
            if ("*".equals(supportPattern)) return true;
            String prefix = supportPattern.replace("*", "");
            return model != null && model.startsWith(prefix);
        }

        @Override
        public ChatResponse chat(ChatRequest request) {
            calls++;
            if (nextThrow != null) {
                ProviderException e = nextThrow;
                nextThrow = null;
                throw e;
            }
            return ChatResponse.of(request.model(), name, ChatMessage.assistant("ok"), Usage.zero());
        }
    }
}
