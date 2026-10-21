package controllers;

import ba.unze.edom.common.dto.KategorijaRanga;
import ba.unze.edom.common.dto.RangListaAdminDTO;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import service.export.ExportOpcije;
import service.export.RangKolona;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class RangListaExportDijalog extends Dialog<ExportOpcije> {

    public RangListaExportDijalog(RangListaAdminDTO lista, List<RangKolona> vidljiveKolone) {
        setTitle("Izvoz rang liste u PDF");
        setHeaderText("Odaberi šta ide u PDF");

        // liste
        CheckBox chkBrucosi = new CheckBox("Brucoši (" + lista.brucosi().size() + ")");
        CheckBox chkSaUslovom = new CheckBox("Više godine s uslovom (" + lista.saUslovom().size() + ")");
        CheckBox chkCekanje = new CheckBox("Lista čekanja (" + lista.listaCekanja().size() + ")");
        CheckBox chkObjedinjeno = new CheckBox("Objedinjena lista (svi po bodovima)");
        chkBrucosi.setSelected(true);
        chkSaUslovom.setSelected(true);
        chkCekanje.setSelected(true);

        CheckBox chkSamoPrimljeni = new CheckBox("Samo primljeni (za brucoše i listu s uslovom)");
        chkSamoPrimljeni.setSelected(true);

        // verzija
        ToggleGroup verzija = new ToggleGroup();
        RadioButton rbJavna = new RadioButton("Javna – R.br, šifra i bodovi (oglasna tabla, web)");
        RadioButton rbInterna = new RadioButton("Interna – kolone koje su sada vidljive u tabeli ("
                + vidljiveKolone.size() + ")");
        rbJavna.setToggleGroup(verzija);
        rbInterna.setToggleGroup(verzija);
        rbJavna.setSelected(true);

        // vrsta liste
        ToggleGroup vrsta = new ToggleGroup();
        RadioButton rbPreliminarna = new RadioButton("Preliminarna");
        RadioButton rbKonacna = new RadioButton("Konačna");
        rbPreliminarna.setToggleGroup(vrsta);
        rbKonacna.setToggleGroup(vrsta);
        rbPreliminarna.setSelected(true);

        // useljenje
        DatePicker dpOd = new DatePicker(LocalDate.now().plusDays(4));
        DatePicker dpDo = new DatePicker(LocalDate.now().plusDays(12));
        GridPane datumi = new GridPane();
        datumi.setHgap(8);
        datumi.setVgap(6);
        datumi.addRow(0, new Label("Useljenje od:"), dpOd);
        datumi.addRow(1, new Label("Useljenje do:"), dpDo);

        VBox sadrzaj = new VBox(8,
                naslov("Liste"), chkBrucosi, chkSaUslovom, chkCekanje, chkObjedinjeno, chkSamoPrimljeni,
                new Separator(),
                naslov("Verzija"), rbJavna, rbInterna,
                new Separator(),
                naslov("Vrsta liste"), rbPreliminarna, rbKonacna,
                new Separator(),
                naslov("Rok za useljenje"), datumi);
        sadrzaj.setPadding(new Insets(10));
        getDialogPane().setContent(sadrzaj);

        ButtonType izvezi = new ButtonType("Izvezi", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(izvezi, ButtonType.CANCEL);

        // bar jedna lista mora biti odabrana, i datumi moraju imati smisla
        Node btnIzvezi = getDialogPane().lookupButton(izvezi);
        btnIzvezi.disableProperty().bind(
                chkBrucosi.selectedProperty()
                        .or(chkSaUslovom.selectedProperty())
                        .or(chkCekanje.selectedProperty())
                        .or(chkObjedinjeno.selectedProperty())
                        .not()
                        .or(dpOd.valueProperty().isNull())
                        .or(dpDo.valueProperty().isNull()));

        setResultConverter(bt -> {
            if (bt != izvezi) return null;
            Set<KategorijaRanga> liste = EnumSet.noneOf(KategorijaRanga.class);
            if (chkBrucosi.isSelected()) liste.add(KategorijaRanga.BRUCOS);
            if (chkSaUslovom.isSelected()) liste.add(KategorijaRanga.SA_USLOVOM);
            if (chkCekanje.isSelected()) liste.add(KategorijaRanga.LISTA_CEKANJA);

            LocalDate od = dpOd.getValue();
            LocalDate doDatuma = dpDo.getValue().isBefore(od) ? od : dpDo.getValue();

            return new ExportOpcije(liste, chkObjedinjeno.isSelected(), rbJavna.isSelected(),
                    vidljiveKolone, rbKonacna.isSelected(), chkSamoPrimljeni.isSelected(), od, doDatuma);
        });
    }

    private Label naslov(String tekst) {
        Label l = new Label(tekst);
        l.setStyle("-fx-font-weight: bold;");
        return l;
    }
}