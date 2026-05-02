package com.kloudocean.nautilus.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@TestPropertySource(properties = {
        "nautilus.routing.strategy=priority",
        "nautilus.routing.providers[0].name=mock",
        "nautilus.routing.providers[0].priority=1",
        "nautilus.routing.providers[0].enabled=true",
        "nautilus.provider.mock.enabled=true"
})
class ChatControllerTest {

    @Autowired private WebApplicationContext context;

    private MockMvc mvc;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void mock_provider_echoes_user_message() throws Exception {
        String body = """
                {
                  "model": "auto",
                  "messages": [
                    {"role": "user", "content": "ping"}
                  ]
                }
                """;

        mvc.perform(post("/v1/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("mock"))
                .andExpect(jsonPath("$.model").value("auto"))
                .andExpect(jsonPath("$.choices[0].message.role").value("assistant"))
                .andExpect(jsonPath("$.choices[0].message.content").value("[mock] ping"))
                .andExpect(jsonPath("$.choices[0].finish_reason").value("stop"))
                .andExpect(jsonPath("$.usage.total_tokens").isNumber());
    }

    @Test
    void rejects_request_with_empty_messages() throws Exception {
        String body = """
                {
                  "model": "auto",
                  "messages": []
                }
                """;

        mvc.perform(post("/v1/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.type").value("invalid_request_error"));
    }

    @Test
    void rejects_request_with_missing_model() throws Exception {
        String body = """
                {
                  "messages": [{"role": "user", "content": "hi"}]
                }
                """;

        mvc.perform(post("/v1/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
