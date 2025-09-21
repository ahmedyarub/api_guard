package org.mindpower.api_guard;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import org.mindpower.api_guard.service.AnalysisService;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public class ApiGuardApplication extends Application implements Runnable {
    @CommandLine.Option(names = {"--cli"}, description = "Run CLI instead of GUI")
    Boolean isCli;

    @CommandLine.Option(names = {"--path"}, description = "Root path of projects")
    String projectsPath;

    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--cli")) {
            new CommandLine(new ApiGuardApplication()).execute(args);
        } else {
            launch();
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        var fxmlLoader = new FXMLLoader(ApiGuardApplication.class.getResource("analysis-view.fxml"));
        var scene = new Scene(fxmlLoader.load(), 300, 200);

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