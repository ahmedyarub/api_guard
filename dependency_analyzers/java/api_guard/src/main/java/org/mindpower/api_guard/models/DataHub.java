/*
 * Copyright 2026 Ahmed Yarub Hani Al Nuaimi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mindpower.api_guard.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@ToString(onlyExplicitlyIncluded = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
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
    private String name;
    private String port;
    private Map<String, String> properties = new HashMap<>();

    public String getFqn() {
        return groupId + "." + artifactId;
    }
}
