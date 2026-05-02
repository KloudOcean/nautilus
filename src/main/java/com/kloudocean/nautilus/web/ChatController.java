package com.kloudocean.nautilus.web;

import com.kloudocean.nautilus.domain.ChatRequest;
import com.kloudocean.nautilus.domain.ChatResponse;
import com.kloudocean.nautilus.routing.RouteResolver;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class ChatController {

    private final RouteResolver resolver;

    public ChatController(RouteResolver resolver) {
        this.resolver = resolver;
    }

    @PostMapping(value = "/chat/completions",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return resolver.route(request);
    }
}
