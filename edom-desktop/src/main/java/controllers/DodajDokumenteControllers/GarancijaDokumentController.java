package controllers.DodajDokumenteControllers;

import dao.DokumentDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import model.Dokument;
import model.VrstaDokumenta;
import service.PdfService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GarancijaDokumentController {

    @FXML private TextField txtImeGaranta;
    @FXML private ComboBox<String> cmbTipGaranta;
    @FXML private CheckBox chkGarancijaPotpisana;
    @FXML private ComboBox<VrstaDokumenta> cmbDokumentGarancija;
    @FXML private Button btnDodaj;
    @FXML private Label lblPdf;

    private String pdfBase64;

    private int prijavaId;
    private List<VrstaDokumenta> vrsteDokumenata = new ArrayList<>();

    public void init(int prijavaId, List<VrstaDokumenta> vrsteDokumenata) {
        this.prijavaId = prijavaId;
        this.vrsteDokumenata = vrsteDokumenata;

        // Tipovi garanta (ako treba)
        cmbTipGaranta.setItems(FXCollections.observableArrayList(
                "Zaposlenik",
                "Penzioner",
                "Vlasnik kompanije"
        ));

        cmbDokumentGarancija.setItems(FXCollections.observableArrayList(this.vrsteDokumenata));
        cmbDokumentGarancija.setDisable(false);
        cmbDokumentGarancija.setConverter(new StringConverter<>() {
            @Override
            public String toString(VrstaDokumenta objekt) {
                return objekt != null ? objekt.getNaziv() : "";
            }

            @Override
            public VrstaDokumenta fromString(String string) {
                return null;
            }
        });

        cmbDokumentGarancija.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(VrstaDokumenta item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNaziv());
            }
        });

        cmbDokumentGarancija.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(VrstaDokumenta item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNaziv());
            }
        });

        if (!this.vrsteDokumenata.isEmpty()) {
            cmbDokumentGarancija.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void onDodajPdf() {
        pdfBase64 = PdfService.uploadPdf(lblPdf.getScene().getWindow());

        if (pdfBase64 != null) {
            lblPdf.setText("PDF dodat");
        } else {
            lblPdf.setText("PDF nije dodat");
        }
    }

    @FXML
    private void dodajDokumentGarancija() {

        if (txtImeGaranta.getText().trim().isEmpty()
                || cmbTipGaranta.getValue() == null
                || !chkGarancijaPotpisana.isSelected()
                || cmbDokumentGarancija.getValue() == null) {

            new Alert(
                    Alert.AlertType.WARNING,
                    "Popunite ime garanta, tip, označite ovjeru i odaberite dokument."
            ).showAndWait();
            return;
        }

        Dokument d = new Dokument();

        d.setNaziv("Garancija - " + cmbDokumentGarancija.getValue().getNaziv()
                + " (" + txtImeGaranta.getText().trim() + ")");

        d.setDatumUpload(LocalDate.now());
        d.setDostavljen(true);
        d.setVrstaDokumenta(cmbDokumentGarancija.getValue());

        if (pdfBase64 != null && !pdfBase64.isBlank()) {
            d.setDokumentB64(pdfBase64);
        }

        new DokumentDAO().unesiDokument(d, prijavaId);

        new Alert(Alert.AlertType.INFORMATION,
                "Garancija uspješno dodana!"
        ).showAndWait();

        resetForma();
    }

    private void resetForma() {
        txtImeGaranta.clear();
        cmbTipGaranta.setValue(null);
        chkGarancijaPotpisana.setSelected(false);

        cmbDokumentGarancija.setItems(FXCollections.observableArrayList(vrsteDokumenata));
        if (!vrsteDokumenata.isEmpty()) {
            cmbDokumentGarancija.getSelectionModel().selectFirst();
        }

        pdfBase64 = null;
        lblPdf.setText("PDF nije dodat");
    }
}
