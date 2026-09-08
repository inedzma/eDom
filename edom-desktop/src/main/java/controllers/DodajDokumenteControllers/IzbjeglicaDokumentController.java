package controllers.DodajDokumenteControllers;

import dao.DokumentDAO;
import dao.VrstaDokumentaDAO;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import model.Dokument;
import model.VrstaDokumenta;
import service.PdfService;

import java.time.LocalDate;

public class IzbjeglicaDokumentController {

    @FXML
    public Label lblPdf;

    @FXML
    private Label lblInfoDokument;

    @FXML
    private CheckBox chkDokument;

     public void init(int prijavaId) {
        this.prijavaId = prijavaId;
    }

    private int prijavaId;
    private String pdfBase64;

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
        VrstaDokumentaDAO vrstaDao = new VrstaDokumentaDAO();
        VrstaDokumenta vrsta = vrstaDao.dohvatiVrstuPoId(23); // ID za izbjeglica dokument
        if (vrsta == null) return;


        if (!chkDokument.isSelected()) {
            return; // admin nije označio → ne snimamo
        }

        Dokument d = new Dokument();
        d.setNaziv(vrsta.getNaziv());
        d.setVrstaDokumenta(vrsta);
        d.setDatumUpload(LocalDate.now());
        d.setDostavljen(true);
        d.setBrojBodova(3);

        if (pdfBase64 != null) {
            d.setDokumentB64(pdfBase64);
        }

        new DokumentDAO().unesiDokument(d, prijavaId);

        pdfBase64 = null;
        lblPdf.setText("");
    }
}
