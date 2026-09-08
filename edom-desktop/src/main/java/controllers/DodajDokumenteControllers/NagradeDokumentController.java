package controllers.DodajDokumenteControllers;

import dao.DokumentDAO;
import dao.PrijavaDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import model.Dokument;
import model.VrstaDokumenta;
import service.KriterijPoOsnovuUdaljenosti;

import java.time.LocalDate;

public class NagradeDokumentController {

    @FXML
    private CheckBox ckhImaNagrade;

    @FXML
    private CheckBox chkDostavljen;

    @FXML
    private Button dodaj;

    private int prijavaId;
    private VrstaDokumenta vrstaDokumenta;
    private double udaljenost;

    public void init(int prijavaId, VrstaDokumenta vrstaDokumenta) {
        this.prijavaId = prijavaId;
        this.vrstaDokumenta = vrstaDokumenta;
    }


    public void dodaj() {
        if (!ckhImaNagrade.isSelected()) {
            showAlert("Greška", "Morate označiti da student ima osvojene nagrade.");
            return;
        }

        if (!chkDostavljen.isSelected()) {
            showAlert("Greška", "Dokument mora biti dostavljen.");
            return;
        }

        Dokument d = new Dokument();
        d.setNaziv("Dokaz o osvojenim nagradama");
        d.setDatumUpload(LocalDate.now());
        d.setBrojBodova(3);
        d.setDostavljen(true);
        d.setVrstaDokumenta(vrstaDokumenta);
        d.setDokumentB64(null);

        new DokumentDAO().unesiDokument(d, prijavaId);

        PrijavaDAO prijavaDAO = new PrijavaDAO();
        prijavaDAO.dodajBodoveNaPrijavu(prijavaId, 3);

        showAlert("Uspješno", "Dodano 3 boda za osvojene nagrade.");
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
