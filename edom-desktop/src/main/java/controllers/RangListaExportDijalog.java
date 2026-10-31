package controllers;

import ba.unze.edom.common.dto.rang.KategorijaRanga;
import ba.unze.edom.common.dto.rang.RangListaAdminDTO;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import service.export.ExportOpcije;
import service.export.RangKolona;

import java.time.LocalDate;
import java.util.*;

public class RangListaExportDijalog extends Dialog<ExportOpcije> {

    private final Map<RangKolona, CheckBox> kolone = new LinkedHashMap<>();

    public RangListaExportDijalog(RangListaAdminDTO lista, List<RangKolona> vidljiveKolone) {
        setTitle("Izvoz rang liste");
        setHeaderText("Odaberi format, liste i kolone");

        // ---------- format ----------
        ToggleGroup format = new ToggleGroup();
        RadioButton rbPdf = new RadioButton("PDF – za objavu i štampu");
        RadioButton rbExcel = new RadioButton("Excel (.xlsx) – za dalju obradu");
        rbPdf.setToggleGroup(format);
        rbExcel.setToggleGroup(format);
        rbPdf.setSelected(true);

        // ---------- liste ----------
        CheckBox chkBrucosi = new CheckBox("Brucoši (" + lista.brucosi().size() + ")");
        CheckBox chkSaUslovom = new CheckBox("Više godine s uslovom (" + lista.saUslovom().size() + ")");
        CheckBox chkCekanje = new CheckBox("Lista čekanja (" + lista.listaCekanja().size() + ")");
        CheckBox chkObjedinjeno = new CheckBox("Objedinjena lista (svi po bodovima)");
        chkBrucosi.setSelected(true);
        chkSaUslovom.setSelected(true);
        chkCekanje.setSelected(true);

        CheckBox chkSamoPrimljeni = new CheckBox("Samo primljeni (za brucoše i listu s uslovom)");
        chkSamoPrimljeni.setSelected(true);

        // ---------- kolone ----------
        GridPane grid = new GridPane();
        grid.setHgap(18);
        grid.setVgap(6);
        int i = 0;
        for (RangKolona k : RangKolona.values()) {
            CheckBox c = new CheckBox(k.naslov());
            c.setSelected(vidljiveKolone.contains(k));
            kolone.put(k, c);
            grid.add(c, i % 2, i / 2);
            i++;
        }

        Button bJavna = new Button("Javna: šifra i bodovi");
        bJavna.setOnAction(e -> oznaci(RangKolona.REDNI_BROJ, RangKolona.SIFRA, RangKolona.UKUPNO));
        Button bBez = new Button("Bez bodova");
        bBez.setOnAction(e -> oznaci(RangKolona.REDNI_BROJ, RangKolona.SIFRA));
        Button bSve = new Button("Sve");
        bSve.setOnAction(e -> kolone.values().forEach(c -> c.setSelected(true)));
        HBox brzi = new HBox(8, bJavna, bBez, bSve);

        Label upozorenje = new Label("⚠ Lista s imenima sadrži lične podatke — nije za javnu objavu.");
        upozorenje.setWrapText(true);
        upozorenje.setStyle("-fx-text-fill: #b86e00;");
        upozorenje.visibleProperty().bind(kolone.get(RangKolona.IME).selectedProperty());
        upozorenje.managedProperty().bind(upozorenje.visibleProperty());

        // ---------- samo za PDF ----------
        ToggleGroup vrsta = new ToggleGroup();
        RadioButton rbPreliminarna = new RadioButton("Preliminarna");
        RadioButton rbKonacna = new RadioButton("Konačna");
        rbPreliminarna.setToggleGroup(vrsta);
        rbKonacna.setToggleGroup(vrsta);
        rbPreliminarna.setSelected(true);

        DatePicker dpOd = new DatePicker(LocalDate.now().plusDays(4));
        DatePicker dpDo = new DatePicker(LocalDate.now().plusDays(12));
        GridPane datumi = new GridPane();
        datumi.setHgap(8);
        datumi.setVgap(6);
        datumi.addRow(0, new Label("Useljenje od:"), dpOd);
        datumi.addRow(1, new Label("Useljenje do:"), dpDo);

        VBox pdfDio = new VBox(8,
                naslov("Vrsta liste (samo PDF)"), new HBox(16, rbPreliminarna, rbKonacna),
                naslov("Rok za useljenje (samo PDF)"), datumi);
        pdfDio.disableProperty().bind(rbExcel.selectedProperty());

        VBox sadrzaj = new VBox(8,
                naslov("Format"), rbPdf, rbExcel,
                new Separator(),
                naslov("Liste"), chkBrucosi, chkSaUslovom, chkCekanje, chkObjedinjeno, chkSamoPrimljeni,
                new Separator(),
                naslov("Kolone"), brzi, grid, upozorenje,
                new Separator(),
                pdfDio);
        sadrzaj.setPadding(new Insets(10));

        ScrollPane skrol = new ScrollPane(sadrzaj);
        skrol.setFitToWidth(true);
        skrol.setPrefViewportHeight(560);
        skrol.setPrefViewportWidth(460);
        getDialogPane().setContent(skrol);

        ButtonType izvezi = new ButtonType("Izvezi", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(izvezi, ButtonType.CANCEL);

        // bar jedna lista, bar jedna kolona, i datumi kod PDF-a
        BooleanBinding nijednaKolona = Bindings.createBooleanBinding(
                () -> kolone.values().stream().noneMatch(CheckBox::isSelected),
                kolone.values().stream().map(CheckBox::selectedProperty).toArray(Observable[]::new));

        Node btnIzvezi = getDialogPane().lookupButton(izvezi);
        btnIzvezi.disableProperty().bind(
                chkBrucosi.selectedProperty()
                        .or(chkSaUslovom.selectedProperty())
                        .or(chkCekanje.selectedProperty())
                        .or(chkObjedinjeno.selectedProperty())
                        .not()
                        .or(nijednaKolona)
                        .or(rbPdf.selectedProperty().and(
                                dpOd.valueProperty().isNull().or(dpDo.valueProperty().isNull()))));

        setResultConverter(bt -> {
            if (bt != izvezi) return null;

            Set<KategorijaRanga> liste = EnumSet.noneOf(KategorijaRanga.class);
            if (chkBrucosi.isSelected()) liste.add(KategorijaRanga.BRUCOS);
            if (chkSaUslovom.isSelected()) liste.add(KategorijaRanga.SA_USLOVOM);
            if (chkCekanje.isSelected()) liste.add(KategorijaRanga.LISTA_CEKANJA);

            List<RangKolona> odabrane = kolone.entrySet().stream()
                    .filter(e -> e.getValue().isSelected())
                    .map(Map.Entry::getKey)
                    .toList();

            LocalDate od = dpOd.getValue();
            LocalDate doDatuma = dpDo.getValue() == null || od == null || !dpDo.getValue().isBefore(od)
                    ? dpDo.getValue() : od;

            return new ExportOpcije(rbExcel.isSelected() ? "EXCEL" : "PDF",
                    liste, chkObjedinjeno.isSelected(), odabrane,
                    rbKonacna.isSelected(), chkSamoPrimljeni.isSelected(), od, doDatuma);
        });
    }

    private void oznaci(RangKolona... odabrane) {
        Set<RangKolona> s = Set.of(odabrane);
        kolone.forEach((k, c) -> c.setSelected(s.contains(k)));
    }

    private Label naslov(String tekst) {
        Label l = new Label(tekst);
        l.setStyle("-fx-font-weight: bold;");
        return l;
    }
}