package org.example.micro_service_map.models;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Endpoint extends Producer {
    public Endpoint(String url) {
        super.url = url;
    }
}
