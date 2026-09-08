package controllers;

import controllers.DodajDokumenteControllers.BraniociDokumentiController;
import dao.PrijavaDAO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Prijava;
import model.StatusPrijave;
import service.BraniociRezultat;

import java.time.LocalDate;

public class NovaPrijavaController {

    @FXML private TextField txtAkGod;
    @FXML private TextArea txtNapomena;
    @FXML private TextField txtClanovi;
    @FXML private TextField txtUdaljenost;
    @FXML private CheckBox chkIzbjeglica;
    @FXML private CheckBox chkBratSestra;
    @FXML private VBox vboxBranioci;

    private BraniociDokumentiController braniociController;

    private int studentId;
    private double prosjek;
    private int godinaStudija;

    private final PrijavaDAO prijavaDAO = new PrijavaDAO();

    @FXML
    public void initialize() {
        txtAkGod.sceneProperty().addListener((obs, o, scene) -> {
            if (scene != null) {
                ucitajBraniociFormu();
            }
        });
    }

    public void setStudentId(int id) { this.studentId = id; }
    public void setProsjek(double prosjek) { this.prosjek = prosjek; }
    public void setGodinaStudija(int godinaStudija) { this.godinaStudija = godinaStudija; }

    @FXML
    private void onSaveClicked() {

        if (txtAkGod.getText().isEmpty()
                || txtClanovi.getText().isEmpty()
                || txtUdaljenost.getText().isEmpty()) {
            showAlert("Greška", "Sva polja su obavezna.");
            return;
        }

        int akGod, clanovi;
        double udaljenost;

        try {
            akGod = Integer.parseInt(txtAkGod.getText());
            clanovi = Integer.parseInt(txtClanovi.getText());
            udaljenost = Double.parseDouble(txtUdaljenost.getText());
        } catch (Exception e) {
            showAlert("Greška", "Neispravan unos brojeva.");
            return;
        }

        if (akGod < LocalDate.now().getYear()) {
            showAlert("Greška", "Akademska godina ne može biti manja od tekuće.");
            return;
        }

        Prijava p = new Prijava();
        p.setIdStudent(studentId);
        p.setDatumPrijava(LocalDate.now());
        p.setAkademskaGodina(akGod);
        p.setNapomena(txtNapomena.getText());
        p.setUkupniBodovi(0);
        p.setStatusPrijave(new StatusPrijave(1, "Na čekanju"));

        prijavaDAO.unesiPrijavu(p);
        int prijavaId = p.getIdPrijava();

        int dodatniBodovi = 0;
        if (chkIzbjeglica.isSelected()) dodatniBodovi += 3;
        if (chkBratSestra.isSelected()) dodatniBodovi += 2;

        if (dodatniBodovi > 0) {
            prijavaDAO.dodajBodoveNaPrijavu(prijavaId, dodatniBodovi);
        }

        BraniociRezultat rezultat = null;
        if (braniociController != null) {
            rezultat = braniociController.izracunajBodove();
        }


        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/dodaj-dokumente.fxml")
            );
            Parent root = loader.load();

            DodajDokumenteController controller = loader.getController();

            controller.setPrijavaId(prijavaId);


            controller.setIzbjeglica(chkIzbjeglica.isSelected());
            controller.setBratSestra(chkBratSestra.isSelected());
            controller.setBraniociRezultat(rezultat);


            controller.setProsjek(prosjek);
            controller.setGodinaStudija(godinaStudija);
            controller.setUdaljenost(udaljenost);


            controller.setClanovi(clanovi);

            Stage stage = (Stage) txtAkGod.getScene().getWindow();
            stage.setScene(new Scene(root));

            // BITNO: maximize tek nakon što se scena postavi
            Platform.runLater(() -> {
                stage.setMaximized(true);
                stage.centerOnScreen();
            });

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Greška", "Ne mogu otvoriti dokumente.");
        }
    }

    @FXML
    private void onBackClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/novi-student.fxml")
            );
            Parent root = loader.load();

            Stage stage = (Stage) txtAkGod.getScene().getWindow();
            stage.setScene(new Scene(root));

            Platform.runLater(() -> {
                stage.setMaximized(true);
                stage.centerOnScreen();
            });

        } catch (Exception e) {
            showAlert("Greška", "Ne mogu se vratiti nazad.");
        }
    }

    private void ucitajBraniociFormu() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/DodajDokumenteSections/branioci-forma.fxml")
            );
            Node node = loader.load();
            braniociController = loader.getController();
            vboxBranioci.getChildren().setAll(node);

        } catch (Exception e) {
            showAlert("Greška", "Ne mogu učitati formu za branioce.");
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
