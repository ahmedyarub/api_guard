package org.mindpower.api_guard.models;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public abstract class Producer {
    protected String url;
    protected String controller;
    protected String method;
    protected String parentFqn;
}
