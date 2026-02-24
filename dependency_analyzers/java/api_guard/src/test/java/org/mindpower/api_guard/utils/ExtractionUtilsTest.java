package org.mindpower.api_guard.utils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExtractionUtilsTest {

    @Test
    void testExtractStringValue_StringLiteral() {
        Expression expr = StaticJavaParser.parseExpression("\"test\"");
        assertEquals("test", ExtractionUtils.extractStringValue(expr));
    }

    @Test
    void testExtractStringValue_Concatenation() {
        Expression expr = StaticJavaParser.parseExpression("\"hello\" + \"/world\"");
        assertEquals("hello/world", ExtractionUtils.extractStringValue(expr));
    }

    @Test
    void testExtractPathFromAnnotation_SingleMember() {
        AnnotationExpr annotation = StaticJavaParser.parseAnnotation("@GetMapping(\"/api/v1\")");
        assertEquals("/api/v1", ExtractionUtils.extractPathFromAnnotation(annotation));
    }

    @Test
    void testExtractPathFromAnnotation_Normal_Value() {
        AnnotationExpr annotation = StaticJavaParser.parseAnnotation("@GetMapping(value = \"/api/v1\")");
        assertEquals("/api/v1", ExtractionUtils.extractPathFromAnnotation(annotation));
    }

    @Test
    void testExtractPathFromAnnotation_Normal_Path() {
        AnnotationExpr annotation = StaticJavaParser.parseAnnotation("@GetMapping(path = \"/api/v1\")");
        assertEquals("/api/v1", ExtractionUtils.extractPathFromAnnotation(annotation));
    }

    @Test
    void testExtractPathFromAnnotation_Complex() {
        AnnotationExpr annotation = StaticJavaParser.parseAnnotation("@GetMapping(path = \"/api\" + \"/v1\")");
        assertEquals("/api/v1", ExtractionUtils.extractPathFromAnnotation(annotation));
    }

    @Test
    void testExtractPathFromUrl() {
        assertEquals("/api/v1", ExtractionUtils.extractPathFromUrl("http://localhost:8080/api/v1"));
        assertEquals("/api/v1", ExtractionUtils.extractPathFromUrl("https://example.com/api/v1"));
        assertEquals("/api/v1", ExtractionUtils.extractPathFromUrl("/api/v1"));
        assertEquals(null, ExtractionUtils.extractPathFromUrl(null));
        assertEquals("", ExtractionUtils.extractPathFromUrl(""));
    }
}
