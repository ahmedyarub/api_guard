package org.mindpower.api_guard.models;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public abstract class Consumer {
    protected String url;
    protected String clientClass;
    protected String parentFqn;
}
