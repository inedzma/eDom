package controllers;

import api.UplateApi;
import service.export.UplateExportService;
import ba.unze.edom.common.dto.uplate.UplateTabelaDTO;
import ba.unze.edom.common.dto.uplate.UplateTabelaDTO.Celija;
import ba.unze.edom.common.dto.uplate.UplateTabelaDTO.Red;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Uplate za smještaj: red po studentu, kolona po mjesecu (X–VII).
 * Klik na mjesec: evidentiranje uplate, ispravka datuma ili poništavanje.
 */
public class UplateController {

    private static final String[] RIMSKI = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"};
    private static final DateTimeFormatter KRATKO = DateTimeFormatter.ofPattern("dd.MM.");
    private static final DateTimeFormatter PUNO = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

    @FXML private Label lblPodnaslov;
    @FXML private ComboBox<Integer> cmbGodina;
    @FXML private HBox boxFilteri;
    @FXML private TextField txtPretraga;
    @FXML private MenuButton mnuSortiraj;
    @FXML private RadioMenuItem sortPrezime;
    @FXML private RadioMenuItem sortSoba;
    @FXML private RadioMenuItem sortDug;
    @FXML private TableView<Red> tabela;

    private UplateTabelaDTO podaci;

    // filteri po stanju
    private final ToggleGroup grupaFiltera = new ToggleGroup();
    private ToggleButton chipSvi, chipDuguju, chipUredni;

    @FXML
    public void initialize() {
        int tekuca = tekucaAkademskaGodina();
        cmbGodina.setItems(FXCollections.observableArrayList(tekuca, tekuca - 1, tekuca - 2));
        cmbGodina.setConverter(new StringConverter<>() {
            @Override public String toString(Integer g) { return g == null ? "" : g + "/" + (g + 1); }
            @Override public Integer fromString(String s) { return null; }
        });
        cmbGodina.setValue(tekuca);
        cmbGodina.valueProperty().addListener((o, a, b) -> ucitaj());

        chipSvi = chip("Svi", null);
        chipDuguju = chip("Duguju", "st-odbijeno");
        chipUredni = chip("Uredni", "st-odobreno");
        chipSvi.setSelected(true);
        boxFilteri.getChildren().addAll(chipSvi, chipDuguju, chipUredni);
        grupaFiltera.selectedToggleProperty().addListener((o, a, b) -> {
            if (b == null) a.setSelected(true);   // jedan filter je uvijek izabran
            prikazi();
        });

        ToggleGroup sort = new ToggleGroup();
        for (RadioMenuItem i : List.of(sortPrezime, sortSoba, sortDug)) {
            i.setToggleGroup(sort);
            i.setOnAction(e -> { mnuSortiraj.setText("Sortiraj: " + i.getText()); prikazi(); });
        }
        txtPretraga.textProperty().addListener((o, a, b) -> prikazi());

        tabela.setPlaceholder(new Label("Nema studenata koji su ove godine useljeni."));

        // desni klik na red: podsjetnik samo tom studentu
        tabela.setRowFactory(t -> {
            TableRow<Red> red = new TableRow<>();
            MenuItem podsjeti = new MenuItem("Pošalji podsjetnik ovom studentu");
            podsjeti.setOnAction(e -> {
                if (red.getItem() != null) posaljiPodsjetnik(List.of(red.getItem()));
            });
            red.itemProperty().addListener((o, a, r) ->
                    red.setContextMenu(r != null && r.dug() > 0 ? new ContextMenu(podsjeti) : null));
            return red;
        });
        ucitaj();
    }

    @FXML
    private void onOsvjezi() {
        ucitaj();
    }

    // ---------- podsjetnik i izvoz ----------

    /** Podsjetnik svim dužnicima koji su trenutno u tabeli (poštuje filter i pretragu). */
    @FXML
    private void onPodsjetnik() {
        List<Red> duznici = tabela.getItems().stream().filter(r -> r.dug() > 0).toList();
        if (duznici.isEmpty()) {
            info("U prikazanoj listi nema studenata koji duguju.");
            return;
        }
        posaljiPodsjetnik(duznici);
    }

    private void posaljiPodsjetnik(List<Red> kome) {
        TextArea napomena = new TextArea();
        napomena.getStyleClass().add("pr-textarea");
        napomena.setPromptText("Dodatna napomena (nije obavezna), npr. rok za uplatu ili broj računa.");
        napomena.setPrefRowCount(4);
        napomena.setWrapText(true);

        Label opis = new Label(kome.size() == 1
                ? "Podsjetnik dobija " + kome.get(0).student() + " (" + kome.get(0).dug() + " mj.)."
                : "Podsjetnik dobija " + kome.size() + " studenata koji duguju.");
        opis.getStyleClass().add("pr-vrijednost");
        Label objasnjenje = new Label("U e-mailu su navedeni neplaćeni mjeseci za svakog studenta.");
        objasnjenje.getStyleClass().add("pr-sitno");

        ButtonType posalji = new ButtonType("Pošalji", ButtonBar.ButtonData.OK_DONE);
        Dialog<ButtonType> d = dijalog("Podsjetnik o uplati", "Slanje podsjetnika e-mailom",
                new VBox(10, opis, objasnjenje, napomena), posalji);
        if (d.showAndWait().orElse(ButtonType.CANCEL) != posalji) return;

        try {
            Map<String, Object> r = UplateApi.podsjetnik(podaci.akademskaGodina(),
                    kome.stream().map(Red::idStudent).toList(), napomena.getText());
            Object poslano = r.get("poslano");
            List<?> nije = r.get("nijePoslano") instanceof List<?> l ? l : List.of();

            StringBuilder t = new StringBuilder("Poslano: " + poslano + ".");
            if (!nije.isEmpty()) {
                t.append("\n\nNije poslano:");
                nije.forEach(x -> t.append("\n  • ").append(x));
            }
            info(t.toString());
        } catch (RuntimeException e) {
            greska("Podsjetnik nije poslan: " + e.getMessage());
        }
    }

    @FXML
    private void onExcel() {
        File f = izaberiFajl("Excel", "*.xlsx", "xlsx");
        if (f == null) return;
        try {
            new UplateExportService().excel(podaci, tabela.getItems(), opisPrikaza(), f);
            info("Izvještaj je sačuvan:\n" + f.getAbsolutePath());
        } catch (RuntimeException e) {
            greska(e.getMessage());
        }
    }

    @FXML
    private void onPdf() {
        File f = izaberiFajl("PDF", "*.pdf", "pdf");
        if (f == null) return;
        try {
            new UplateExportService().pdf(podaci, tabela.getItems(), opisPrikaza(), f);
            info("Izvještaj je sačuvan:\n" + f.getAbsolutePath());
        } catch (RuntimeException e) {
            greska(e.getMessage());
        }
    }

    private File izaberiFajl(String vrsta, String filter, String ekstenzija) {
        if (podaci == null) return null;
        FileChooser fc = new FileChooser();
        fc.setTitle("Sačuvaj izvještaj o uplatama");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(vrsta, filter));
        int g = podaci.akademskaGodina();
        fc.setInitialFileName("uplate-" + g + "-" + (g + 1)
                + (chipDuguju.isSelected() ? "-duznici" : "") + "." + ekstenzija);
        return fc.showSaveDialog(tabela.getScene().getWindow());
    }

    /** Kratak opis onoga što je na ekranu, za zaglavlje izvještaja. */
    private String opisPrikaza() {
        String s = chipDuguju.isSelected() ? "Studenti koji duguju"
                : chipUredni.isSelected() ? "Studenti bez dugovanja"
                : "Svi studenti";
        String trazi = txtPretraga.getText() == null ? "" : txtPretraga.getText().trim();
        return trazi.isEmpty() ? s : s + " (pretraga: " + trazi + ")";
    }

    // ---------- učitavanje i prikaz ----------

    private void ucitaj() {
        try {
            podaci = UplateApi.tabela(cmbGodina.getValue());
            napraviKolone();
            prikazi();
        } catch (RuntimeException e) {
            greska("Uplate se ne mogu učitati: " + e.getMessage());
        }
    }

    private void prikazi() {
        if (podaci == null) return;
        List<Red> svi = podaci.redovi();

        // brojevi na filterima i u podnaslovu
        long duguju = svi.stream().filter(r -> r.dug() > 0).count();
        int mjeseci = svi.stream().mapToInt(Red::dug).sum();
        chipSvi.setText("Svi   " + svi.size());
        chipDuguju.setText("Duguju   " + duguju);
        chipUredni.setText("Uredni   " + (svi.size() - duguju));
        lblPodnaslov.setText("Akademska godina " + podaci.akademskaGodina() + "/" + (podaci.akademskaGodina() + 1)
                + "  ·  " + svi.size() + " studenata"
                + (duguju > 0 ? "  ·  " + duguju + " duguje (ukupno " + mjeseci + " mj.)" : "  ·  svi su uredni"));

        String trazi = txtPretraga.getText() == null ? "" : txtPretraga.getText().trim().toLowerCase(Locale.ROOT);
        Predicate<Red> poStanju = chipDuguju.isSelected() ? r -> r.dug() > 0
                : chipUredni.isSelected() ? r -> r.dug() == 0
                : r -> true;

        List<Red> redovi = svi.stream()
                .filter(poStanju)
                .filter(r -> trazi.isEmpty() || r.student().toLowerCase(Locale.ROOT).contains(trazi)
                        || (r.soba() != null && String.valueOf(r.soba()).equals(trazi)))
                .sorted(redoslijed())
                .toList();
        tabela.setItems(FXCollections.observableArrayList(redovi));
    }

    private Comparator<Red> redoslijed() {
        Comparator<Red> poImenu = Comparator.comparing(r -> r.student().toLowerCase(Locale.ROOT));
        if (sortSoba.isSelected()) {
            return Comparator.comparing(Red::soba, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(poImenu);
        }
        if (sortDug.isSelected()) {
            return Comparator.comparingInt(Red::dug).reversed().thenComparing(poImenu);
        }
        return poImenu;
    }

    private void napraviKolone() {
        tabela.getColumns().clear();

        // student: ime i ispod soba, kao na ekranu Prijave
        TableColumn<Red, Red> student = new TableColumn<>("STUDENT");
        student.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        student.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Red r, boolean prazno) {
                super.updateItem(r, prazno);
                if (prazno || r == null) { setGraphic(null); return; }
                Label ime = new Label(r.student());
                ime.getStyleClass().add("pr-ime");
                Label soba = new Label(r.soba() != null ? "Soba " + r.soba() : "Iseljen");
                soba.getStyleClass().add("pr-sitno");
                setGraphic(new VBox(2, ime, soba));
                setText(null);
            }
        });
        student.setPrefWidth(240);
        student.setSortable(false);
        tabela.getColumns().add(student);

        for (int i = 0; i < podaci.mjeseci().size(); i++) {
            final int idx = i;
            YearMonth m = YearMonth.parse(podaci.mjeseci().get(i));
            TableColumn<Red, Celija> kol = new TableColumn<>(RIMSKI[m.getMonthValue()]);
            kol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().celije().get(idx)));
            kol.setCellFactory(c -> new MjesecCelija());
            kol.setPrefWidth(96);
            kol.setMinWidth(92);
            kol.setSortable(false);
            tabela.getColumns().add(kol);
        }

        TableColumn<Red, Integer> dug = new TableColumn<>("DUG");
        dug.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().dug()));
        dug.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer d, boolean prazno) {
                super.updateItem(d, prazno);
                if (prazno || d == null) { setGraphic(null); return; }
                Label l = new Label(d == 0 ? "Uredno" : d + " mj.");
                l.getStyleClass().addAll("pr-status", d == 0 ? "st-odobreno" : "st-odbijeno");
                setGraphic(l);
                setText(null);
            }
        });
        dug.setPrefWidth(100);
        dug.setSortable(false);
        tabela.getColumns().add(dug);
    }

    /** Ćelija jednog mjeseca: pilula po stanju, klik otvara evidenciju. */
    private class MjesecCelija extends TableCell<Red, Celija> {

        MjesecCelija() {
            getStyleClass().add("up-mjesec");
            setOnMouseClicked(e -> {
                if (getItem() != null && getTableRow() != null && getTableRow().getItem() != null) {
                    klik(getTableRow().getItem(), getItem());
                }
            });
        }

        @Override
        protected void updateItem(Celija c, boolean prazno) {
            super.updateItem(c, prazno);
            setText(null);
            if (prazno || c == null) { setGraphic(null); setTooltip(null); return; }

            Label l = new Label();
            l.getStyleClass().add("up-celija");
            switch (c.stanje()) {
                case UplateTabelaDTO.PLACENO -> {
                    l.setText("✔ " + c.datum().format(KRATKO));
                    l.getStyleClass().add("up-placeno");
                    setTooltip(new Tooltip("Plaćeno " + c.datum().format(PUNO)));
                }
                case UplateTabelaDTO.DUG -> {
                    l.setText("✖");
                    l.getStyleClass().add("up-dug");
                    setTooltip(new Tooltip("Nije plaćeno"));
                }
                case UplateTabelaDTO.CEKA -> {
                    l.setText("•");
                    l.getStyleClass().add("up-ceka");
                    setTooltip(new Tooltip("Tekući ili budući mjesec"));
                }
                default -> {
                    l.setText("–");
                    l.getStyleClass().add("up-van");
                    setTooltip(new Tooltip("Student tada nije bio u domu"));
                }
            }
            setGraphic(l);
        }
    }

    // ---------- evidencija ----------

    private void klik(Red r, Celija c) {
        YearMonth m = YearMonth.parse(c.mjesec());
        String mjesec = RIMSKI[m.getMonthValue()] + " " + m.getYear();
        boolean placeno = UplateTabelaDTO.PLACENO.equals(c.stanje());

        DatePicker datum = new DatePicker(placeno ? c.datum() : LocalDate.now());
        datum.getStyleClass().add("pr-polje");
        datum.setMaxWidth(Double.MAX_VALUE);

        Label ko = new Label(r.student() + "  ·  " + mjesec);
        ko.getStyleClass().add("pr-vrijednost");
        VBox sadrzaj = new VBox(10, ko);
        if (UplateTabelaDTO.NIJE_U_DOMU.equals(c.stanje())) {
            Label nap = new Label("Student tada nije bio u domu.");
            nap.getStyleClass().add("pr-upozorenje");
            nap.setWrapText(true);
            sadrzaj.getChildren().add(nap);
        }
        Label nazivDatuma = new Label("Datum uplate");
        nazivDatuma.getStyleClass().add("pr-polje-naslov");
        sadrzaj.getChildren().addAll(nazivDatuma, datum);

        ButtonType sacuvaj = new ButtonType(placeno ? "Sačuvaj datum" : "Evidentiraj uplatu", ButtonBar.ButtonData.OK_DONE);
        ButtonType ponisti = new ButtonType("Poništi uplatu", ButtonBar.ButtonData.LEFT);

        Dialog<ButtonType> d = placeno
                ? dijalog("Uplata za smještaj", "Uplata je evidentirana", sadrzaj, sacuvaj, ponisti)
                : dijalog("Uplata za smještaj", "Evidentiranje uplate", sadrzaj, sacuvaj);

        Optional<ButtonType> izbor = d.showAndWait();
        if (izbor.isEmpty() || izbor.get() == ButtonType.CANCEL) return;

        try {
            if (izbor.get() == ponisti) {
                UplateApi.ponisti(r.idStudent(), c.mjesec());
            } else {
                if (datum.getValue() == null) { greska("Odaberite datum uplate."); return; }
                UplateApi.evidentiraj(r.idStudent(), c.mjesec(), datum.getValue());
            }
            ucitaj();
        } catch (RuntimeException e) {
            greska(e.getMessage());
        }
    }

    // ---------- pomoćne ----------

    /** Pilula za filter, s tačkom u boji statusa (kao na ekranu Prijave). */
    private ToggleButton chip(String tekst, String boja) {
        ToggleButton t = new ToggleButton(tekst);
        t.getStyleClass().add("pr-chip");
        t.setToggleGroup(grupaFiltera);
        if (boja != null) {
            Circle tacka = new Circle(4);
            tacka.getStyleClass().addAll("pr-tacka", boja);
            t.setGraphic(tacka);
        }
        return t;
    }

    /** Od oktobra je nova akademska godina. */
    private static int tekucaAkademskaGodina() {
        YearMonth sada = YearMonth.now();
        return sada.getMonthValue() >= 10 ? sada.getYear() : sada.getYear() - 1;
    }

    /** Dijalog u istom izgledu kao ekran (prijave.css i uplate.css); Odustani se dodaje sam. */
    private Dialog<ButtonType> dijalog(String naslov, String zaglavlje, javafx.scene.Node sadrzaj, ButtonType... dugmad) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle(naslov);
        d.setHeaderText(zaglavlje);
        d.getDialogPane().setContent(sadrzaj);
        d.getDialogPane().getStylesheets().addAll(tabela.getScene().getStylesheets());
        for (String css : List.of("/styles/prijave.css", "/styles/uplate.css")) {
            var url = getClass().getResource(css);
            if (url != null) d.getDialogPane().getStylesheets().add(url.toExternalForm());
        }
        d.getDialogPane().getStyleClass().add("pr-root");
        d.getDialogPane().getButtonTypes().addAll(dugmad);
        d.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        return d;
    }

    private void info(String poruka) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, poruka);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void greska(String poruka) {
        Alert a = new Alert(Alert.AlertType.ERROR, poruka);
        a.setHeaderText(null);
        a.showAndWait();
    }
}