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
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.RequiredArgsConstructor;
import org.mindpower.api_guard.models.Consumer;
import org.mindpower.api_guard.models.DataHub;
import org.mindpower.api_guard.models.RestClient;

import java.util.List;

import static org.mindpower.api_guard.Utils.ExtractionUtils.extractPathFromAnnotation;
import static org.mindpower.api_guard.Utils.ExtractionUtils.extractStringValue;

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

        var baseUrl = extractPathFromAnnotation(annotation.get());

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

        // Check for RestTemplate calls
        if (n.getScope().isPresent()) {
            Expression scope = n.getScope().get();
            String scopeName = scope.toString();

            if (scopeName.contains("restTemplate")) {
                clients.add(extractRestTemplateCall(n));
            } else if (scopeName.contains("webClient")) {
                var client = extractWebClientCall(n);
                if (client != null) {
                    clients.add(client);
                }
            }
        }
    }

    private RestClient extractFeignClient(MethodDeclaration method, String baseUrl, String className) {
        var annotationOptional = method.getAnnotations().getFirst();

        if (annotationOptional.isPresent()) {
            var url = extractPathFromAnnotation(annotationOptional.get());

            return new RestClient(baseUrl + url, className, dataHub.getFqn());
        }

        return null;
    }

    private RestClient extractRestTemplateCall(MethodCallExpr call) {
        String url = null;

        // Extract URL from first argument
        if (!call.getArguments().isEmpty()) {
            url = extractStringValue(call.getArgument(0));
        }

        return new RestClient(url, call.getNameAsString(), dataHub.getFqn());
    }

    private RestClient extractWebClientCall(MethodCallExpr call) {
        var url = extractWebClientUrl(call);

        if (url != null && !url.isEmpty()) {
            return new RestClient(url, call.getNameAsString(), dataHub.getFqn());
        }

        return null;
    }

    private String extractWebClientUrl(MethodCallExpr call) {
        // Look for .uri() method in the chain
        var current = call;
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
}