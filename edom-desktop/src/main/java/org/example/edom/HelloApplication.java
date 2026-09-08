package org.example.edom;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                HelloApplication.class.getResource("/views/login-view.fxml")
        );

        Scene scene = new Scene(fxmlLoader.load());

        var cssUrl = HelloApplication.class.getResource("/styles/login-style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        InputStream iconStream = getClass().getResourceAsStream("/images/logoIkona.png");
        if (iconStream != null) {
            Image icon = new Image(iconStream);
            stage.getIcons().add(icon);
        } else {
            System.out.println("Ikona nije pronađena! Provjeri putanju.");
        }

        stage.setTitle("E-Dom - Prijava");
        stage.setScene(scene);

        stage.setResizable(true);
        stage.setMaximized(true);

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
