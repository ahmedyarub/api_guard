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

package org.mindpower.api_guard.utils;

import com.github.javaparser.ast.expr.*;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ExtractionUtils {
    public static String extractPathFromAnnotation(AnnotationExpr annotation) {
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

    public static String extractStringValue(Expression expr) {
        if (expr instanceof StringLiteralExpr) {
            return ((StringLiteralExpr) expr).getValue();
        } else if (expr instanceof ArrayInitializerExpr array) {
            var value = array.getValues().getFirst();
            if (value.isPresent()) {
                return extractStringValue(value.get());
            }
        } else if (expr instanceof BinaryExpr binary && binary.getOperator() == BinaryExpr.Operator.PLUS) {
            return extractStringValue(binary.getLeft()) + extractStringValue(binary.getRight());
        }

        return "";
    }
}
