package controllers.DodajDokumenteControllers;

import dao.DokumentDAO;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import model.Dokument;
import model.VrstaDokumenta;

import java.time.LocalDate;

public class UplatnicaDokumentConroller {
    @FXML
    private CheckBox chkDostavljen;

    private int prijavaId;
    private VrstaDokumenta vrsta;

    public void init(int prijavaId, VrstaDokumenta vrsta) {
        this.prijavaId = prijavaId;
        this.vrsta = vrsta;
    }

    @FXML
    private void dodaj() {

        Dokument d = new Dokument();
        d.setNaziv("Uplatnica za prijavu u studentski dom");
        d.setDatumUpload(LocalDate.now());
        d.setDostavljen(true);
        d.setVrstaDokumenta(vrsta);

        new DokumentDAO().unesiDokument(d, prijavaId);
    }
}
