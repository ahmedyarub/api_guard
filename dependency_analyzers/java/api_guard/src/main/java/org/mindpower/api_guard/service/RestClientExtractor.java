package org.mindpower.api_guard.service;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.RequiredArgsConstructor;
import org.mindpower.api_guard.models.Consumer;
import org.mindpower.api_guard.models.DataHub;
import org.mindpower.api_guard.models.RestClient;

import java.util.List;

@RequiredArgsConstructor
public class RestClientExtractor extends VoidVisitorAdapter<List<Consumer>> {
    private final DataHub dataHub;

    @Override
    public void visit(ClassOrInterfaceDeclaration n, List<Consumer> clients) {
        super.visit(n, clients);

        var annotation = n.getAnnotations().stream().filter(a -> a.getNameAsString().equals("FeignClient")).findFirst();

        if (annotation.isEmpty()) {
            return;
        }

        if (annotation.get() instanceof NormalAnnotationExpr normal) {
            var baseUrl = normal.getPairs()
                    .stream()
                    .filter(p -> p.getNameAsString().equals("value") || p.getNameAsString().equals("path"))
                    .findFirst()
                    .map(p -> extractStringValue(p.getValue()))
                    .orElse("");

            n.getMethods().forEach(method -> {
                var client = extractFeignClient(method, baseUrl, n.getFullyQualifiedName().orElse(n.getNameAsString()));

                if (client != null) {
                    clients.add(client);
                }
            });
        }
    }

    @Override
    public void visit(MethodCallExpr n, List<Consumer> clients) {
        super.visit(n, clients);

        String methodName = n.getNameAsString();

        // Check for RestTemplate calls
        if (n.getScope().isPresent()) {
            Expression scope = n.getScope().get();
            String scopeName = scope.toString();

            if (scopeName.contains("restTemplate")) {
                RestClient client = extractRestTemplateCall(n, methodName);
                if (client != null) {
                    clients.add(client);
                }
            } else if (scopeName.contains("webClient")) {
                RestClient client = extractWebClientCall(n, methodName);
                if (client != null) {
                    clients.add(client);
                }
            }
        }
    }

    private RestClient extractFeignClient(MethodDeclaration method, String baseUrl, String className) {
        String url = "";
        var annotationOptional = method.getAnnotations().getFirst();

        if (annotationOptional.isPresent()) {
            var annotation = annotationOptional.get();

            if (annotation instanceof SingleMemberAnnotationExpr smAnnotation) {
                url = smAnnotation.getMemberValue().toString().replace("\"", "");
            }

            return new RestClient(baseUrl + url, className, dataHub.getFqn());
        }

        return null;
    }

    private RestClient extractRestTemplateCall(MethodCallExpr call, String methodName) {
        String httpMethod = null;
        String url = null;

        // Map RestTemplate method to HTTP method
        if (methodName.startsWith("get")) {
            httpMethod = "GET";
        } else if (methodName.startsWith("post")) {
            httpMethod = "POST";
        } else if (methodName.startsWith("put")) {
            httpMethod = "PUT";
        } else if (methodName.startsWith("delete")) {
            httpMethod = "DELETE";
        } else if (methodName.equals("exchange")) {
            httpMethod = "EXCHANGE";
        }

        // Extract URL from first argument
        if (!call.getArguments().isEmpty()) {
            url = extractStringValue(call.getArgument(0));
        }

        if (httpMethod != null && url != null && !url.isEmpty()) {
            return new RestClient(url, call.getNameAsString(), dataHub.getFqn());
        }

        return null;
    }

    private RestClient extractWebClientCall(MethodCallExpr call, String methodName) {
        String httpMethod = switch (methodName) {
            case "get" -> "GET";
            case "post" -> "POST";
            case "put" -> "PUT";
            case "delete" -> "DELETE";
            case "patch" -> "PATCH";
            default -> null;

            // Common WebClient method names
        };

        if (httpMethod != null) {
            // Try to find URI in chained method calls
            String url = extractWebClientUrl(call);

            if (url != null && !url.isEmpty()) {
                return new RestClient(url, call.getNameAsString(), dataHub.getFqn());
            }
        }

        return null;
    }

    private String extractWebClientUrl(MethodCallExpr call) {
        // Look for .uri() method in the chain
        MethodCallExpr current = call;
        while (current != null) {
            if (current.getNameAsString().equals("uri") && !current.getArguments().isEmpty()) {
                return extractStringValue(current.getArgument(0));
            }

            // Check if there's a chained call
            if (current.getScope().isPresent() && current.getScope().get() instanceof MethodCallExpr) {
                current = (MethodCallExpr) current.getScope().get();
            } else {
                break;
            }
        }
        return null;
    }

    private String extractStringValue(Expression expr) {
        if (expr instanceof StringLiteralExpr) {
            return ((StringLiteralExpr) expr).getValue();
        }
        return "";
    }
}