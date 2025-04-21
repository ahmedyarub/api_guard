package org.mindpower.api_guard.models;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RestClient extends Consumer {
    public RestClient(String url) {
        super.url = url;
    }
}
