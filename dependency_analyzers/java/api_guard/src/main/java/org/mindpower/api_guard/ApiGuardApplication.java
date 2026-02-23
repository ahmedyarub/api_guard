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

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import org.mindpower.api_guard.service.AnalysisService;
import picocli.CommandLine;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public class ApiGuardApplication extends Application implements Runnable {
    @CommandLine.Option(names = {"--cli"}, description = "Run CLI instead of GUI")
    Boolean isCli;

    @CommandLine.Option(names = {"--path"}, description = "Root path of projects")
    String projectsPath;

    @CommandLine.Option(names = {"--open"}, description = "Project paths to open")
    String[] openPaths;

    @CommandLine.Option(names = {"--analyze"}, description = "Analyze immediately")
    boolean analyze;

    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--cli")) {
            new CommandLine(new ApiGuardApplication()).execute(args);
        } else {
            launch(args);
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        var args = getParameters().getRaw().toArray(new String[0]);
        new CommandLine(this).parseArgs(args);

        var fxmlLoader = new FXMLLoader(ApiGuardApplication.class.getResource("analysis-view.fxml"));
        var scene = new Scene(fxmlLoader.load(), 800, 600);

        AnalysisController controller = fxmlLoader.getController();

        if (openPaths != null) {
            for (String path : openPaths) {
                controller.addProject(new File(path));
            }
        }

        if (analyze) {
            controller.performAnalysis();
        }

        stage.setTitle("API Guard");
        stage.setScene(scene);
        stage.show();
    }

    @SneakyThrows
    @Override
    public void run() {
        var analysisService = new AnalysisService();
        analysisService.addFolder(Path.of(projectsPath));
        analysisService.analyze();

        var mapper = new ObjectMapper();

        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(analysisService.getDataHubs()));

        System.exit(0);
    }
}