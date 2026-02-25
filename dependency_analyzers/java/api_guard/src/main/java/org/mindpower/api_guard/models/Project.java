package org.mindpower.api_guard.models;

import lombok.Data;

import java.nio.file.Path;

@Data
public class Project {
    String name;
    Path path;
}
