package org.mindpower.api_guard.models;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DataHub {
    @ToString.Include
    @EqualsAndHashCode.Include
    String groupId;

    @ToString.Include
    @EqualsAndHashCode.Include
    String artifactId;

    @ToString.Include
    @EqualsAndHashCode.Include
    String contextPath;

    String rootFolder;
    List<Producer> producers = new ArrayList<>();
    List<Consumer> consumers = new ArrayList<>();
    Set<Link> links = new HashSet<>();
}
