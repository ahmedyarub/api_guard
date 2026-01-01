package org.mindpower.api_guard.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Endpoint extends Producer {
    public Endpoint(String url, String controller, String method, String parentFqn) {
        super.url = url;
        super.controller = controller;
        super.method = method;
        super.parentFqn = parentFqn;
    }
}
