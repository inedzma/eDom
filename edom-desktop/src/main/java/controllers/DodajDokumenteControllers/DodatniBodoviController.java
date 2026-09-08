package controllers.DodajDokumenteControllers;

import dao.DokumentDAO;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import model.Dokument;
import model.VrstaDokumenta;
import service.PdfService;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.List;

public class DodatniBodoviController {

    @FXML
    private ComboBox<VrstaDokumenta> cmbDokument;

    @FXML
    private Label lblPdf;

    @FXML
    private Label lblInfoDokument;

    private int prijavaId;
    private String pdfBase64;
    private  double bodovi;

    public void init(int prijavaId, String nazivDokumenta, List<VrstaDokumenta> dokumenti, double bodovi) {
        this.bodovi = bodovi;
        this.prijavaId = prijavaId;
        cmbDokument.getItems().addAll(dokumenti);


        cmbDokument.setConverter(new StringConverter<VrstaDokumenta>() {
            @Override
            public String toString(VrstaDokumenta dokument) {
                if (dokument == null) {
                    return null;
                }

                return dokument.getNaziv();
            }

            @Override
            public VrstaDokumenta fromString(String string) {
                // Ovo ti treba samo ako je ComboBox "editable", inače može vratiti null
                return cmbDokument.getItems().stream()
                        .filter(item -> item.getNaziv().equals(string))
                        .findFirst().orElse(null);
            }
        });

        if (nazivDokumenta != null && !nazivDokumenta.isEmpty()) {
            lblInfoDokument.setText("Dodajte dokument za: " + nazivDokumenta);
        } else {
            lblInfoDokument.setText("Dodajte dokument za dodatne bodove");
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
    private void dodajDokument() {
        VrstaDokumenta vrsta = cmbDokument.getValue();
        if (vrsta == null) return;

        Dokument d = new Dokument();
        d.setNaziv(vrsta.getNaziv());
        d.setVrstaDokumenta(vrsta);
        d.setDatumUpload(LocalDate.now());
        d.setDostavljen(true);
        d.setBrojBodova(bodovi);

        if (pdfBase64 != null) {
            d.setDokumentB64(pdfBase64);
        }

        new DokumentDAO().unesiDokument(d, prijavaId);

        cmbDokument.getSelectionModel().clearSelection();
        pdfBase64 = null;
        lblPdf.setText("");
    }
}