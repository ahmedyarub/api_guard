package org.mindpower.api_guard.models;

import org.junit.jupiter.api.Test;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectTest {

    @Test
    void testProject() {
        Project project = new Project();
        project.setName("Test Project");
        project.setPath(Paths.get("/tmp/test"));

        assertEquals("Test Project", project.getName());
        assertEquals(Paths.get("/tmp/test"), project.getPath());
    }
}
