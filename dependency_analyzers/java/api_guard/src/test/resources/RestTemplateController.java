package org.example.caller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class CallController {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${responder.url}")
    private String responderUrl;

    @GetMapping("/rest_template_client_router/{param}")
    public String restTemplateToRouter(@PathVariable String param) {
        return restTemplate.getForObject(responderUrl + "/responder_mvc_router/router/lower/{param}", String.class, param);
    }
}