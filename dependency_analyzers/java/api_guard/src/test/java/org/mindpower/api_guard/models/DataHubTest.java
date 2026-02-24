package org.mindpower.api_guard.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DataHubTest {

    @Test
    void testGetFqn() {
        DataHub dataHub = new DataHub();
        dataHub.setGroupId("org.mindpower");
        dataHub.setArtifactId("api_guard");

        assertEquals("org.mindpower.api_guard", dataHub.getFqn());
    }
}
