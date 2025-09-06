package org.mindpower.api_guard.service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import javafx.util.Pair;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.mindpower.api_guard.models.*;
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

@NoArgsConstructor
public class AnalysisService {
    private static final List<String> endpointAnnotations = List.of("RestController");
    private static final List<String> clientAnnotations = List.of("FeignClient");

    @Getter
    private final ArrayList<DataHub> dataHubs = new ArrayList<>();

    public void addFolder(Path rootPath) throws XPathExpressionException, ParserConfigurationException, IOException, SAXException {
        dataHubs.addAll(scanDataHubs(rootPath));
    }

    public void analyze() {
        var producersMap = parseProducersMap();

        createDataHubsGraph(producersMap);
    }

    private ArrayList<DataHub> scanDataHubs(Path base) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        var dataHubs = new ArrayList<DataHub>();

        for (var it = find(base, "xml"); it.hasNext(); ) {
            var f = it.next();
            if (f.getName().equals("pom.xml")) {
                var dataHub = new DataHub();

                var factory = DocumentBuilderFactory.newInstance();
                var builder = factory.newDocumentBuilder();
                var file = new File(f.getAbsolutePath());
                var doc = builder.parse(file);
                var xpath = XPathFactory.newInstance().newXPath();

                dataHub.setGroupId(xpath.evaluate("/project/groupId", doc));
                dataHub.setArtifactId(xpath.evaluate("/project/artifactId", doc));
                dataHub.setContextPath(getContextPath(f.getParent()));
                dataHub.setRootFolder(f.getParent());

                parseJavaFile(f.getParent(), dataHub);

                dataHubs.add(dataHub);
            }

        }

        return dataHubs;
    }

    private void createDataHubsGraph(Map<String, Pair<DataHub, Producer>> producersMap) {
        for (var fromDH : dataHubs) {
            for (var consumer : fromDH.getConsumers()) {
                var url = consumer.getUrl();
                if (producersMap.containsKey(url)) {
                    var toPair = producersMap.get(url);

                    fromDH.getLinks().add(new Link(fromDH, consumer, toPair.getKey(), toPair.getValue()));
                }
            }
        }
    }

    private Map<String, Pair<DataHub, Producer>> parseProducersMap() {
        var map = new HashMap<String, Pair<DataHub, Producer>>();

        for (var dataHub : dataHubs) {
            for (var producer : dataHub.getProducers()) {
                map.put(dataHub.getContextPath() + producer.getUrl(), new Pair<>(dataHub, producer));
            }
        }

        return map;
    }

    private String getContextPath(String path) throws IOException {
        var is = Files.newInputStream(Paths.get(path, "src", "main", "resources", "application.properties"));
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
                                    dataHub.getProducers().add(new Endpoint(url, parsed.getPackageDeclaration().get().getName() + "." + t.getNameAsString(), md.getName().toString(), dataHub.getGroupId() + "." + dataHub.getArtifactId()));
                                } else {
                                    var ann = (NormalAnnotationExpr) t.getAnnotations().stream().filter(annotationExpr -> clientAnnotations.contains(annotationExpr.getName().toString())).findFirst().get();
                                    var pair = ann.getPairs().stream().filter(pairExpr -> pairExpr.getName().toString().equals("path")).findFirst().get();
                                    var context = ((StringLiteralExpr) pair.getValue()).getValue();
                                    var url = ((StringLiteralExpr) sam.getMemberValue()).getValue();

                                    dataHub.getConsumers().add(new RestClient(context + url, parsed.getPackageDeclaration().get().getName() + "." + t.getNameAsString(), dataHub.getGroupId() + "." + dataHub.getArtifactId()));
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

        return FileUtils.iterateFiles(startPath.toFile(), WildcardFileFilter.builder().setWildcards("*" + extension).get(), TrueFileFilter.INSTANCE);
    }
}
