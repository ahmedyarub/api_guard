package org.mindpower.api_guard;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ApiGuardApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ApiGuardApplication.class.getResource("analysis-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 960);
        stage.setTitle("API Guard");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}