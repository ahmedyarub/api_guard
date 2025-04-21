package org.mindpower.api_guard;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.mindpower.api_guard.models.DataHub;
import org.mindpower.api_guard.models.Endpoint;
import org.mindpower.api_guard.models.RestClient;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class AnalysisController {
    private static final List<String> endpointAnnotations = List.of("RestController");
    private static final List<String> clientAnnotations = List.of("FeignClient");

    @FXML
    private Button analyzeButton;

    @FXML
    protected void onAnalyzeButtonClick() throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        var dataHubs = new ArrayList<DataHub>();

        for (Iterator<File> it = find(Path.of("E:/java/multi-micro/"), "xml"); it.hasNext(); ) {
            var f = it.next();
            if (f.getName().equals("pom.xml")) {
                var dataHub = new DataHub();

                var factory = DocumentBuilderFactory.newInstance();
                var builder = factory.newDocumentBuilder();
                var file = new File(f.getAbsolutePath());
                var doc = builder.parse(file);
                var xpath = XPathFactory.newInstance().newXPath();

                dataHub.setFqn(
                        xpath.evaluate("/project/groupId", doc) + xpath.evaluate("/project/artifactId", doc)
                );

                dataHub.setContextPath(getContextPath(f.getParent()));

                parseJavaFile(f.getParent(), dataHub);

                dataHubs.add(dataHub);
            }

        }

        var producersMap = parseProducersMap(dataHubs);

        createDataHubsGraph(producersMap, dataHubs);

        drawGraph(dataHubs);
    }

    private void drawGraph(ArrayList<DataHub> dataHubs) {
        var stage = (Stage) analyzeButton.getScene().getWindow();

        Pane pane = new Pane();

        // Define two nodes
        var g1 = createLabeledNode(250, 400, 200, dataHubs.get(0).getFqn());

        var g2 = createLabeledNode(800, 400, 200, dataHubs.get(1).getFqn());

        // Edge from A to B
        Line edge = new Line(450, 400, 600, 400);

        // Arrowhead
        Polygon arrowHead = createArrowHead(edge);

        pane.getChildren().addAll(edge, arrowHead, g1, g2);

        Scene scene = new Scene(pane, 1280, 960);
        stage.setTitle("Directed Graph Example");
        stage.setScene(scene);
        stage.show();
    }

    public Group createLabeledNode(double x, double y, double radius, String labelText) {
        Circle circle = new Circle(x, y, radius);
        circle.setFill(Color.LIGHTBLUE);
        circle.setStroke(Color.DARKBLUE);

        Text label = new Text(x, y, labelText);
        label.setFill(Color.BLACK);

        // Center the text
        label.setX(x - label.getLayoutBounds().getWidth() / 2);
        label.setY(y + label.getLayoutBounds().getHeight() / 4);

        return new Group(circle, label);
    }

    private Polygon createArrowHead(Line line) {
        double ex = line.getEndX();
        double ey = line.getEndY();
        double sx = line.getStartX();
        double sy = line.getStartY();

        double angle = Math.atan2(ey - sy, ex - sx);

        double arrowLength = 10;
        double arrowWidth = 7;

        double x1 = ex - arrowLength * Math.cos(angle - Math.PI / 6);
        double y1 = ey - arrowLength * Math.sin(angle - Math.PI / 6);

        double x2 = ex - arrowLength * Math.cos(angle + Math.PI / 6);
        double y2 = ey - arrowLength * Math.sin(angle + Math.PI / 6);

        Polygon arrowHead = new Polygon();
        arrowHead.getPoints().addAll(ex, ey, x1, y1, x2, y2);
        arrowHead.setFill(Color.BLACK);

        return arrowHead;
    }

    private void createDataHubsGraph(Map<String, DataHub> producersMap, ArrayList<DataHub> dataHubs) {
        for (var dataHub : dataHubs) {
            for (var consumer : dataHub.getConsumers()) {
                var url = consumer.getUrl();
                if (producersMap.containsKey(url)) {
                    dataHub.getLinks().add(producersMap.get(url));
                }
            }
        }
    }

    private Map<String, DataHub> parseProducersMap(ArrayList<DataHub> dataHubs) {
        var map = new HashMap<String, DataHub>();

        for (var dataHub : dataHubs) {
            for (var producer : dataHub.getProducers()) {
                map.put(dataHub.getContextPath() + producer.getUrl(), dataHub);
            }
        }

        return map;
    }

    private String getContextPath(String path) throws IOException {
        var is = Files.newInputStream(Paths.get(path + "\\src\\main\\resources\\application.properties"));
        var props = new Properties();
        props.load(is);

        return props.getProperty("server.servlet.contextPath");
    }

    void parseJavaFile(String root, DataHub dataHub) throws IOException {
        for (Iterator<File> it = find(Path.of(root), "java"); it.hasNext(); ) {
            var f = it.next();
            var parsed = StaticJavaParser.parse(f);

            for (var t : parsed.getTypes()) {
                String annotationType;

                if (t.getAnnotations().stream().anyMatch(annotationExpr -> endpointAnnotations.contains(annotationExpr.getName().toString()))) {
                    annotationType = "Endpoint";
                } else if (t.getAnnotations().stream().anyMatch(annotationExpr -> clientAnnotations.contains(annotationExpr.getName().toString()))) {
                    annotationType = "Client";
                } else {
                    continue;
                }

                for (var m : t.getMembers()) {
                    if (m instanceof MethodDeclaration md) {
                        for (var an : md.getAnnotations()) {
                            if (an instanceof SingleMemberAnnotationExpr sam) {
                                if (annotationType.equals("Endpoint")) {
                                    var url = ((StringLiteralExpr) sam.getMemberValue()).getValue();
                                    dataHub.getProducers().add(new Endpoint(url));
                                } else {
                                    var ann = (NormalAnnotationExpr) t.getAnnotations().stream().filter(annotationExpr -> clientAnnotations.contains(annotationExpr.getName().toString())).findFirst().get();
                                    var pair = ann.getPairs().stream().filter(pairExpr -> pairExpr.getName().toString().equals("path")).findFirst().get();
                                    var context = ((StringLiteralExpr) pair.getValue()).getValue();
                                    var url = ((StringLiteralExpr) sam.getMemberValue()).getValue();
                                    dataHub.getConsumers().add(new RestClient(context + url));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Iterator<File> find(Path startPath, String extension) {
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }
        return FileUtils.iterateFiles(
                startPath.toFile(),
                WildcardFileFilter.builder().setWildcards("*" + extension).get(),
                TrueFileFilter.INSTANCE);
    }
}