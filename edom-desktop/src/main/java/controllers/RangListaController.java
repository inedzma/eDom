package controllers;

import api.ApiException;
import api.RangListaApi;
import ba.unze.edom.common.dto.KategorijaRanga;
import ba.unze.edom.common.dto.RangListaAdminDTO;
import ba.unze.edom.common.dto.RangRedDTO;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import service.export.ExportOpcije;
import service.export.RangKolona;
import service.export.RangListaExcelExportService;
import service.export.RangListaHtmlExportService;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/** Rang lista: statistika, objava na webu, grupe, pronalaženje, dokaz o uslovu, izvoz. */
public class RangListaController {

    @FXML private Label lblGodina;
    @FXML private Label lblSazetak;
    @FXML private Label lblObjava;
    @FXML private HBox boxStatistika;
    @FXML private ToggleButton btnBrucosi;
    @FXML private ToggleButton btnSaUslovom;
    @FXML private ToggleButton btnCekanje;
    @FXML private ToggleButton btnSve;
    @FXML private CheckBox chkPopuniBrucosima;
    @FXML private MenuButton btnKolone;
    @FXML private Button btnObjavi;
    @FXML private TextField txtTrazi;
    @FXML private TableView<RangRedDTO> tblRangLista;

    /** Kolone pravi kontroler — redoslijed = redoslijed u meniju i u PDF-u. */
    private final Map<TableColumn<RangRedDTO, RangRedDTO>, RangKolona> kolone = new LinkedHashMap<>();
    private final TableColumn<RangRedDTO, RangRedDTO> colAkcije = new TableColumn<>("");

    private CheckMenuItem stavkaKategorija;
    private RangListaAdminDTO lista;

    @FXML
    public void initialize() {
        provjeriFxml();
        napraviKolone();
        initKolone();
        initAkcije();
        initRedove();
        initMeniKolona();
        initGrupe();
        initPretraga();
        tblRangLista.setPlaceholder(new Label("Nema studenata u ovoj grupi."));
        chkPopuniBrucosima.selectedProperty().addListener((obs, o, n) -> ucitaj());
        ucitaj();
    }

    /** Ako se učita stari FXML, reci tačno šta fali. */
    private void provjeriFxml() {
        Map<String, Object> potrebno = new LinkedHashMap<>();
        potrebno.put("lblGodina", lblGodina);
        potrebno.put("lblSazetak", lblSazetak);
        potrebno.put("lblObjava", lblObjava);
        potrebno.put("boxStatistika", boxStatistika);
        potrebno.put("btnBrucosi", btnBrucosi);
        potrebno.put("btnSaUslovom", btnSaUslovom);
        potrebno.put("btnCekanje", btnCekanje);
        potrebno.put("btnSve", btnSve);
        potrebno.put("chkPopuniBrucosima", chkPopuniBrucosima);
        potrebno.put("btnKolone", btnKolone);
        potrebno.put("txtTrazi", txtTrazi);
        potrebno.put("tblRangLista", tblRangLista);

        List<String> fale = potrebno.entrySet().stream()
                .filter(e -> e.getValue() == null).map(Map.Entry::getKey).toList();
        if (!fale.isEmpty()) {
            throw new IllegalStateException("rang-lista.fxml nema fx:id " + fale
                    + " — učitava se stari FXML (zamijeni ga u src/main/resources/views, pa Rebuild).");
        }
    }

    // =====================================================================
    //  KOLONE
    // =====================================================================

    private void napraviKolone() {
        for (RangKolona k : RangKolona.values()) {
            TableColumn<RangRedDTO, RangRedDTO> col = new TableColumn<>(k.naslov().toUpperCase());
            col.setPrefWidth(sirina(k));
            if (k.centrirano()) col.setStyle("-fx-alignment: CENTER;");
            if (k == RangKolona.KATEGORIJA) col.setVisible(false);   // uključuje se u prikazu "Sve"
            kolone.put(col, k);
        }
        colAkcije.setPrefWidth(230);
        tblRangLista.getColumns().setAll(kolone.keySet());
        tblRangLista.getColumns().add(colAkcije);
    }

    private double sirina(RangKolona k) {
        return switch (k) {
            case REDNI_BROJ -> 55;
            case SIFRA -> 115;
            case GODINA -> 75;
            case IME -> 240;
            case KATEGORIJA, SOCIJALNI -> 115;
            case USPJEH, BRANIOCI, UDALJENOST, UKUPNO -> 90;
            case STATUS -> 170;
        };
    }

    private void initKolone() {
        kolone.forEach((col, k) -> {
            col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
            col.setSortable(false);   // redoslijed određuje rang, ne klik na kolonu
            col.setCellFactory(tc -> new TableCell<>() {
                private final Label oznaka = new Label();

                @Override
                protected void updateItem(RangRedDTO r, boolean empty) {
                    super.updateItem(r, empty);
                    setTooltip(null);
                    setGraphic(null);
                    getStyleClass().remove("pr-bodovi");
                    if (empty || r == null) {
                        setText(null);
                        return;
                    }
                    String tekst = k.vrijednost(r, getIndex() + 1);

                    switch (k) {
                        case SIFRA -> {
                            setText(null);
                            oznaka.setText(tekst);
                            oznaka.getStyleClass().setAll("label", "pr-sifra");
                            setGraphic(oznaka);
                        }
                        case STATUS -> {
                            setText(null);
                            oznaka.setText(r.greska() != null ? "⚠ Neispravni podaci" : tekst);
                            oznaka.getStyleClass().setAll("label", "pr-status", statusKlasa(r));
                            setGraphic(oznaka);
                            if (r.greska() != null) setTooltip(new Tooltip(r.greska()));
                        }
                        case KATEGORIJA -> {
                            setText(null);
                            oznaka.setText(tekst);
                            oznaka.getStyleClass().setAll("label", "pr-status", kategorijaKlasa(r.kategorija()));
                            setGraphic(oznaka);
                        }
                        case UKUPNO -> {
                            setText(tekst);
                            getStyleClass().add("pr-bodovi");
                            if (r.greska() != null) setTooltip(new Tooltip(r.greska()));
                        }
                        default -> setText(tekst);
                    }
                }
            });
        });
        tblRangLista.getSelectionModel().setCellSelectionEnabled(false);
    }

    private static String statusKlasa(RangRedDTO r) {
        if (r.greska() != null) return "st-odbijeno";
        if (r.kategorija() == KategorijaRanga.LISTA_CEKANJA) return "st-cekanje";
        return r.primljen() ? "st-odobreno" : "st-iseljen";
    }

    private static String kategorijaKlasa(KategorijaRanga k) {
        if (k == null) return "st-ostalo";
        return switch (k) {
            case BRUCOS -> "st-pregled";
            case SA_USLOVOM -> "st-odobreno";
            case LISTA_CEKANJA -> "st-cekanje";
        };
    }

    private void initAkcije() {
        colAkcije.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colAkcije.setSortable(false);
        colAkcije.setCellFactory(tc -> new TableCell<>() {
            private final Button btnDetalji = new Button("Detalji");
            private final Button btnDokaz = new Button("Dokaz ✓");
            private final Button btnPonisti = new Button("Poništi dokaz");
            private final HBox box = new HBox(6);
            {
                btnDetalji.getStyleClass().add("pr-mali-btn");
                btnDokaz.getStyleClass().addAll("pr-mali-btn", "pr-mali-btn--zeleno");
                btnPonisti.getStyleClass().addAll("pr-mali-btn", "pr-mali-btn--crveno");
                btnDokaz.setTooltip(new Tooltip("Student je donio dokaz o ispunjavanju uslova za upis"));
                btnDetalji.setOnAction(e -> { if (getItem() != null) otvoriDetalje(getItem().idPrijava()); });
                btnDokaz.setOnAction(e -> { if (getItem() != null) evidentirajDokaz(getItem()); });
                btnPonisti.setOnAction(e -> { if (getItem() != null) ponistiDokaz(getItem()); });
            }

            @Override
            protected void updateItem(RangRedDTO r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) {
                    setGraphic(null);
                    return;
                }
                box.getChildren().setAll(btnDetalji);
                if (r.kategorija() == KategorijaRanga.LISTA_CEKANJA) {
                    box.getChildren().add(btnDokaz);
                } else if (r.kategorija() == KategorijaRanga.SA_USLOVOM && r.dokazNaknadno()) {
                    box.getChildren().add(btnPonisti);
                }
                setGraphic(box);
            }
        });
    }

    /** Boje redova, crvena crta ispod posljednjeg primljenog, dvoklik → detalji. */
    private void initRedove() {
        tblRangLista.setRowFactory(tv -> {
            TableRow<RangRedDTO> row = new TableRow<>() {
                @Override
                protected void updateItem(RangRedDTO r, boolean empty) {
                    super.updateItem(r, empty);
                    getStyleClass().removeAll("red-ispod-crte", "red-cekanje", "red-greska", "red-crta");
                    if (empty || r == null) return;

                    if (r.greska() != null) getStyleClass().add("red-greska");
                    else if (r.kategorija() == KategorijaRanga.LISTA_CEKANJA) getStyleClass().add("red-cekanje");
                    else if (!r.primljen()) getStyleClass().add("red-ispod-crte");

                    if (r.primljen() && !btnSve.isSelected() && posljednjiPrimljen(getIndex())) {
                        getStyleClass().add("red-crta");
                    }
                }
            };
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) otvoriDetalje(row.getItem().idPrijava());
            });
            return row;
        });
    }

    private boolean posljednjiPrimljen(int i) {
        ObservableList<RangRedDTO> items = tblRangLista.getItems();
        return i + 1 >= items.size() || !items.get(i + 1).primljen();
    }

    private void initMeniKolona() {
        btnKolone.getItems().clear();
        kolone.forEach((col, k) -> {
            CheckMenuItem item = new CheckMenuItem(k.naslov());
            item.setSelected(col.isVisible());
            col.visibleProperty().bindBidirectional(item.selectedProperty());
            if (k == RangKolona.KATEGORIJA) stavkaKategorija = item;
            btnKolone.getItems().add(item);
        });
    }

    private void initGrupe() {
        ToggleGroup grupa = new ToggleGroup();
        for (ToggleButton b : List.of(btnBrucosi, btnSaUslovom, btnCekanje, btnSve)) b.setToggleGroup(grupa);
        btnBrucosi.setSelected(true);

        grupa.selectedToggleProperty().addListener((obs, stari, novi) -> {
            if (novi == null) stari.setSelected(true);   // jedna grupa je uvijek odabrana
            else prikaziGrupu();
        });
    }

    // =====================================================================
    //  PRONALAŽENJE (ne filtrira, da redni brojevi i crta ostanu tačni)
    // =====================================================================

    private void initPretraga() {
        txtTrazi.textProperty().addListener((o, a, n) -> pronadji(0));
        txtTrazi.setOnAction(e -> pronadji(tblRangLista.getSelectionModel().getSelectedIndex() + 1));
    }

    private void pronadji(int od) {
        String q = txtTrazi.getText() == null ? "" : txtTrazi.getText().trim().toLowerCase();
        List<RangRedDTO> items = tblRangLista.getItems();
        if (q.isEmpty() || items.isEmpty()) {
            tblRangLista.getSelectionModel().clearSelection();
            return;
        }
        for (int k = 0; k < items.size(); k++) {
            int i = (Math.max(0, od) + k) % items.size();
            RangRedDTO r = items.get(i);
            if ((r.sifra() != null && r.sifra().toLowerCase().contains(q))
                    || RangKolona.punoIme(r).toLowerCase().contains(q)) {
                tblRangLista.getSelectionModel().clearAndSelect(i);
                tblRangLista.scrollTo(Math.max(0, i - 3));
                return;
            }
        }
        tblRangLista.getSelectionModel().clearSelection();
    }

    // =====================================================================
    //  UČITAVANJE
    // =====================================================================

    private void ucitaj() {
        boolean popuni = chkPopuniBrucosima.isSelected();
        uPozadini(() -> RangListaApi.dohvati(null, popuni), l -> {
            lista = l;
            lblGodina.setText(l.akademskaGodina() != null
                    ? "Akademska godina " + l.akademskaGodina() + "   ·   " + l.kapacitet() + " mjesta"
                    : "Nema prijava");

            int b = l.brucosi().size(), s = l.saUslovom().size(), c = l.listaCekanja().size();
            btnBrucosi.setText("Brucoši   " + b);
            btnSaUslovom.setText("S uslovom   " + s);
            btnCekanje.setText("Lista čekanja   " + c);
            btnSve.setText("Sve   " + (b + s + c));

            prikaziStatistiku(l);
            prikaziGrupu();
            ucitajObjavu();
        }, "Greška pri učitavanju rang liste.");
    }

    private void prikaziStatistiku(RangListaAdminDTO l) {
        boxStatistika.getChildren().setAll(
                stat("BRUCOŠI", l.primljenoBrucosa(), "od kvote " + l.kvotaBrucosi(),
                        udio(l.primljenoBrucosa(), l.kvotaBrucosi())),
                stat("VIŠE GODINE", l.primljenoVisih(), "od kvote " + l.kvotaVisi(),
                        udio(l.primljenoVisih(), l.kvotaVisi())),
                stat("SLOBODNO", l.slobodno(), "od " + l.kapacitet() + " mjesta", -1),
                stat("LISTA ČEKANJA", l.listaCekanja().size(), "čeka dokaz o uslovu", -1));
    }

    /** Pločica u zaglavlju: naslov, veliki broj, opis i (ako ima kvotu) traka popunjenosti. */
    private VBox stat(String naslov, long broj, String opis, double popunjeno) {
        Label n = new Label(naslov);
        n.getStyleClass().add("pr-hero-stat-naslov");
        Label b = new Label(String.valueOf(broj));
        b.getStyleClass().add("pr-hero-stat-broj");
        Label o = new Label(opis);
        o.getStyleClass().add("pr-hero-stat-opis");

        VBox v = new VBox(2, n, b, o);
        if (popunjeno >= 0) {
            ProgressBar traka = new ProgressBar(popunjeno);
            traka.getStyleClass().add("pr-hero-traka");
            traka.setMaxWidth(Double.MAX_VALUE);
            VBox.setMargin(traka, new javafx.geometry.Insets(8, 0, 0, 0));
            v.getChildren().add(traka);
        }
        v.getStyleClass().add("pr-hero-stat");
        v.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(v, Priority.ALWAYS);
        return v;
    }

    private static double udio(long broj, long od) {
        return od <= 0 ? 0 : Math.min(1.0, (double) broj / od);
    }

    private void prikaziGrupu() {
        if (lista == null) return;
        boolean sve = btnSve.isSelected();
        List<RangRedDTO> redovi =
                btnBrucosi.isSelected() ? lista.brucosi()
                        : btnSaUslovom.isSelected() ? lista.saUslovom()
                        : btnCekanje.isSelected() ? lista.listaCekanja()
                        : RangListaHtmlExportService.objedinjeno(lista);

        stavkaKategorija.setSelected(sve);
        tblRangLista.setItems(FXCollections.observableArrayList(redovi));
        tblRangLista.refresh();

        long neispravnih = redovi.stream().filter(r -> r.greska() != null).count();
        lblSazetak.setText(redovi.size() + " u prikazu"
                + (neispravnih > 0 ? "   ·   ⚠ " + neispravnih + " s neispravnim podacima" : ""));
        if (!txtTrazi.getText().isBlank()) pronadji(0);
    }

    @FXML
    private void onRefresh() {
        ucitaj();
    }

    // =====================================================================
    //  OBJAVA NA WEBU
    // =====================================================================

    private void ucitajObjavu() {
        uPozadini(RangListaApi::zadnjaObjava, o -> {
            boolean ima = o.idObjava() != null;
            lblObjava.setText(ima
                    ? "✓  Objavljeno na webu   ·   " + nazivVrste(o.vrsta()).toLowerCase() + " lista   ·   "
                    + o.objavljeno() + "   ·   " + o.brojStudenata() + " studenata"
                    : "○  Još nije objavljeno na webu");
            lblObjava.setTooltip(new Tooltip(ima
                    ? "Objavio: " + o.objavio() + "\nIzmjene poslije objave nisu na webu dok ne objavite ponovo."
                    : "Studenti na webu vide poruku da rang lista još nije objavljena."));
            lblObjava.getStyleClass().remove("pr-hero-objava--da");
            if (ima) lblObjava.getStyleClass().add("pr-hero-objava--da");
        }, "Greška pri provjeri objave.");
    }
    @FXML
    private void objavi() {
        if (lista == null) return;

        ComboBox<String> cbVrsta = new ComboBox<>(FXCollections.observableArrayList("Preliminarna", "Konačna"));
        cbVrsta.getSelectionModel().selectFirst();
        DatePicker dpOd = new DatePicker(LocalDate.now().plusDays(1));
        DatePicker dpDo = new DatePicker(LocalDate.now().plusDays(9));

        Label napomena = new Label("Termin useljenja prikazuje se na web stranici rang liste.\n"
                + "Ako termin još nije poznat, obrišite oba datuma.");
        napomena.setWrapText(true);

        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        g.addRow(0, new Label("Vrsta liste:"), cbVrsta);
        g.addRow(1, new Label("Useljenje od:"), dpOd);
        g.addRow(2, new Label("Useljenje do:"), dpDo);
        g.add(napomena, 0, 3, 2, 1);

        Dialog<ButtonType> dijalog = new Dialog<>();
        dijalog.setTitle("Objava rang liste");
        dijalog.setHeaderText("Koju listu objavljujete na webu?");
        dijalog.getDialogPane().setContent(g);
        dijalog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (dijalog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        LocalDate od = dpOd.getValue();
        LocalDate doDatuma = dpDo.getValue();
        if ((od == null) != (doDatuma == null) || (od != null && od.isAfter(doDatuma))) {
            new Alert(Alert.AlertType.WARNING,
                    "Unesite ispravan termin useljenja (od – do) ili obrišite oba datuma.").showAndWait();
            return;
        }

        String naziv = cbVrsta.getValue();
        String vrsta = "Konačna".equals(naziv) ? "KONACNA" : "PRELIMINARNA";
        boolean popuni = chkPopuniBrucosima.isSelected();
        long neispravnih = Stream.of(lista.brucosi(), lista.saUslovom(), lista.listaCekanja())
                .flatMap(List::stream).filter(r -> r.greska() != null).count();

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy.");
        String poruka = "Objaviti " + naziv.toLowerCase() + " rang listu za akademsku "
                + lista.akademskaGodina() + "?\n\n"
                + "Studenti će na webu vidjeti listu kakva je SADA (šifra i bodovi).\n"
                + "Kasnije izmjene prijava neće se vidjeti na webu dok ne objavite ponovo.\n\n"
                + (od != null ? "Useljenje: " + od.format(df) + " – " + doDatuma.format(df)
                : "Useljenje: termin će biti objavljen naknadno")
                + (popuni ? "\n\nSlobodna mjesta su popunjena brucošima." : "")
                + (neispravnih > 0
                ? "\n\n" + neispravnih + " prijava s neispravnim podacima (⚠) neće biti objavljeno." : "");

        Alert potvrda = new Alert(Alert.AlertType.CONFIRMATION, poruka, ButtonType.YES, ButtonType.NO);
        potvrda.setHeaderText("Objava rang liste");
        if (potvrda.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        uPozadini(() -> RangListaApi.objavi(vrsta, popuni, od, doDatuma), o -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION,
                    nazivVrste(o.vrsta()) + " rang lista je objavljena (" + o.objavljeno() + ", "
                            + o.brojStudenata() + " studenata).", ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();
            ucitajObjavu();
        }, "Greška pri objavi rang liste.");
    }

    private String nazivVrste(String vrsta) {
        return "KONACNA".equals(vrsta) ? "Konačna" : "Preliminarna";
    }

    // =====================================================================
    //  DOKAZ O USLOVU
    // =====================================================================

    private void evidentirajDokaz(RangRedDTO r) {
        Alert potvrda = new Alert(Alert.AlertType.CONFIRMATION,
                "Student " + RangKolona.punoIme(r) + " (šifra " + r.sifra() + ") je dostavio dokaz "
                        + "o ispunjavanju uslova za upis?\n\nUpisat će se današnji datum i vrijeme, a prijava "
                        + "prelazi na listu \"s uslovom\", iza studenata koji su dokaz priložili uz prijavu.",
                ButtonType.YES, ButtonType.NO);
        potvrda.setHeaderText("Dokaz o uslovu za upis");
        if (potvrda.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        uPozadini(() -> RangListaApi.evidentirajDokaz(r.idPrijava()), d -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Dokaz evidentiran: " + d.dokazUslova(), ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();
            ucitaj();
        }, "Greška pri evidentiranju dokaza.");
    }

    private void ponistiDokaz(RangRedDTO r) {
        Alert potvrda = new Alert(Alert.AlertType.CONFIRMATION,
                "Poništiti dokaz evidentiran " + r.dokazUslova() + " za " + RangKolona.punoIme(r)
                        + "?\nPrijava se vraća na listu čekanja.",
                ButtonType.YES, ButtonType.NO);
        potvrda.setHeaderText("Poništavanje dokaza");
        if (potvrda.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        uPozadini(() -> RangListaApi.ponistiDokaz(r.idPrijava()), d -> ucitaj(),
                "Greška pri poništavanju dokaza.");
    }

    // =====================================================================
    //  DETALJI
    // =====================================================================

    private void otvoriDetalje(Integer idPrijave) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/detalji-prijave.fxml"));
            Scene scene = new Scene(loader.load());
            DetaljiPrijaveController controller = loader.getController();
            controller.ucitaj(idPrijave);

            Stage stage = new Stage();
            stage.setTitle("Detalji prijave");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setOnHidden(e -> ucitaj());           // izmjene se odmah vide na rang listi
            stage.show();
            Platform.runLater(() -> maximize(stage));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void maximize(Stage stage) {
        Rectangle2D b = Screen.getPrimary().getVisualBounds();
        stage.setX(b.getMinX());
        stage.setY(b.getMinY());
        stage.setWidth(b.getWidth());
        stage.setHeight(b.getHeight());
    }

    // =====================================================================
    //  IZVOZ
    // =====================================================================

    @FXML
    private void onExportHtmlPdf() {
        if (lista == null) return;

        List<RangKolona> vidljive = kolone.entrySet().stream()
                .filter(e -> e.getKey().isVisible()).map(Map.Entry::getValue).toList();

        Optional<ExportOpcije> izbor = new RangListaExportDijalog(lista, vidljive).showAndWait();
        if (izbor.isEmpty()) return;
        ExportOpcije o = izbor.get();

        String ekstenzija = o.excel() ? ".xlsx" : ".pdf";
        FileChooser fc = new FileChooser();
        fc.setTitle("Sačuvaj rang listu");
        fc.getExtensionFilters().add(o.excel()
                ? new FileChooser.ExtensionFilter("Excel fajl", "*.xlsx")
                : new FileChooser.ExtensionFilter("PDF fajl", "*.pdf"));
        fc.setInitialFileName("Rang_lista_"
                + (o.excel() ? "" : (o.konacna() ? "konacna_" : "preliminarna_"))
                + lista.godina() + ekstenzija);

        File file = fc.showSaveDialog(tblRangLista.getScene().getWindow());
        if (file == null) return;

        uPozadini(() -> o.excel()
                        ? new RangListaExcelExportService().export(lista, o, file)
                        : new RangListaHtmlExportService().export(lista, o, file),
                izostavljeno -> {
                    String poruka = o.excel() ? "Excel fajl je kreiran." : "PDF je kreiran.";
                    if (izostavljeno > 0) {
                        poruka += "\n\n" + izostavljeno + " prijav" + (izostavljeno == 1 ? "a nije" : "e nisu")
                                + " u izvozu zbog neispravnih podataka (⚠ u tabeli).";
                    }
                    Alert a = new Alert(Alert.AlertType.INFORMATION, poruka, ButtonType.OK);
                    a.setHeaderText(null);
                    a.showAndWait();
                }, "Greška pri izvozu rang liste.");
    }

    // =====================================================================
    //  POMOĆNE
    // =====================================================================

    private <T> void uPozadini(Supplier<T> posao, Consumer<T> uspjeh, String opstaGreska) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return posao.get();
            }
        };
        task.setOnSucceeded(e -> uspjeh.accept(task.getValue()));
        task.setOnFailed(e -> prikaziGresku(task.getException(), opstaGreska));
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void prikaziGresku(Throwable ex, String opsta) {
        if (!(ex instanceof ApiException)) ex.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR, ex instanceof ApiException ? ex.getMessage() : opsta);
        a.setHeaderText(null);
        a.showAndWait();
    }
}