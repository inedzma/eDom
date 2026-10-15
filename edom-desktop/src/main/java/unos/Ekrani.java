package util;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

public final class Ekrani {

    private Ekrani() {}

    /** Učita FXML, pripremi kontroler i zamijeni scenu u prozoru u kojem je `izvor`. */
    public static <C> C zamijeni(Node izvor, String fxml, Consumer<C> priprema) {
        try {
            FXMLLoader loader = new FXMLLoader(Ekrani.class.getResource(fxml));
            Parent root = loader.load();
            C controller = loader.getController();
            if (priprema != null) priprema.accept(controller);

            Stage stage = (Stage) izvor.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setResizable(true);
            Platform.runLater(() -> stage.setMaximized(true));
            return controller;
        } catch (IOException e) {
            throw new UncheckedIOException("Ne mogu učitati " + fxml, e);
        }
    }
}