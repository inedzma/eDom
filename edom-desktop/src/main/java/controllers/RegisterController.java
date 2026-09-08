package controllers;

import dao.KorisnikDAO;
import dao.UlogaDAO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.Korisnik;
import model.Uloga;
import org.example.edom.HelloApplication;
import util.PasswordUtil;
import util.TextUtil;

import java.io.IOException;
import java.sql.SQLException;

public class RegisterController {

    @FXML private TextField txtIme;
    @FXML private TextField txtPrezime;
    @FXML private TextField txtUsername;
    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtPassword2;
    @FXML private Label lblError;

    private final KorisnikDAO korisnikDAO = new KorisnikDAO();
    private final UlogaDAO ulogaDAO = new UlogaDAO();

    @FXML
    private void onRegisterClicked(ActionEvent event) throws SQLException {

        String ime = txtIme.getText().trim();
        String prezime = txtPrezime.getText().trim();
        String username = txtUsername.getText().trim();
        String email = txtEmail.getText().trim();
        String pass1 = txtPassword.getText();
        String pass2 = txtPassword2.getText();

        if (ime.isEmpty() || prezime.isEmpty() || username.isEmpty()
                || email.isEmpty() || pass1.isEmpty() || pass2.isEmpty()) {
            lblError.setText("Popunite sva polja.");
            return;
        }

        if (!pass1.equals(pass2)) {
            lblError.setText("Lozinke se ne podudaraju.");
            return;
        }

        if (korisnikDAO.nadjiUsername(username) != null) {
            lblError.setText("Korisničko ime je zauzeto.");
            return;
        }

        if (pass1.length() < 8
                || !pass1.matches(".*[A-Z].*")
                || !pass1.matches(".*[a-z].*")
                || !pass1.matches(".*\\d.*")
                || !pass1.matches(".*[!@#$%^&*()].*")) {

            lblError.setText("Lozinka mora imati min. 8 karaktera, veliko i malo slovo, broj i specijalni znak.");
            return;
        }

        ime = TextUtil.formatirajIme(ime);
        prezime = TextUtil.formatirajIme(prezime);

        Uloga adminUloga = ulogaDAO.dohvatiUloguPoId(2); // pretpostavka: ADMIN = ID 1
        if (adminUloga == null) {
            lblError.setText("Greška: ADMIN uloga ne postoji u bazi.");
            return;
        }

        Korisnik k = new Korisnik();
        k.setIme(ime);
        k.setPrezime(prezime);
        k.setUsername(username);
        k.setEmail(email);
        k.setPasswordHash(PasswordUtil.hash(pass1));
        k.setUloga(adminUloga);

        try {
            korisnikDAO.unesiKorisnika(k);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Registracija");
            alert.setHeaderText(null);
            alert.setContentText("Administrator je uspješno registrovan.");
            alert.showAndWait();

            otvoriLoginEkran(event);

        } catch (Exception e) {
            e.printStackTrace();
            lblError.setText("Greška pri registraciji.");
        }
    }

    @FXML
    private void onBackToLoginClicked(ActionEvent event) {
        otvoriLoginEkran(event);
    }

    private void otvoriLoginEkran(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/views/login-view.fxml")
            );

            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle("E-Dom - Prijava");
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
