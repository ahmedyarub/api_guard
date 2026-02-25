package org.mindpower.api_guard.models;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public abstract class Producer {
    protected String url;
    protected String controller;
    protected String method;
    protected String parentFqn;
}
