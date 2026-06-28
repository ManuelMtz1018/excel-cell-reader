package com.example.excelreader;

import com.jfoenix.controls.JFXDecorator;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/fxml/main-view.fxml"));
        JFXDecorator decorator = new JFXDecorator(stage, loader.load());
        decorator.setCustomMaximize(true);
        Scene scene = new Scene(decorator, 700, 780);
        scene.getStylesheets().add(MainApp.class.getResource("/css/styles.css").toExternalForm());

        MainController controller = loader.getController();

        stage.setTitle("Excel Cell Reader");
        stage.setMinWidth(640);
        stage.setMinHeight(620);
        stage.setOnCloseRequest(event -> controller.stopMonitoring());
        stage.setScene(scene);
        stage.show();
    }

}
