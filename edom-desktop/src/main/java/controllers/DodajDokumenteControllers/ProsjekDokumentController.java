package controllers.DodajDokumenteControllers;

import dao.DokumentDAO;
import dao.PrijavaDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Dokument;
import model.VrstaDokumenta;
import service.KriterijPoOsnovuUspjeha;

import java.time.LocalDate;

public class ProsjekDokumentController {

    @FXML private Label lblIspiti;
    @FXML private Label lblOpis;
    @FXML private TextField txtBrojPolozenih;
    @FXML private CheckBox chkDostavljen;
    @FXML private RadioButton rbUvjerenje;
    @FXML private RadioButton rbIndeks;

    private final ToggleGroup toggleDokument = new ToggleGroup();

    private int prijavaId;
    private double prosjek;
    private int godinaStudija;

    private VrstaDokumenta vrstaSvjedodzbe;
    private VrstaDokumenta vrstaUvjerenje;
    private VrstaDokumenta vrstaIndeks;

    private final KriterijPoOsnovuUspjeha kriterij = new KriterijPoOsnovuUspjeha();

    @FXML
    public void initialize() {
        rbUvjerenje.setToggleGroup(toggleDokument);
        rbIndeks.setToggleGroup(toggleDokument);
        rbUvjerenje.setSelected(true);
    }

    // Poziva se iz DodajDokumenteController
    public void init(int prijavaId, int godinaStudija, double prosjek,
                     VrstaDokumenta vrstaSvjedodzbe,
                     VrstaDokumenta vrstaUvjerenje,
                     VrstaDokumenta vrstaIndeks) {

        this.prijavaId = prijavaId;
        this.godinaStudija = godinaStudija;
        this.prosjek = prosjek;
        this.vrstaSvjedodzbe = vrstaSvjedodzbe;
        this.vrstaUvjerenje = vrstaUvjerenje;
        this.vrstaIndeks = vrstaIndeks;

        // UI logika
        if (godinaStudija == 1) {
            lblOpis.setText("Svjedodžba o završetku srednje škole:");
            setNodeVisible(lblIspiti, false);
            setNodeVisible(txtBrojPolozenih, false);
            setNodeVisible(rbUvjerenje, false);
            setNodeVisible(rbIndeks, false);
        } else {
            lblOpis.setText("Odaberi i unesi dokument sa fakulteta:");
            setNodeVisible(lblIspiti, true);
            setNodeVisible(txtBrojPolozenih, true);
            setNodeVisible(rbUvjerenje, true);
            setNodeVisible(rbIndeks, true);
            rbUvjerenje.setSelected(true);
        }
    }

    @FXML
    private void dodaj() {
        if (!chkDostavljen.isSelected()) {
            showAlert("Greška", "Morate označiti da je dokument dostavljen.");
            return;
        }

        Dokument d = new Dokument();
        d.setDatumUpload(LocalDate.now());
        d.setDostavljen(true);
        d.setDokumentB64(null); // Ili dodaj logiku za upload fajla ako imaš

        double bodovi;

        // --- LOGIKA RAČUNANJA BODOVA ---
        if (godinaStudija == 1) {
            // Brucoši
            bodovi = kriterij.izracunajBodoveBrucosi(prosjek);
            d.setNaziv(vrstaSvjedodzbe.getNaziv());
            d.setVrstaDokumenta(vrstaSvjedodzbe);

        } else {
            // Stariji studenti
            if (rbUvjerenje.isSelected()) {
                d.setNaziv(vrstaUvjerenje.getNaziv());
                d.setVrstaDokumenta(vrstaUvjerenje);
            } else if (rbIndeks.isSelected()) {
                d.setNaziv(vrstaIndeks.getNaziv());
                d.setVrstaDokumenta(vrstaIndeks);
            } else {
                showAlert("Greška", "Morate odabrati vrstu dokumenta.");
                return;
            }

            int brojPolozenih = parseIntOrZero(txtBrojPolozenih.getText());
            // Ako je unio 0 ili prazno, bodovi ce biti manji, ali validno
            bodovi = kriterij.izracunajBodove(godinaStudija, prosjek, brojPolozenih);
        }

        d.setBrojBodova(bodovi);

        // --- UPIS U BAZU (Dokument + Ukupni bodovi) ---
        DokumentDAO docDao = new DokumentDAO();
        docDao.unesiDokument(d, prijavaId); // Pretpostavljam da ova metoda radi INSERT dokumenta

        PrijavaDAO prijavaDao = new PrijavaDAO();
        prijavaDao.dodajBodoveNaPrijavu(prijavaId, bodovi); // KLJUČNO: Ažurira ukupnu sumu!

        showAlert("Uspješno", "Dokument je evidentiran. Osvojeni bodovi po osnovu uspjeha: " + String.format("%.2f", bodovi));

        // Opcionalno: onemogući dugme da ne klikću dvaput
        // btnDodaj.setDisable(true);
    }

    private void setNodeVisible(Control node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private int parseIntOrZero(String text) {
        try {
            if(text == null || text.trim().isEmpty()) return 0;
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}