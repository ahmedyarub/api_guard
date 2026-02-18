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

package org.mindpower.api_guard.service;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import javafx.util.Pair;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.mindpower.api_guard.models.DataHub;
import org.mindpower.api_guard.models.Link;
import org.mindpower.api_guard.models.Producer;
import org.xml.sax.SAXException;
import org.yaml.snakeyaml.Yaml;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Log
@NoArgsConstructor
public class AnalysisService {
    private final JavaParser javaParser = new JavaParser();
    private final Yaml yamlParser = new Yaml();

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
                if (!isSpringBootApplication(f)) {
                    log.warning("Project is not a Spring Boot application!");

                    continue;
                }

                var dataHub = new DataHub();

                var factory = DocumentBuilderFactory.newInstance();
                var builder = factory.newDocumentBuilder();
                var file = new File(f.getAbsolutePath());
                var doc = builder.parse(file);
                var xpath = XPathFactory.newInstance().newXPath();

                dataHub.setGroupId(xpath.evaluate("/project/groupId", doc));
                dataHub.setArtifactId(xpath.evaluate("/project/artifactId", doc));

                var serviceName = getProperty(f.getParentFile(), Collections.singletonList("spring.application.name"));
                dataHub.setName(serviceName == null ? dataHub.getArtifactId() : serviceName);

                dataHub.setContextPath(getProperty(f.getParentFile(), List.of("server.servlet.context-path", "spring.webflux.base-path")));
                dataHub.setRootFolder(f.getParent());

                parseJavaFile(f.getParent(), dataHub);

                dataHubs.add(dataHub);
            }
        }

        return dataHubs;
    }

    private boolean isSpringBootApplication(File pomFile) {
        try {
            String content = Files.readString(pomFile.toPath());
            return content.contains("spring-boot-starter") || content.contains("org.springframework.boot");
        } catch (IOException e) {
            log.severe(String.format("Error reading pom.xml: %s", e.getMessage()));
            return false;
        }
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

    @SuppressWarnings("unchecked")
    private String getProperty(File serviceDir, List<String> propertyPaths) {
        // Try to get from application properties
        File appProps = new File(serviceDir, "src/main/resources/application.properties");
        File appYaml = new File(serviceDir, "src/main/resources/application.yml");
        File appYaml2 = new File(serviceDir, "src/main/resources/application.yaml");

        try {
            if (appProps.exists()) {
                try (FileInputStream fis = new FileInputStream(appProps)) {
                    Properties props = new Properties();

                    props.load(fis);

                    for (var propertyPath : propertyPaths) {
                        var name = props.getProperty(propertyPath);
                        if (name != null && !name.isEmpty()) {
                            return name;
                        }
                    }
                }
            }

            if (appYaml.exists() || appYaml2.exists()) {
                File yamlFile = appYaml.exists() ? appYaml : appYaml2;

                try (FileInputStream fis = new FileInputStream(yamlFile)) {
                    Object current = yamlParser.load(fis);

                    for (var propertyPath : propertyPaths) {
                        String[] parts = propertyPath.split("\\.");
                        for (String part : parts) {
                            if (current == null) {
                                return null;
                            }

                            if (current instanceof Map) {
                                Map<String, Object> map = (Map<String, Object>) current;
                                current = map.get(part);
                            } else {
                                // Not a map, can't navigate further
                                return null;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.severe(String.format("Error reading application config: %s", e.getMessage()));
        }

        // Fallback to directory name
        return "/" + serviceDir.getName();
    }

    void parseJavaFile(String root, DataHub dataHub) {

        for (Iterator<File> it = find(Path.of(root), "java"); it.hasNext(); ) {
            var f = it.next();
            try {
                ParseResult<CompilationUnit> parseResult = javaParser.parse(f);
                if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                    var cu = parseResult.getResult().get();

                    // Extract endpoints
                    var endpointExtractor = new EndpointExtractor(dataHub);
                    cu.accept(endpointExtractor, dataHub.getProducers());

                    // Extract REST clients
                    var clientExtractor = new RestClientExtractor(dataHub);
                    cu.accept(clientExtractor, dataHub.getConsumers());
                }
            } catch (Exception e) {
                log.severe(String.format("Error parsing Java file %s: %s", f.getName(), e.getMessage()));
            }
        }
    }

    private Iterator<File> find(Path startPath, String extension) {
        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }

        return FileUtils.iterateFiles(startPath.toFile(), WildcardFileFilter.builder()
                .setWildcards("*" + extension)
                .get(), TrueFileFilter.INSTANCE);
    }
}
