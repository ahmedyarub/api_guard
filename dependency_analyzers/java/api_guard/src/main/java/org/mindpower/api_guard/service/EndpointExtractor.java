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
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.RequiredArgsConstructor;
import org.mindpower.api_guard.models.DataHub;
import org.mindpower.api_guard.models.Endpoint;
import org.mindpower.api_guard.models.Producer;

import java.util.List;

import static org.mindpower.api_guard.Utils.ExtractionUtils.extractPathFromAnnotation;
import static org.mindpower.api_guard.Utils.ExtractionUtils.extractStringValue;

@RequiredArgsConstructor
public class EndpointExtractor extends VoidVisitorAdapter<List<Producer>> {
    private final DataHub dataHub;

    @Override
    public void visit(ClassOrInterfaceDeclaration n, List<Producer> endpoints) {
        super.visit(n, endpoints);

        extractRestController(n, endpoints);
    }

    @Override
    public void visit(MethodDeclaration n, List<Producer> endpoints) {
        super.visit(n, endpoints);

        // Check if method returns RouterFunction
        if (n.getType().toString().contains("RouterFunction")) {
            // Look for @Bean annotation
            boolean isBean = n.getAnnotations().stream().anyMatch(a -> a.getNameAsString().equals("Bean"));

            if (isBean) {
                extractRouterEndpoints(n, endpoints);
            }
        }
    }

    private void extractRouterEndpoints(MethodDeclaration method, List<Producer> endpoints) {
        @SuppressWarnings("unchecked") var className = method.findAncestor(ClassOrInterfaceDeclaration.class)
                .map(cid -> cid.getFullyQualifiedName().orElse("Unknown"))
                .orElse("Unknown");

        // Visit the method body to find route definitions
        method.getBody().ifPresent(body -> body.accept(new VoidVisitorAdapter<>() {
            @Override
            public void visit(MethodCallExpr n, List<Producer> endpoints) {
                super.visit(n, endpoints);

                // Extract route patterns
                if (n.getNameAsString().equals("route")) {
                    extractRouteFromCall(n, className, method.getNameAsString(), endpoints);
                }
            }
        }, endpoints));
    }

    private void extractRestController(ClassOrInterfaceDeclaration n, List<Producer> endpoints) {
        var annotationOptional = n.getAnnotations()
                .stream()
                .filter(a -> a.getNameAsString().equals("RestController") || a.getNameAsString().equals("Controller"))
                .findFirst();

        if (annotationOptional.isEmpty()) {
            return;
        }

        n.getMethods()
                .forEach(method -> extractEndpointFromMethod(method, n.getFullyQualifiedName()
                        .orElse(n.getNameAsString()), endpoints));
    }

    private void extractEndpointFromMethod(MethodDeclaration method, String className, List<Producer> endpoints) {

        for (var annotation : method.getAnnotations()) {
            var methodPath = extractPathFromAnnotation(annotation);

            endpoints.add(new Endpoint(methodPath, className, method.getName().toString(), dataHub.getFqn()));
        }
    }

    private void extractRouteFromCall(MethodCallExpr call, String className, String methodName, List<Producer> endpoints) {
        if (call.getArguments().size() >= 2) {
            var endpoint = new Endpoint();

            endpoint.setMethod(methodName);
            endpoint.setController(className);
            endpoint.setParentFqn(dataHub.getFqn());

            extractRouteInfo(call.getArgument(0), endpoint, endpoints);
        }
    }

    private void extractRouteInfo(Expression expr, Endpoint endpoint, List<Producer> endpoints) {
        if (expr instanceof MethodCallExpr methodCall) {
            var methodName = methodCall.getNameAsString();

            // Handle RequestPredicates.GET("/path") style
            switch (methodName) {
                case "GET", "POST", "PUT", "DELETE", "PATCH" -> {
                    if (!methodCall.getArguments().isEmpty()) {
                        endpoint.setUrl(extractStringValue(methodCall.getArgument(0)));
                    }
                }
                case "path" -> {
                    // Handle RequestPredicates.path("/path")
                    if (!methodCall.getArguments().isEmpty()) {
                        endpoint.setUrl(extractStringValue(methodCall.getArgument(0)));
                    }
                }
                case "method" -> {
                    // Handle RequestPredicates.method(HttpMethod.GET)
                    if (!methodCall.getArguments().isEmpty()) {
                        endpoint.setMethod(extractHttpMethod(methodCall.getArgument(0)));
                    }
                }
            }

            // Check for chained calls like GET("/path").and(...)
            if (methodCall.getScope().isPresent()) {
                extractRouteInfo(methodCall.getScope().get(), endpoint, endpoints);
            } else {
                // only add the endpoint at the end of the chain
                endpoints.add(endpoint);
            }
        }
    }

    private String extractHttpMethod(Expression expr) {
        if (expr instanceof FieldAccessExpr field) {
            return field.getNameAsString();
        }

        return "GET";
    }
}