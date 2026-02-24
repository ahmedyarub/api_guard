package org.mindpower.api_guard.service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindpower.api_guard.models.Consumer;
import org.mindpower.api_guard.models.DataHub;
import org.mindpower.api_guard.models.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RestClientExtractorTest {

    private DataHub dataHub;
    private RestClientExtractor extractor;

    @BeforeEach
    void setUp() {
        dataHub = new DataHub();
        dataHub.setGroupId("org.example");
        dataHub.setArtifactId("test-caller");
        extractor = new RestClientExtractor(dataHub);
    }

    @Test
    void testExtractFeignClient() throws IOException {
        CompilationUnit cu = loadCompilationUnit("/FeignResponderClient.java");
        List<Consumer> clients = new ArrayList<>();

        extractor.visit(cu, clients);

        // Expected 2 methods in FeignResponderClient: utilToLower and utilToUpper
        // Note: The sample file FeignResponderClient.java has methods annotated with GetMapping
        // Let's verify what's actually in there.
        assertFalse(clients.isEmpty(), "Should have found Feign clients");

        // Checking for a specific one
        // The FeignClient has path="/responder_mvc_annotation" and method has path="/annotation/lower/{param}"
        boolean found = clients.stream()
                .anyMatch(c -> c.getUrl().equals("/responder_mvc_annotation/annotation/lower/{param}"));
        assertTrue(found, "Should have found the lower case endpoint");
    }

    @Test
    void testExtractRestTemplate() throws IOException {
        CompilationUnit cu = loadCompilationUnit("/RestTemplateController.java");
        List<Consumer> clients = new ArrayList<>();

        extractor.visit(cu, clients);

        // This file calls restTemplate.getForObject(... + "/annotation/lower/" + ...)
        // The extractStringValue might resolve concatenation if simple enough.
        // Let's see if it catches unique calls.
        assertFalse(clients.isEmpty(), "Should have found RestTemplate calls");
    }

    @Test
    void testExtractWebClient() throws IOException {
        CompilationUnit cu = loadCompilationUnit("/WebClientController.java");
        List<Consumer> clients = new ArrayList<>();

        extractor.visit(cu, clients);

        assertFalse(clients.isEmpty(), "Should have found WebClient calls");
        // WebClientController in test resources uses .uri("/responder_webflux/annotation/lower/{param}", param)
        boolean found = clients.stream()
                .anyMatch(c -> c.getUrl() != null && c.getUrl().contains("/responder_webflux/annotation/lower/{param}"));
        assertTrue(found, "Should have extracted path from WebClient URI call");
    }

    private CompilationUnit loadCompilationUnit(String resourceName) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourceName)) {
            assertNotNull(is, "Resource not found: " + resourceName);
            return StaticJavaParser.parse(is);
        }
    }
}
