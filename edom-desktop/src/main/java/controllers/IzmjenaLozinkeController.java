package controllers;

import dao.KorisnikDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.edom.HelloApplication;
import service.EmailService;
import util.PasswordUtil;
import util.ResetTokenManager;
import model.Korisnik;
import util.TokenGenerator;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.sql.SQLException;

public class IzmjenaLozinkeController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label lblInfo;
    @FXML private TextField txtCode;
    @FXML private Button btnReset;
    @FXML private Button btnNazad;

    private final KorisnikDAO korisnikDAO = new KorisnikDAO();

    @FXML
    public void initialize() {
        // Pratimo kada se scena zakači za prozor
        txtUsername.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                // Provjeravamo da li prozor (Window) već postoji
                if (newScene.getWindow() != null) {
                    podesiProzor((Stage) newScene.getWindow());
                } else {
                    // Ako prozor još nije spreman, čekamo da se pojavi
                    newScene.windowProperty().addListener((obs2, oldWindow, newWindow) -> {
                        if (newWindow != null) {
                            podesiProzor((Stage) newWindow);
                        }
                    });
                }
            }
        });
    }

    // Pomoćna metoda za dimenzije prozora
    private void podesiProzor(Stage stage) {
        stage.setWidth(550);  // Povećao sam malo jer 400 može biti usko
        stage.setHeight(600); // 300 je PREMALO za sva ona polja, stavio sam 600
        stage.setResizable(false);
        stage.centerOnScreen();
    }

    private String generatedToken;

    @FXML
    private void onSendCodeClicked() throws SQLException {
        String username = txtUsername.getText().trim();
        if (username.isEmpty()) {
            lblInfo.setText("Unesite korisničko ime ili email.");
            return;
        }

        Korisnik k = korisnikDAO.nadjiUsername(username);
        if (k == null) {
            lblInfo.setText("Korisnik ne postoji.");
            return;
        }

        generatedToken = TokenGenerator.generate();
        ResetTokenManager.dodajToken(k.getEmail(), generatedToken);

        EmailService.sendResetCode(k.getEmail(), generatedToken);

        lblInfo.setText("Kod poslan na email.");
        txtCode.setDisable(false);
        txtNewPassword.setDisable(false);
        txtConfirmPassword.setDisable(false);
        btnReset.setDisable(false);
    }

    @FXML
    private void onResetClicked() throws SQLException {
        String username = txtUsername.getText().trim();
        String code = txtCode.getText().trim();
        String pass1 = txtNewPassword.getText();
        String pass2 = txtConfirmPassword.getText();

        if (!ResetTokenManager.provjeriToken(korisnikDAO.nadjiUsername(username).getEmail(), code)) {
            lblInfo.setText("Neispravan kod!");
            return;
        }

        if (!pass1.equals(pass2)) {
            lblInfo.setText("Lozinke se ne podudaraju.");
            return;
        }

        String hash = PasswordUtil.hash(pass1);
        korisnikDAO.promijeniLozinku(username, hash);
        ResetTokenManager.ukloniToken(korisnikDAO.nadjiUsername(username).getEmail());

        lblInfo.setText("Lozinka uspješno promijenjena.");
    }


    public void nazadNaPrijavu(javafx.event.ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/views/login-view.fxml")
            );

            // Učitaj login scenu
            Scene scene = new Scene(loader.load());

            // Dohvati trenutni prozor (stage) pomoću eventa
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();

            stage.setTitle("E-Dom - Prijava");
            stage.setScene(scene);

            // BITNO: Pošto je ovaj prozor bio fiksne veličine,
            // moramo ga ponovo učiniti rastezljivim prije maksimiziranja
            stage.setResizable(true);
            stage.setMaximized(true);

            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
