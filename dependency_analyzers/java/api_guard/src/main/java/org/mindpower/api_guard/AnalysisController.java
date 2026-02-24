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

package org.mindpower.api_guard;

import com.brunomnsilva.smartgraph.graph.DigraphEdgeList;
import com.brunomnsilva.smartgraph.graphview.SmartCircularSortedPlacementStrategy;
import com.brunomnsilva.smartgraph.graphview.SmartGraphPanel;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.java.Log;
import org.mindpower.api_guard.models.DataHub;
import org.mindpower.api_guard.models.Link;
import org.mindpower.api_guard.service.AnalysisService;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.prefs.Preferences;

@Log
public class AnalysisController {
    private static final String LAST_DIRECTORY_KEY = "lastSelectedDirectory";
    private final Preferences prefs = Preferences.userNodeForPackage(ApiGuardApplication.class);
    private AnalysisService analysisService = new AnalysisService();

    // Default constructor for FXML
    public AnalysisController() {
    }

    // Constructor for testing
    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @FXML
    private Button analyzeButton;

    @FXML
    private Button openButton;

    @FXML
    private ListView<DataHub> projectsList;

    @FXML
    public void onOpenButtonClick(ActionEvent actionEvent) {
        var selectedDirectory = chooseDirectory();

        if (selectedDirectory != null) {
            saveLastDirectory(selectedDirectory);
            addProject(selectedDirectory);
        }
    }

    protected File chooseDirectory() {
        var directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select the root folder of your projects");

        directoryChooser.setInitialDirectory(getLastDirectory());

        return directoryChooser.showDialog(null);
    }

    public void addProject(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            try {
                analysisService.addFolder(Path.of(directory.getAbsolutePath()));
                if (projectsList != null) {
                    projectsList.getItems().setAll(analysisService.getDataHubs());
                }
            } catch (Exception e) {
                log.severe(String.format("Error adding project %s: %s", directory.getName(), e.getMessage()));
            }
        }
    }

    public void performAnalysis() {
        onAnalyzeButtonClick();
    }

    @FXML
    protected void onAnalyzeButtonClick() {
        analysisService.analyze();

        drawGraph();
    }

    protected void drawGraph() {
        var g = convertToGraph();

        var initialPlacement = new SmartCircularSortedPlacementStrategy();
        var graphView = new SmartGraphPanel<>(g, initialPlacement);
        var scene = new Scene(graphView, 1024, 768);

        var stage = new Stage(StageStyle.DECORATED);
        stage.setMaximized(true);
        stage.setTitle("Dependency Graph");
        stage.setScene(scene);
        stage.show();

        graphView.init();
    }

    private DigraphEdgeList<DataHub, Link> convertToGraph() {
        var g = new DigraphEdgeList<DataHub, Link>();

        for (var dataHub : analysisService.getDataHubs()) {
            g.insertVertex(dataHub);
        }

        for (var dataHub : analysisService.getDataHubs()) {
            for (var link : dataHub.getLinks()) {
                g.insertEdge(link.getFrom(), link.getTo(), link);
            }
        }

        return g;
    }

    private File getLastDirectory() {
        String lastPath = prefs.get(LAST_DIRECTORY_KEY, System.getProperty("user.home"));
        File dir = new File(lastPath);
        // Validate the directory still exists
        return dir.exists() && dir.isDirectory() ? dir : new File(System.getProperty("user.home"));
    }

    protected void saveLastDirectory(File directory) {
        if (directory != null) {
            // If it's a file, get its parent directory
            String path = directory.isDirectory() ? directory.getAbsolutePath() : directory.getParentFile()
                    .getAbsolutePath();
            prefs.put(LAST_DIRECTORY_KEY, path);
        }
    }
}