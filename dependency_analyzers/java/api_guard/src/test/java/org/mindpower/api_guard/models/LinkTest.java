package org.mindpower.api_guard.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LinkTest {

    @Test
    void testPointers() {
        DataHub from = new DataHub();
        from.setGroupId("com.example");
        from.setArtifactId("from-service");

        DataHub to = new DataHub();
        to.setGroupId("com.example");
        to.setArtifactId("to-service");

        Consumer consumer = new RestClient("/api/test", "TestClient", "com.example.from-service");
        Producer producer = new Endpoint("/api/test", "TestController", "method", "com.example.to-service");

        Link link = new Link(from, consumer, to, producer);

        assertEquals("com.example:from-service", link.getFromPointer());
        assertEquals("com.example:to-service", link.getToPointer());
    }
}
