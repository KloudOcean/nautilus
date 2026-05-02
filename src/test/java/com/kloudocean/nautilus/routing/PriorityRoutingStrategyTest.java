package com.kloudocean.nautilus.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.kloudocean.nautilus.domain.ChatMessage;
import com.kloudocean.nautilus.domain.ChatRequest;
import com.kloudocean.nautilus.domain.ChatResponse;
import com.kloudocean.nautilus.provider.LlmProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

class PriorityRoutingStrategyTest {

    private final PriorityRoutingStrategy strategy = new PriorityRoutingStrategy();

    @Test
    void picks_first_eligible_provider() {
        LlmProvider primary = stub("primary");
        LlmProvider secondary = stub("secondary");

        LlmProvider picked = strategy.pick(request(), List.of(primary, secondary));

        assertThat(picked.name()).isEqualTo("primary");
    }

    @Test
    void returns_null_when_no_providers() {
        assertThat(strategy.pick(request(), List.of())).isNull();
    }

    @Test
    void name_matches_constant() {
        assertThat(strategy.name()).isEqualTo("priority");
    }

    private static ChatRequest request() {
        return new ChatRequest("auto", List.of(ChatMessage.user("hi")), null, null, null, null, null);
    }

    private static LlmProvider stub(String name) {
        return new LlmProvider() {
            @Override public String name() { return name; }
            @Override public boolean supports(String model) { return true; }
            @Override public ChatResponse chat(ChatRequest r) { throw new UnsupportedOperationException(); }
        };
    }
}
