package com.kloudocean.nautilus.provider;

import com.kloudocean.nautilus.domain.ChatMessage;
import com.kloudocean.nautilus.domain.ChatRequest;
import com.kloudocean.nautilus.domain.ChatResponse;
import com.kloudocean.nautilus.domain.Usage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Echoes the last user message back as the assistant response.
 * Always healthy. Used by integration tests and the local quickstart so the
 * gateway works before any real provider keys are configured.
 *
 * Active only when {@code nautilus.provider.mock.enabled=true}.
 */
@Component
@ConditionalOnProperty(prefix = "nautilus.provider.mock", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MockProvider implements LlmProvider {

    public static final String NAME = "mock";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean supports(String model) {
        return model == null
                || model.isBlank()
                || model.startsWith("mock")
                || "auto".equalsIgnoreCase(model)
                || "echo".equalsIgnoreCase(model);
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        String lastUser = lastUserMessage(request);
        String reply = "[mock] " + lastUser;
        int promptTokens = approxTokens(lastUser);
        int completionTokens = approxTokens(reply);
        return ChatResponse.of(
                request.model(),
                NAME,
                ChatMessage.assistant(reply),
                Usage.of(promptTokens, completionTokens));
    }

    private String lastUserMessage(ChatRequest request) {
        for (int i = request.messages().size() - 1; i >= 0; i--) {
            ChatMessage m = request.messages().get(i);
            if (m.role() == com.kloudocean.nautilus.domain.Role.USER) {
                return m.content();
            }
        }
        return "";
    }

    private int approxTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        return Math.max(1, text.length() / 4);
    }
}
