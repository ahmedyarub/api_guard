package org.example.caller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
public class CallController {
    private final WebClient webClient;

    public CallController(@Value("${responder.url}") String responderUrl) {
        this.webClient = WebClient.builder().baseUrl(responderUrl).build();
    }

    @GetMapping("/web_client_annotation/{param}")
    public String webClientToAnnotation(@PathVariable String param) {
        return webClient.get()
                .uri("/responder_webflux/annotation/lower/{param}", param)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}