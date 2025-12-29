package org.mindpower.api_guard.Utils;

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
        }

        return "";
    }
}
