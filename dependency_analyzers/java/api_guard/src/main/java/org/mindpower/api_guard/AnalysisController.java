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
import org.mindpower.api_guard.models.DataHub;
import org.mindpower.api_guard.models.Link;
import org.mindpower.api_guard.service.AnalysisService;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.nio.file.Path;

public class AnalysisController {
    private final AnalysisService analysisService = new AnalysisService();
    @FXML
    private Button analyzeButton;
    @FXML
    private Button openButton;
    @FXML
    private ListView<DataHub> projectsList;

    @FXML
    public void onOpenButtonClick(ActionEvent actionEvent) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        var directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select the root folder of your projects");

        var selectedDirectory = directoryChooser.showDialog(null);

        if (selectedDirectory != null) {
            analysisService.addFolder(Path.of(selectedDirectory.getAbsolutePath()));

            for (var dataHub : analysisService.getDataHubs()) {
                projectsList.getItems().add(dataHub);
            }
        }
    }

    @FXML
    protected void onAnalyzeButtonClick() {
        analysisService.analyze();

        drawGraph();
    }

    private void drawGraph() {
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
}