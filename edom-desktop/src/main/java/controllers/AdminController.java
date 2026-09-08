package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class AdminController {

    @FXML
    private StackPane contentArea;

    @FXML
    public void initialize() {
        loadView("/views/dashboard-view.fxml");
    }

    @FXML
    public void showPregledSistema(ActionEvent event) {
        loadView("/views/dashboard-view.fxml");
    }

    @FXML
    public void showPrijave(ActionEvent event) {
        loadView("/views/prijave.fxml");
    }

    @FXML
    public void showStudenti(ActionEvent event) {
        loadView("/views/studenti.fxml");
    }

    @FXML
    public void onMojProfil(ActionEvent event) {
        loadView("/views/moj-profil.fxml");
    }

    public void loadViewPublic(String fxmlPath) {
        loadView(fxmlPath);
    }

    private void loadView(String fxmlPath) {
        if (contentArea == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            var globalCss = getClass().getResource("/css/app.css");
            if (globalCss != null && !view.getStylesheets().contains(globalCss.toExternalForm())) {
                view.getStylesheets().add(globalCss.toExternalForm());
            }

            if ("/views/prijave.fxml".equals(fxmlPath)) {
                var prijaveCss = getClass().getResource("/css/prijave.css");
                if (prijaveCss != null && !view.getStylesheets().contains(prijaveCss.toExternalForm())) {
                    view.getStylesheets().add(prijaveCss.toExternalForm());
                }
            }

            Object controller = loader.getController();
            if (controller instanceof DashboardController) {
                ((DashboardController) controller).setAdminController(this);
            }

            contentArea.getChildren().setAll(view);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void onLogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Odjava");
        alert.setHeaderText(null);
        alert.setContentText("Da li ste sigurni da se želite odjaviti?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/login-view.fxml")
            );
            Parent root = loader.load();

            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void showRangLista(ActionEvent actionEvent) {
        loadView("/views/rang-lista.fxml");
    }
}
