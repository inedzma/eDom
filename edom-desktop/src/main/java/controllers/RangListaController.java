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
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import service.export.ExportOpcije;
import service.export.RangKolona;
import service.export.RangListaHtmlExportService;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RangListaController {

    @FXML private Label lblGodina;
    @FXML private Label lblSazetak;
    @FXML private ToggleButton btnBrucosi;
    @FXML private ToggleButton btnSaUslovom;
    @FXML private ToggleButton btnCekanje;
    @FXML private ToggleButton btnSve;
    @FXML private CheckBox chkPopuniBrucosima;
    @FXML private Menu menuKolone;

    @FXML private TableView<RangRedDTO> tblRangLista;

    /** Kolone pravi kontroler, ne FXML — redoslijed = redoslijed u meniju i u internom PDF-u. */
    private final Map<TableColumn<RangRedDTO, RangRedDTO>, RangKolona> kolone = new LinkedHashMap<>();
    private final TableColumn<RangRedDTO, RangRedDTO> colAkcije = new TableColumn<>("");

    /** Redoslijed = redoslijed u meniju i u internom PDF-u. */

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
        chkPopuniBrucosima.selectedProperty().addListener((obs, o, n) -> ucitaj());
        ucitaj();
    }

    /** Ako se učita stari FXML, reci tačno šta fali. */
    private void provjeriFxml() {
        Map<String, Object> potrebno = new LinkedHashMap<>();
        potrebno.put("lblGodina", lblGodina);
        potrebno.put("lblSazetak", lblSazetak);
        potrebno.put("btnBrucosi", btnBrucosi);
        potrebno.put("btnSaUslovom", btnSaUslovom);
        potrebno.put("btnCekanje", btnCekanje);
        potrebno.put("btnSve", btnSve);
        potrebno.put("chkPopuniBrucosima", chkPopuniBrucosima);
        potrebno.put("menuKolone", menuKolone);
        potrebno.put("tblRangLista", tblRangLista);

        List<String> fale = potrebno.entrySet().stream()
                .filter(e -> e.getValue() == null)
                .map(Map.Entry::getKey)
                .toList();
        if (!fale.isEmpty()) {
            throw new IllegalStateException("rang-lista.fxml nema fx:id " + fale
                    + " — učitava se stari FXML (zamijeni ga u src/main/resources/views, pa Rebuild).");
        }
    }

    private void napraviKolone() {
        for (RangKolona k : RangKolona.values()) {
            TableColumn<RangRedDTO, RangRedDTO> col = new TableColumn<>(k.naslov());
            col.setPrefWidth(sirina(k));
            if (k.centrirano()) col.getStyleClass().add("centered-cell");
            if (k == RangKolona.KATEGORIJA) col.setVisible(false);   // uključuje se u prikazu "Sve"
            kolone.put(col, k);
        }
        colAkcije.setPrefWidth(200);

        tblRangLista.getColumns().setAll(kolone.keySet());
        tblRangLista.getColumns().add(colAkcije);
    }

    private double sirina(RangKolona k) {
        return switch (k) {
            case REDNI_BROJ -> 55;
            case SIFRA, GODINA -> 75;
            case IME -> 240;
            case KATEGORIJA, SOCIJALNI -> 110;
            case USPJEH, BRANIOCI, UDALJENOST, UKUPNO -> 85;
            case STATUS -> 190;
        };
    }

    // ---------- kolone ----------

    private void initKolone() {
        kolone.forEach((col, k) -> {
            col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
            col.setSortable(false);   // redoslijed određuje rang, ne klik na kolonu
            col.setCellFactory(tc -> new TableCell<>() {
                @Override
                protected void updateItem(RangRedDTO r, boolean empty) {
                    super.updateItem(r, empty);
                    setTooltip(null);
                    if (empty || r == null) {
                        setText(null);
                        return;
                    }
                    setText(k.vrijednost(r, getIndex() + 1));
                    if (r.greska() != null && (k == RangKolona.UKUPNO || k == RangKolona.STATUS)) {
                        setTooltip(new Tooltip(r.greska()));
                    }
                }
            });
        });
        tblRangLista.getSelectionModel().setCellSelectionEnabled(false);
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
                btnDetalji.getStyleClass().add("det-btn");
                btnDokaz.getStyleClass().add("dokaz-btn");
                btnPonisti.getStyleClass().add("ponisti-btn");
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

    /** Boje redova + crvena crta ispod posljednjeg primljenog. */
    private void initRedove() {
        tblRangLista.setRowFactory(tv -> new TableRow<>() {
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
        });
    }

    private boolean posljednjiPrimljen(int i) {
        ObservableList<RangRedDTO> items = tblRangLista.getItems();
        return i + 1 >= items.size() || !items.get(i + 1).primljen();
    }

    private void initMeniKolona() {
        menuKolone.getItems().clear();
        kolone.forEach((col, k) -> {
            CheckMenuItem item = new CheckMenuItem(k.naslov());
            item.setSelected(col.isVisible());
            col.visibleProperty().bindBidirectional(item.selectedProperty());
            if (k == RangKolona.KATEGORIJA) stavkaKategorija = item;
            menuKolone.getItems().add(item);
        });
    }

    private void initGrupe() {
        ToggleGroup grupa = new ToggleGroup();
        btnBrucosi.setToggleGroup(grupa);
        btnSaUslovom.setToggleGroup(grupa);
        btnCekanje.setToggleGroup(grupa);
        btnSve.setToggleGroup(grupa);
        btnBrucosi.setSelected(true);

        grupa.selectedToggleProperty().addListener((obs, stari, novi) -> {
            if (novi == null) {
                stari.setSelected(true);   // jedna grupa je uvijek odabrana
            } else {
                prikaziGrupu();
            }
        });
    }

    // ---------- učitavanje ----------

    private void ucitaj() {
        boolean popuni = chkPopuniBrucosima.isSelected();
        uPozadini(() -> RangListaApi.dohvati(null, popuni), l -> {
            lista = l;
            lblGodina.setText(l.akademskaGodina() != null
                    ? "Akademska godina " + l.akademskaGodina()
                    : "Nema prijava");
            btnBrucosi.setText("Brucoši (" + l.brucosi().size() + ")");
            btnSaUslovom.setText("S uslovom (" + l.saUslovom().size() + ")");
            btnCekanje.setText("Lista čekanja (" + l.listaCekanja().size() + ")");
            btnSve.setText("Sve (" + (l.brucosi().size() + l.saUslovom().size() + l.listaCekanja().size()) + ")");
            lblSazetak.setText(String.format(
                    "Kapacitet %d  ·  Brucoši: primljeno %d (kvota %d)  ·  Više godine: primljeno %d (kvota %d)"
                            + "  ·  Slobodno: %d  ·  Na listi čekanja: %d",
                    l.kapacitet(), l.primljenoBrucosa(), l.kvotaBrucosi(),
                    l.primljenoVisih(), l.kvotaVisi(), l.slobodno(), l.listaCekanja().size()));
            prikaziGrupu();
        }, "Greška pri učitavanju rang liste.");
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
    }

    @FXML
    private void onRefresh() {
        ucitaj();
    }

    // ---------- dokaz o uslovu ----------

    private void evidentirajDokaz(RangRedDTO r) {
        Alert potvrda = new Alert(Alert.AlertType.CONFIRMATION,
                "Student " + RangKolona.punoIme(r) + " (šifra " + r.sifra() + ") je dostavio dokaz "
                        + "o ispunjavanju uslova za upis?\n\nUpisat će se današnji datum i vrijeme, a prijava "
                        + "prelazi na listu \"s uslovom\", iza studenata koji su dokaz priložili uz prijavu.",
                ButtonType.YES, ButtonType.NO);
        potvrda.setHeaderText("Dokaz o uslovu za upis");
        if (potvrda.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        uPozadini(() -> RangListaApi.evidentirajDokaz(r.idPrijava()), d -> {
            new Alert(Alert.AlertType.INFORMATION,
                    "Dokaz evidentiran: " + d.dokazUslova(), ButtonType.OK).showAndWait();
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

    // ---------- detalji ----------

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

    // ---------- export ----------

    @FXML
    private void onExportHtmlPdf() {
        if (lista == null) return;

        List<RangKolona> vidljive = kolone.entrySet().stream()
                .filter(e -> e.getKey().isVisible())
                .map(Map.Entry::getValue)
                .toList();

        Optional<ExportOpcije> izbor = new RangListaExportDijalog(lista, vidljive).showAndWait();
        if (izbor.isEmpty()) return;
        ExportOpcije o = izbor.get();

        FileChooser fc = new FileChooser();
        fc.setTitle("Sačuvaj rang listu");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF fajl", "*.pdf"));
        fc.setInitialFileName("Rang_lista_" + (o.konacna() ? "konacna_" : "preliminarna_")
                + (o.javna() ? "javna_" : "interna_") + lista.godina() + ".pdf");

        File file = fc.showSaveDialog(tblRangLista.getScene().getWindow());
        if (file == null) return;

        try {
            int izostavljeno = new RangListaHtmlExportService().export(lista, o, file);
            String poruka = "PDF uspješno kreiran!";
            if (izostavljeno > 0) {
                poruka += "\n\n" + izostavljeno + " prijav" + (izostavljeno == 1 ? "a nije" : "e nisu")
                        + " u PDF-u zbog neispravnih podataka (⚠ u tabeli).";
            }
            new Alert(Alert.AlertType.INFORMATION, poruka, ButtonType.OK).showAndWait();
        } catch (RuntimeException ex) {
            prikaziGresku(ex, "Greška pri izvozu rang liste.");
        }
    }

    // ---------- pomoćne ----------

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
        String poruka = ex instanceof ApiException ? ex.getMessage() : opsta;
        new Alert(Alert.AlertType.ERROR, poruka).showAndWait();
    }
}