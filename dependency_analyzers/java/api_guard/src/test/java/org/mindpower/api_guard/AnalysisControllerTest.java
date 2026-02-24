package org.mindpower.api_guard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindpower.api_guard.models.DataHub;
import org.mindpower.api_guard.service.AnalysisService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisControllerTest {

    @Mock
    private AnalysisService analysisService;

    private AnalysisController controller;

    @BeforeEach
    void setUp() {
        // Use a test subclass to bypass UI components
        controller = new TestableAnalysisController(analysisService);
    }

    @Test
    void testAddProject() throws Exception {
        File mockDir = mock(File.class);
        when(mockDir.exists()).thenReturn(true);
        when(mockDir.isDirectory()).thenReturn(true);
        when(mockDir.getAbsolutePath()).thenReturn("/tmp/test-project");

        // Prepare the service mock
        // Note: Mockito detects unnecessary stubs if getDataHubs is not called.
        // It is called in addProject if projectsList is not null, but projectsList is FXML injected and null here.
        // So we skip this stub if not needed, or make it lenient.
        lenient().when(analysisService.getDataHubs()).thenReturn(new ArrayList<DataHub>());

        controller.addProject(mockDir);

        verify(analysisService, times(1)).addFolder(any(Path.class));
    }

    @Test
    void testPerformAnalysis() {
        // Prepare the service mock
        lenient().when(analysisService.getDataHubs()).thenReturn(new ArrayList<DataHub>());

        controller.performAnalysis();

        verify(analysisService, times(1)).analyze();
    }

    @Test
    void testOnOpenButtonClick() {
        // This relies on the overridden chooseDirectory method in TestableAnalysisController
        controller.onOpenButtonClick(null);

        // Verify addProject logic was triggered (which calls addFolder)
        try {
            verify(analysisService, times(1)).addFolder(any(Path.class));
        } catch (Exception e) {
            // ignore
        }
    }

    // Subclass to override UI-dependent methods
    static class TestableAnalysisController extends AnalysisController {

        public TestableAnalysisController(AnalysisService analysisService) {
            super(analysisService);
        }

        @Override
        protected File chooseDirectory() {
            File mockDir = mock(File.class);
            when(mockDir.exists()).thenReturn(true);
            when(mockDir.isDirectory()).thenReturn(true);
            when(mockDir.getAbsolutePath()).thenReturn("/tmp/mock-selected-dir");
            return mockDir;
        }

        @Override
        protected void drawGraph() {
            // No-op to avoid JavaFX UI calls
        }

        @Override
        protected void saveLastDirectory(File directory) {
            // No-op to avoid writing to user preferences during tests
        }
    }
}
