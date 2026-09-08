package controllers.DodajDokumenteControllers;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import service.BraniociRezultat;
import service.KriterijPoOsnovuDjeceBranilaca;
import service.PdfService;

import java.util.HashMap;
import java.util.Map;

public class BraniociDokumentiController {

    @FXML private CheckBox chkStudentRVI;
    @FXML private TextField txtPostotakInvaliditeta;

    @FXML private CheckBox chkInvaliditet;

    @FXML private CheckBox chkDjecaSehida;
    @FXML private CheckBox chkClanPorodiceSehida;

    @FXML private CheckBox chkInvalidnostRoditelja;
    @FXML private TextField txtPostInvalidnostiRoditelja;

    @FXML private CheckBox chkDjecaOSRBiH;
    @FXML private TextField txtBrojMjeseci;

    @FXML private CheckBox chkDjecaNosilacaRPriznanja;
    @FXML private CheckBox chkStudentLogoras;
    @FXML private CheckBox chkRoditeljLogoras;
    @FXML private CheckBox chkDjecaBez1Roditelja;
    @FXML private CheckBox chkDjecaBezObaRoditelja;
    @FXML private CheckBox chkBezRoditeljskogStaranja;
    @FXML private CheckBox chkKorisniciSocijalnePomoci;

    @FXML private Label lblPdf;

    private String pdfBase64;
    private final KriterijPoOsnovuDjeceBranilaca kriterij = new KriterijPoOsnovuDjeceBranilaca();


    @FXML
    public void initialize() {

        txtPostotakInvaliditeta.visibleProperty().bind(chkStudentRVI.selectedProperty());
        txtPostotakInvaliditeta.managedProperty().bind(chkStudentRVI.selectedProperty());
        chkStudentRVI.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) txtPostotakInvaliditeta.clear();
        });
        txtPostInvalidnostiRoditelja.visibleProperty().bind(chkInvalidnostRoditelja.selectedProperty());
        txtPostInvalidnostiRoditelja.managedProperty().bind(chkInvalidnostRoditelja.selectedProperty());
        chkInvalidnostRoditelja.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) txtPostInvalidnostiRoditelja.clear();
        });

        txtBrojMjeseci.visibleProperty().bind(chkDjecaOSRBiH.selectedProperty());
        txtBrojMjeseci.managedProperty().bind(chkDjecaOSRBiH.selectedProperty());
        chkDjecaOSRBiH.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) txtBrojMjeseci.clear();
        });
    }


    public BraniociRezultat izracunajBodove() {

        Map<String, Boolean> stanja = new HashMap<>();

        stanja.put("StudentRvi", chkStudentRVI.isSelected());
        stanja.put("Invalidnost", chkInvaliditet.isSelected());
        stanja.put("PoginuoBranilac", chkDjecaSehida.isSelected());
        stanja.put("ClanPorodiceSehida", chkClanPorodiceSehida.isSelected());
        stanja.put("InvalidnostRoditelja", chkInvalidnostRoditelja.isSelected());
        stanja.put("DijeteOSRBiH", chkDjecaOSRBiH.isSelected());
        stanja.put("DjecaNosilacaRPriznanja", chkDjecaNosilacaRPriznanja.isSelected());
        stanja.put("StudentLogoras", chkStudentLogoras.isSelected());
        stanja.put("RoditeljLogoras", chkRoditeljLogoras.isSelected());
        stanja.put("BezJednogRoditelja", chkDjecaBez1Roditelja.isSelected());
        stanja.put("BezObaRoditelja", chkDjecaBezObaRoditelja.isSelected());
        stanja.put("BezRoditeljskogStaratelja", chkBezRoditeljskogStaranja.isSelected());
        stanja.put("KorisniciSocijalnePomoci", chkKorisniciSocijalnePomoci.isSelected());

        return kriterij.izracunaj(
                stanja,
                txtPostotakInvaliditeta,
                txtPostInvalidnostiRoditelja,
                txtBrojMjeseci
        );
    }


    @FXML
    private void onDodajPdf() {

        pdfBase64 = PdfService.uploadPdf(lblPdf.getScene().getWindow());

        if (pdfBase64 != null) {
            lblPdf.setText("PDF dodat");
        } else {
            lblPdf.setText("PDF nije odabran");
        }
    }
}
