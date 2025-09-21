package org.mindpower.api_guard.service;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.RequiredArgsConstructor;
import org.mindpower.api_guard.models.DataHub;
import org.mindpower.api_guard.models.Endpoint;
import org.mindpower.api_guard.models.Producer;

import java.util.List;

@RequiredArgsConstructor
public class EndpointExtractor extends VoidVisitorAdapter<List<Producer>> {
    private final DataHub dataHub;

    @Override
    public void visit(ClassOrInterfaceDeclaration n, List<Producer> endpoints) {
        super.visit(n, endpoints);

        // Check if class has @RestController or @Controller
        boolean isController = n.getAnnotations()
                .stream()
                .anyMatch(a -> a.getNameAsString().equals("RestController") || a.getNameAsString()
                        .equals("Controller"));

        if (!isController) {
            return;
        }

        String classPath = extractPathFromAnnotation(n.getAnnotations(), "RequestMapping");

        // Process all methods in the controller
        n.getMethods().forEach(method -> {
            extractEndpointFromMethod(
                    method,
                    classPath, n.getFullyQualifiedName().orElse(n.getNameAsString()),
                    endpoints
            );
        });
    }

    private void extractEndpointFromMethod(MethodDeclaration method, String classPath, String className, List<Producer> endpoints) {

        for (AnnotationExpr annotation : method.getAnnotations()) {
            String annotationName = annotation.getNameAsString();
            String httpMethod = null;
            String methodPath = switch (annotationName) {
                case "GetMapping" -> {
                    httpMethod = "GET";
                    yield extractPathFromAnnotation(annotation);
                }
                case "PostMapping" -> {
                    httpMethod = "POST";
                    yield extractPathFromAnnotation(annotation);
                }
                case "PutMapping" -> {
                    httpMethod = "PUT";
                    yield extractPathFromAnnotation(annotation);
                }
                case "DeleteMapping" -> {
                    httpMethod = "DELETE";
                    yield extractPathFromAnnotation(annotation);
                }
                case "PatchMapping" -> {
                    httpMethod = "PATCH";
                    yield extractPathFromAnnotation(annotation);
                }
                case "RequestMapping" -> {
                    httpMethod = extractHttpMethodFromRequestMapping(annotation);
                    yield extractPathFromAnnotation(annotation);
                }
                default -> "";
            };

            if (httpMethod != null) {
                String fullPath = combinePaths(classPath, methodPath);

                var endpoint = new Endpoint(fullPath, className, method.getName().toString(), dataHub.getFqn());

                endpoints.add(endpoint);
            }
        }
    }

    private String extractPathFromAnnotation(NodeList<AnnotationExpr> annotations, String annotationName) {
        return annotations.stream()
                .filter(a -> a.getNameAsString().equals(annotationName))
                .findFirst()
                .map(this::extractPathFromAnnotation)
                .orElse("");
    }

    private String extractPathFromAnnotation(AnnotationExpr annotation) {
        if (annotation instanceof SingleMemberAnnotationExpr singleMember) {
            return extractStringValue(singleMember.getMemberValue());
        } else if (annotation instanceof NormalAnnotationExpr normal) {
            return normal.getPairs()
                    .stream()
                    .filter(p -> p.getNameAsString().equals("value") || p.getNameAsString().equals("path"))
                    .findFirst()
                    .map(p -> extractStringValue(p.getValue()))
                    .orElse("");
        }
        return "";
    }

    private String extractHttpMethodFromRequestMapping(AnnotationExpr annotation) {
        if (annotation instanceof NormalAnnotationExpr normal) {
            return normal.getPairs().stream().filter(p -> p.getNameAsString().equals("method")).findFirst().map(p -> {
                Expression value = p.getValue();
                if (value instanceof FieldAccessExpr) {
                    return ((FieldAccessExpr) value).getNameAsString().replace("RequestMethod.", "");
                }
                return "GET"; // Default
            }).orElse("GET");
        }
        return "GET";
    }

    private String extractStringValue(Expression expr) {
        if (expr instanceof StringLiteralExpr) {
            return ((StringLiteralExpr) expr).getValue();
        } else if (expr instanceof ArrayInitializerExpr array) {
            var value = array.getValues().getFirst();
            if (value.isPresent()) {
                return extractStringValue(value.get());
            }
        }
        return "";
    }

    private String combinePaths(String classPath, String methodPath) {
        if (classPath == null || classPath.isEmpty()) {
            return methodPath.isEmpty() ? "/" : methodPath;
        }
        if (methodPath.isEmpty()) {
            return classPath;
        }
        if (!classPath.endsWith("/") && !methodPath.startsWith("/")) {
            return classPath + "/" + methodPath;
        }
        if (classPath.endsWith("/") && methodPath.startsWith("/")) {
            return classPath + methodPath.substring(1);
        }
        return classPath + methodPath;
    }
}