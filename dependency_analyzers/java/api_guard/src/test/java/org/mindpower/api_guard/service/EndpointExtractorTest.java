package org.mindpower.api_guard.service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindpower.api_guard.models.DataHub;
import org.mindpower.api_guard.models.Endpoint;
import org.mindpower.api_guard.models.Producer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EndpointExtractorTest {

    private DataHub dataHub;
    private EndpointExtractor extractor;

    @BeforeEach
    void setUp() {
        dataHub = new DataHub();
        dataHub.setGroupId("org.example");
        dataHub.setArtifactId("test-service");
        extractor = new EndpointExtractor(dataHub);
    }

    @Test
    void testExtractFromAnnotationController() throws IOException {
        CompilationUnit cu = loadCompilationUnit("/AnnotationController.java");
        List<Producer> producers = new ArrayList<>();

        extractor.visit(cu, producers);

        assertEquals(1, producers.size(), "Should have found 1 producer in AnnotationController");
        Endpoint endpoint = (Endpoint) producers.get(0);
        assertEquals("/annotation/lower/{param}", endpoint.getUrl());
        assertEquals("annotationConvertLower", endpoint.getMethod());
        assertEquals("org.example.responder.AnnotationController", endpoint.getController());
        assertEquals("org.example.test-service", endpoint.getParentFqn());
    }

    @Test
    void testExtractFromRouterController() throws IOException {
        CompilationUnit cu = loadCompilationUnit("/RouterController.java");
        List<Producer> producers = new ArrayList<>();

        extractor.visit(cu, producers);

        assertEquals(1, producers.size(), "Should have found 1 producer in RouterController");
        Endpoint endpoint = (Endpoint) producers.get(0);
        assertEquals("/router/lower/{param}", endpoint.getUrl());
        assertEquals("route", endpoint.getMethod());
        assertEquals("org.example.responder.RouterController", endpoint.getController());
        assertEquals("org.example.test-service", endpoint.getParentFqn());
    }

    private CompilationUnit loadCompilationUnit(String resourceName) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourceName)) {
            assertNotNull(is, "Resource not found: " + resourceName);
            return StaticJavaParser.parse(is);
        }
    }
}
