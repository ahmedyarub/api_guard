package org.example.micro_service_map.models;

import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@ToString
public class DataHub {
    String fqn;
    String contextPath;
    List<Producer> producers = new ArrayList<>();
    List<Consumer> consumers = new ArrayList<>();
    Set<DataHub> links = new HashSet<>();
}
