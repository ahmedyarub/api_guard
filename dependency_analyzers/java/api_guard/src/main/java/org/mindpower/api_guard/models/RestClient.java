package org.mindpower.api_guard.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RestClient extends Consumer {
    public RestClient(String url, String clientClass, String parentFqn) {
        super.url = url;
        super.clientClass = clientClass;
        super.parentFqn = parentFqn;
    }
}
