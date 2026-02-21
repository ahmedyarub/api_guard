/*
 * Copyright 2026 Ahmed Yarub Hani Al Nuaimi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mindpower.api_guard.service;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.RequiredArgsConstructor;
import org.mindpower.api_guard.models.Consumer;
import org.mindpower.api_guard.models.DataHub;
import org.mindpower.api_guard.models.RestClient;

import java.util.List;
import java.util.Set;

import static org.mindpower.api_guard.utils.ExtractionUtils.extractPathFromAnnotation;
import static org.mindpower.api_guard.utils.ExtractionUtils.extractStringValue;

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

        var baseUrl = resolveProperty(extractPathFromAnnotation(annotation.get()));

        n.getMethods().forEach(method -> {
            var client = extractFeignClient(method, baseUrl, n.getFullyQualifiedName().orElse(n.getNameAsString()));

            if (client != null) {
                clients.add(client);
            }
        });
    }

    @Override
    public void visit(MethodCallExpr n, List<Consumer> clients) {
        super.visit(n, clients);

        if (n.getScope().isPresent()) {
            Expression scope = n.getScope().get();
            String scopeName = scope.toString();

            if (scopeName.contains("restTemplate")) {
                addIfUnique(clients, extractRestTemplateCall(n));
            } else if (scopeName.contains("webClient")) {
                addIfUnique(clients, extractWebClientCall(n));
            }
        }
    }

    private static final Set<String> MAPPING_ANNOTATIONS = Set.of(
            "GetMapping", "PostMapping", "PutMapping", "DeleteMapping",
            "PatchMapping", "RequestMapping"
    );

    private RestClient extractFeignClient(MethodDeclaration method, String baseUrl, String className) {
        var mappingAnnotation = method.getAnnotations().stream()
                .filter(a -> MAPPING_ANNOTATIONS.contains(a.getNameAsString()))
                .findFirst();

        if (mappingAnnotation.isPresent()) {
            var url = extractPathFromAnnotation(mappingAnnotation.get());

            return new RestClient(baseUrl + url, className, dataHub.getFqn());
        }

        return null;
    }

    private RestClient extractRestTemplateCall(MethodCallExpr call) {
        String url = null;

        // Extract URL from first argument
        if (!call.getArguments().isEmpty()) {
            url = extractPath(resolveProperty(extractStringValue(call.getArgument(0))));
        }

        return new RestClient(url, call.getNameAsString(), dataHub.getFqn());
    }

    public String extractPath(String fullUrl) {
        if (fullUrl == null)
            return null;

        int protocolEnd = fullUrl.indexOf("://");

        if (protocolEnd != -1) {
            int pathStart = fullUrl.indexOf("/", protocolEnd + 3);

            if (pathStart != -1) {
                return fullUrl.substring(pathStart);
            }
        }

        return fullUrl;
    }

    private RestClient extractWebClientCall(MethodCallExpr call) {
        var url = extractWebClientUrl(call);

        if (url != null && !url.isEmpty()) {
            return new RestClient(resolveProperty(url), call.getNameAsString(), dataHub.getFqn());
        }

        return null;
    }

    private static void addIfUnique(List<Consumer> clients, Consumer client) {
        if (client != null && client.getUrl() != null
                && clients.stream().noneMatch(c -> c.getUrl().equals(client.getUrl()))) {
            clients.add(client);
        }
    }

    private String extractWebClientUrl(MethodCallExpr call) {
        var current = call;

        while (current != null) {
            if (current.getNameAsString().equals("uri") && !current.getArguments().isEmpty()) {
                var url = extractStringValue(current.getArgument(0));
                if (url != null && !url.isEmpty()) {
                    return url;
                }

                if (current.getArgument(0) instanceof LambdaExpr pathLambda) {
                    var body = pathLambda.getBody();

                    if (body.isExpressionStmt()) {
                        var expr = body.asExpressionStmt().getExpression();

                        if (expr.isMethodCallExpr()) {
                            var lambdaCall = expr.asMethodCallExpr();

                            while (lambdaCall != null) {
                                if (lambdaCall.getNameAsString().equals("path") && !lambdaCall.getArguments()
                                        .isEmpty()) {
                                    return extractStringValue(lambdaCall.getArgument(0));
                                }

                                if (lambdaCall.getScope().isPresent() && lambdaCall.getScope()
                                        .get()
                                        .isMethodCallExpr()) {
                                    lambdaCall = lambdaCall.getScope().get().asMethodCallExpr();
                                } else {
                                    lambdaCall = null; // Stop if we hit the variable name (uriBuilder)
                                }
                            }
                        }
                    }
                }
            }

            if (current.getScope().isPresent() && current.getScope().get() instanceof MethodCallExpr) {
                current = (MethodCallExpr) current.getScope().get();
            } else {
                break;
            }
        }

        return null;
    }

    private String resolveProperty(String url) {
        if (url == null) {
            return null;
        }

        String result = url;
        int maxIterations = 10;
        int iteration = 0;

        while (result.contains("${") && iteration < maxIterations) {
            int start = result.indexOf("${");
            int end = result.indexOf("}", start);

            if (end == -1) {
                break;
            }

            String content = result.substring(start + 2, end);
            String propertyKey = content;
            String defaultValue = null;

            int colonIndex = content.indexOf(":");
            if (colonIndex != -1) {
                propertyKey = content.substring(0, colonIndex);
                defaultValue = content.substring(colonIndex + 1);
            }

            String propertyValue = dataHub.getProperties().get(propertyKey);
            if (propertyValue == null) {
                propertyValue = defaultValue;
            }

            if (propertyValue != null) {
                result = result.replace("${" + content + "}", propertyValue);
            } else {
                break;
            }
            iteration++;
        }

        return result;
    }
}