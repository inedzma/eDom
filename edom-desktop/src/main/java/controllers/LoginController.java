package controllers;
import api.ApiException;
import api.AuthApi;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.example.edom.HelloApplication;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

import java.io.IOException;
import java.net.URL;
import java.util.prefs.Preferences;

public class LoginController {

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private TextField txtPasswordVisible;

    @FXML
    private javafx.scene.control.Button btnTogglePassword;

    @FXML
    private ImageView imgEye;

    @FXML
    private Label lblError;

    @FXML
    private javafx.scene.layout.StackPane loadingOverlay;

    @FXML
    private javafx.scene.control.ProgressIndicator loadingIndicator;

    @FXML
    private CheckBox chkRememberMe;



    private boolean passwordShown = false;

    private static final String PREF_KEY_USERNAME = "remembered_username";
    private final Preferences prefs = Preferences.userNodeForPackage(LoginController.class);

    @FXML
    private void initialize() {
        txtPasswordVisible.textProperty().bindBidirectional(txtPassword.textProperty());

        txtPasswordVisible.setVisible(false);
        txtPasswordVisible.setManaged(false);

        txtPassword.setVisible(true);
        txtPassword.setManaged(true);

        setEyeIcon(false);

        String remembered = prefs.get(PREF_KEY_USERNAME, "");
        if (remembered != null && !remembered.isBlank()) {
            txtUsername.setText(remembered);
            if (chkRememberMe != null) {
                chkRememberMe.setSelected(true);
            }
        }
    }

    @FXML
    private void onTogglePasswordVisibility() {
        passwordShown = !passwordShown;

        txtPasswordVisible.setVisible(passwordShown);
        txtPasswordVisible.setManaged(passwordShown);

        txtPassword.setVisible(!passwordShown);
        txtPassword.setManaged(!passwordShown);

        if (passwordShown) {
            txtPasswordVisible.requestFocus();
            txtPasswordVisible.positionCaret(txtPasswordVisible.getText().length());
        } else {
            txtPassword.requestFocus();
            txtPassword.positionCaret(txtPassword.getText().length());
        }

        setEyeIcon(passwordShown);
    }

    private void setEyeIcon(boolean shown) {
        String path = shown ? "/images/eye-off.png" : "/images/eye.png";
        try {
            Image img = new Image(getClass().getResourceAsStream(path));
            imgEye.setImage(img);
        } catch (Exception e) {
            System.out.println(" Ne mogu učitati ikonu: " + path);
        }
    }

    /**
     * Helper metoda koja otvara novu scenu preko cijelog ekrana (maximized)
     */
    private void openScene(Stage stage, Parent root, String title) {
        Scene scene = new Scene(root);

        URL cssUrl = HelloApplication.class.getResource("/styles/style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.out.println("⚠ style.css nije pronađen!");
        }

        stage.setTitle(title);
        stage.setScene(scene);

        stage.setResizable(true);

        stage.show();

        Platform.runLater(() -> {
            stage.setMaximized(true);

            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
        });
    }

    @FXML
    private void onLoginClicked(ActionEvent event) {

        String user = txtUsername.getText().trim();
        String pass = txtPassword.getText();          // lozinka se NE trimuje

        if (user.isEmpty() || pass.isEmpty()) {
            lblError.setText("Unesite korisničko ime i lozinku.");
            return;
        }

        lblError.setText("");
        loadingOverlay.setVisible(true);
        loadingOverlay.toFront();
        btnTogglePassword.setDisable(true);

        Task<Void> loginTask = new Task<>() {
            @Override
            protected Void call() {
                AuthApi.prijavi(user, pass);   // sva provjera je na serveru
                return null;
            }
        };

        loginTask.setOnSucceeded(e -> {
            if (chkRememberMe != null && chkRememberMe.isSelected()) {
                prefs.put(PREF_KEY_USERNAME, user);
            } else {
                prefs.remove(PREF_KEY_USERNAME);
            }

            try {
                FXMLLoader loader = new FXMLLoader(
                        HelloApplication.class.getResource("/views/admin-main-view.fxml"));
                Parent root = loader.load();
                Stage stage = (Stage) txtUsername.getScene().getWindow();
                openScene(stage, root, "E-Dom - Administratorski panel");
            } catch (IOException ex) {
                ex.printStackTrace();
                lblError.setText("Greška pri otvaranju administratorskog ekrana.");
            }

            zavrsiUcitavanje();
        });

        loginTask.setOnFailed(e -> {
            Throwable ex = loginTask.getException();
            lblError.setText(ex instanceof ApiException
                    ? ex.getMessage()
                    : "Greška pri prijavi.");
            if (!(ex instanceof ApiException)) ex.printStackTrace();
            zavrsiUcitavanje();
        });

        Thread t = new Thread(loginTask);
        t.setDaemon(true);
        t.start();
    }

    private void zavrsiUcitavanje() {
        loadingOverlay.setVisible(false);
        btnTogglePassword.setDisable(false);
    }

    @FXML
    private void onForgotPasswordClicked(ActionEvent event) {
        String url = api.ApiClient.bazniUrl() + "/zaboravljena-lozinka";

        // AWT Desktop ne smije blokirati JavaFX nit
        Thread t = new Thread(() -> {
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
            } catch (Exception e) {
                Platform.runLater(() ->
                        lblError.setText("Otvorite u browseru: " + url));
            }
        });
        t.setDaemon(true);
        t.start();
    }
}
