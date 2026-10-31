package controllers;

import api.ApiException;
import api.PrijavaApi;
import ba.unze.edom.common.dto.prijava.PrijavaListaDTO;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import service.export.PrijavaHtmlExportService;
import util.Ekrani;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Ekran "Prijave": brzi filteri po statusu, pretraga, tabela, izvoz. */
public class prijaveController {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

    /** Redoslijed pilula (kako ide obrada prijave). */
    private static final List<String> REDOSLIJED_STATUSA = List.of(
            "na pregledu", "nepotpuna", "na čekanju", "odobreno", "zaključeno", "iseljen", "odbijeno");

    @FXML private TableView<PrijavaListaDTO> tblPrijave;
    @FXML private TableColumn<PrijavaListaDTO, Integer> colId;
    @FXML private TableColumn<PrijavaListaDTO, String> colStudent;
    @FXML private TableColumn<PrijavaListaDTO, Integer> colGodina;
    @FXML private TableColumn<PrijavaListaDTO, LocalDate> colDatum;
    @FXML private TableColumn<PrijavaListaDTO, Double> colBodovi;
    @FXML private TableColumn<PrijavaListaDTO, Integer> colDokumenti;
    @FXML private TableColumn<PrijavaListaDTO, String> colStatus;
    @FXML private TableColumn<PrijavaListaDTO, String> colNapomena;

    @FXML private TextField txtSearch;
    @FXML private Menu menuFakulteti;
    @FXML private Menu menuGodineStudija;
    @FXML private FlowPane boxStatusi;
    @FXML private Label lblPodnaslov;
    @FXML private Label lblPrikazano;
    @FXML private Button btnOcisti;

    private final ObservableList<PrijavaListaDTO> masterList = FXCollections.observableArrayList();
    private FilteredList<PrijavaListaDTO> filteredList;

    private final Set<String> odabraniFakulteti = new HashSet<>();
    private final Set<Integer> odabraneGodine = new HashSet<>();
    private String odabraniStatus;                       // null = sve
    private final ToggleGroup statusGrupa = new ToggleGroup();

    @FXML
    public void initialize() {
        initKolone();
        initFilteri();
        initRedovi();
        tblPrijave.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tblPrijave.setPlaceholder(new Label("Nema prijava za odabrane filtere."));
        sortiraj(colId, TableColumn.SortType.DESCENDING);   // najnovije prvo
        ucitajPrijave();
    }

    // =====================================================================
    //  KOLONE
    // =====================================================================

    private void initKolone() {
        colId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().idPrijava()));
        colId.setStyle("-fx-alignment: CENTER;");

        // student: sortira po prezimenu, prikazuje ime i prezime + indeks · fakultet
        colStudent.setCellValueFactory(c -> new SimpleStringProperty(
                nullSafe(c.getValue().prezime()) + " " + nullSafe(c.getValue().ime())));
        colStudent.setCellFactory(col -> new TableCell<>() {
            private final Label ime = new Label();
            private final Label pod = new Label();
            private final VBox box = new VBox(2, ime, pod);
            {
                ime.getStyleClass().add("pr-ime");
                pod.getStyleClass().add("pr-sitno");
            }

            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(null);
                PrijavaListaDTO p = red(this, empty);
                if (p == null) {
                    setGraphic(null);
                    return;
                }
                ime.setText(nullSafe(p.ime()) + " " + nullSafe(p.prezime()));
                String fakultet = nullSafe(p.fakultet()).replace(" UNZE", "");
                pod.setText(spoji(" · ", p.brojIndeksa(), fakultet));
                setGraphic(box);
            }
        });

        colGodina.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().godinaStudija()));
        colGodina.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer g, boolean empty) {
                super.updateItem(g, empty);
                setText(empty ? null : godinaTekst(g));
            }
        });

        colDatum.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().datumPrijave()));
        colDatum.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(DATUM));
            }
        });

        // bodovi: "potvrđeno / moguće" + traka; neispravne prijave na dno pri sortiranju
        colBodovi.setCellValueFactory(c -> new SimpleObjectProperty<>(
                c.getValue().greska() != null ? -1.0 : nz(c.getValue().potvrdjeniBodovi())));
        colBodovi.setCellFactory(col -> new TableCell<>() {
            private final Label potv = new Label();
            private final Label mog = new Label();
            private final ProgressBar traka = new ProgressBar();
            private final VBox box = new VBox(5, new HBox(4, potv, mog), traka);
            {
                potv.getStyleClass().add("pr-bodovi");
                mog.getStyleClass().add("pr-sitno");
                traka.getStyleClass().add("pr-traka");
                traka.setPrefWidth(120);
            }

            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(null);
                setTooltip(null);
                PrijavaListaDTO p = red(this, empty);
                if (p == null) {
                    setGraphic(null);
                    return;
                }
                if (p.greska() != null) {
                    Label l = new Label("⚠ neispravni podaci");
                    l.getStyleClass().add("pr-greska");
                    setGraphic(l);
                    setTooltip(new Tooltip(p.greska()));
                    return;
                }
                double a = nz(p.potvrdjeniBodovi());
                double b = nz(p.potencijalniBodovi());
                potv.setText(String.format("%.2f", a));
                mog.setText(String.format("/ %.2f", b));
                traka.setProgress(b > 0 ? Math.min(1, a / b) : 0);
                traka.getStyleClass().remove("pr-traka--ceka");
                if (p.cekaVerifikaciju()) {
                    traka.getStyleClass().add("pr-traka--ceka");
                    setTooltip(new Tooltip("Dio bodova čeka provjeru dokumenata."));
                }
                setGraphic(box);
            }
        });

        colDokumenti.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().brojDokumenata()));
        colDokumenti.setStyle("-fx-alignment: CENTER;");

        colStatus.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().status())));
        colStatus.setCellFactory(col -> new TableCell<>() {
            private final Label pilula = new Label();

            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setText(null);
                if (empty || s == null || s.isBlank()) {
                    setGraphic(null);
                    return;
                }
                pilula.setText(velikoPrvo(s));
                pilula.getStyleClass().setAll("label", "pr-status", statusKlasa(s));
                setGraphic(pilula);
            }
        });

        colNapomena.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().napomena())));
        colNapomena.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String n, boolean empty) {
                super.updateItem(n, empty);
                boolean prazno = empty || n == null || n.isBlank();
                setText(prazno ? null : n.replace('\n', ' '));
                setTooltip(prazno ? null : new Tooltip(n));
                getStyleClass().remove("pr-napomena");
                if (!prazno) getStyleClass().add("pr-napomena");
            }
        });
    }

    // =====================================================================
    //  UČITAVANJE
    // =====================================================================

    private void ucitajPrijave() {
        Task<List<PrijavaListaDTO>> task = new Task<>() {
            @Override
            protected List<PrijavaListaDTO> call() {
                return PrijavaApi.lista(null);
            }
        };
        task.setOnSucceeded(e -> {
            masterList.setAll(task.getValue());

            odabraniFakulteti.retainAll(skupi(PrijavaListaDTO::fakultet));
            odabraneGodine.retainAll(skupi(PrijavaListaDTO::godinaStudija));
            napuniMeni(menuFakulteti, skupi(PrijavaListaDTO::fakultet), odabraniFakulteti, f -> f);
            napuniMeni(menuGodineStudija, skupi(PrijavaListaDTO::godinaStudija), odabraneGodine,
                    prijaveController::godinaTekst);

            napraviPilule();
            osvjeziPodnaslov();
            applyAllFilters();
        });
        task.setOnFailed(e -> prikaziGresku(task.getException(), "Greška pri učitavanju prijava."));
        pokreni(task);
    }

    @FXML
    private void onRefresh() {
        ucitajPrijave();
    }

    private void osvjeziPodnaslov() {
        Integer godina = masterList.stream().map(PrijavaListaDTO::akademskaGodina)
                .filter(Objects::nonNull).max(Integer::compare).orElse(null);
        long naPregledu = masterList.stream().filter(p -> "na pregledu".equals(p.status())).count();

        List<String> dijelovi = new ArrayList<>();
        if (godina != null) dijelovi.add("Akademska godina " + godina + "/" + (godina + 1));
        dijelovi.add(masterList.size() + " " + prijava(masterList.size()));
        if (naPregledu > 0) dijelovi.add(naPregledu + " čeka pregled");
        lblPodnaslov.setText(String.join("   ·   ", dijelovi));
    }

    // =====================================================================
    //  PILULE PO STATUSU
    // =====================================================================

    private void napraviPilule() {
        boxStatusi.getChildren().clear();
        statusGrupa.getToggles().clear();

        Map<String, Long> broj = masterList.stream()
                .collect(Collectors.groupingBy(p -> nullSafe(p.status()), Collectors.counting()));
        if (odabraniStatus != null && !broj.containsKey(odabraniStatus)) odabraniStatus = null;

        dodajPilulu(null, "Sve", masterList.size());
        for (String s : REDOSLIJED_STATUSA) {
            if (broj.containsKey(s)) dodajPilulu(s, velikoPrvo(s), broj.get(s));
        }
        broj.keySet().stream()
                .filter(s -> !s.isBlank() && !REDOSLIJED_STATUSA.contains(s))
                .sorted()
                .forEach(s -> dodajPilulu(s, velikoPrvo(s), broj.get(s)));
    }

    private void dodajPilulu(String status, String tekst, long n) {
        ToggleButton t = new ToggleButton(tekst + "   " + n);
        t.getStyleClass().add("pr-chip");
        t.setToggleGroup(statusGrupa);
        if (status != null) {
            Circle tacka = new Circle(4);
            tacka.getStyleClass().addAll("pr-tacka", statusKlasa(status));
            t.setGraphic(tacka);
        }
        t.setSelected(Objects.equals(status, odabraniStatus));
        t.setOnAction(e -> {
            odabraniStatus = status;
            t.setSelected(true);          // klik na već odabranu pilulu je ne gasi
            applyAllFilters();
        });
        boxStatusi.getChildren().add(t);
    }

    // =====================================================================
    //  FILTERI I PRETRAGA
    // =====================================================================

    private void initFilteri() {
        filteredList = new FilteredList<>(masterList, p -> true);
        txtSearch.textProperty().addListener((obs, o, n) -> applyAllFilters());

        SortedList<PrijavaListaDTO> sorted = new SortedList<>(filteredList);
        sorted.comparatorProperty().bind(tblPrijave.comparatorProperty());
        tblPrijave.setItems(sorted);
    }

    private <T extends Comparable<T>> Set<T> skupi(Function<PrijavaListaDTO, T> polje) {
        return masterList.stream().map(polje).filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private <T> void napuniMeni(Menu meni, Set<T> vrijednosti, Set<T> odabrane, Function<T, String> tekst) {
        meni.getItems().clear();
        for (T v : vrijednosti) {
            CheckMenuItem item = new CheckMenuItem(tekst.apply(v));
            item.setSelected(odabrane.contains(v));
            item.setOnAction(e -> {
                if (item.isSelected()) odabrane.add(v);
                else odabrane.remove(v);
                applyAllFilters();
            });
            meni.getItems().add(item);
        }
        if (vrijednosti.isEmpty()) {
            MenuItem prazno = new MenuItem("(nema)");
            prazno.setDisable(true);
            meni.getItems().add(prazno);
        }
    }

    private void applyAllFilters() {
        String q = txtSearch.getText() == null ? "" : txtSearch.getText().toLowerCase().trim();

        filteredList.setPredicate(p -> {
            boolean searchOk = q.isEmpty()
                    || (nullSafe(p.ime()) + " " + nullSafe(p.prezime())).toLowerCase().contains(q)
                    || (nullSafe(p.prezime()) + " " + nullSafe(p.ime())).toLowerCase().contains(q)
                    || nullSafe(p.brojIndeksa()).toLowerCase().contains(q)
                    || nullSafe(p.fakultet()).toLowerCase().contains(q)
                    || nullSafe(p.napomena()).toLowerCase().contains(q)
                    || Objects.toString(p.idPrijava(), "").equals(q);

            boolean statusOk = odabraniStatus == null || odabraniStatus.equals(p.status());
            boolean fakultetOk = odabraniFakulteti.isEmpty() || odabraniFakulteti.contains(p.fakultet());
            boolean godinaOk = odabraneGodine.isEmpty() || odabraneGodine.contains(p.godinaStudija());
            return searchOk && statusOk && fakultetOk && godinaOk;
        });

        int n = filteredList.size();
        lblPrikazano.setText(n == masterList.size()
                ? n + " " + prijava(n)
                : "Prikazano " + n + " od " + masterList.size() + " " + prijava(masterList.size()));

        boolean aktivno = !q.isEmpty() || odabraniStatus != null
                || !odabraniFakulteti.isEmpty() || !odabraneGodine.isEmpty();
        btnOcisti.setVisible(aktivno);
        btnOcisti.setManaged(aktivno);
    }

    @FXML
    private void onOcisti() {
        txtSearch.clear();
        odabraniStatus = null;
        odabraniFakulteti.clear();
        odabraneGodine.clear();
        for (Menu m : List.of(menuFakulteti, menuGodineStudija)) {
            m.getItems().forEach(i -> { if (i instanceof CheckMenuItem c) c.setSelected(false); });
        }
        napraviPilule();
        applyAllFilters();
    }

    // =====================================================================
    //  SORTIRANJE
    // =====================================================================

    @FXML private void sortNajnovije()     { sortiraj(colId, TableColumn.SortType.DESCENDING); }
    @FXML private void sortNajstarije()    { sortiraj(colId, TableColumn.SortType.ASCENDING); }
    @FXML private void sortPrezimeAZ()     { sortiraj(colStudent, TableColumn.SortType.ASCENDING); }
    @FXML private void sortPrezimeZA()     { sortiraj(colStudent, TableColumn.SortType.DESCENDING); }
    @FXML private void sortBodoviSilazno() { sortiraj(colBodovi, TableColumn.SortType.DESCENDING); }
    @FXML private void sortBodoviUzlazno() { sortiraj(colBodovi, TableColumn.SortType.ASCENDING); }

    private void sortiraj(TableColumn<PrijavaListaDTO, ?> kolona, TableColumn.SortType smjer) {
        kolona.setSortType(smjer);
        tblPrijave.getSortOrder().setAll(kolona);
    }

    // =====================================================================
    //  OTVARANJE DETALJA
    // =====================================================================

    private void initRedovi() {
        tblPrijave.setRowFactory(tv -> {
            TableRow<PrijavaListaDTO> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty() && e.getClickCount() == 2) otvoriDetalje(row.getItem().idPrijava());
            });
            MenuItem otvori = new MenuItem("Otvori detalje");
            otvori.setOnAction(e -> { if (!row.isEmpty()) otvoriDetalje(row.getItem().idPrijava()); });
            row.contextMenuProperty().bind(javafx.beans.binding.Bindings
                    .when(row.emptyProperty()).then((ContextMenu) null).otherwise(new ContextMenu(otvori)));
            return row;
        });
        tblPrijave.setOnKeyPressed(e -> {
            PrijavaListaDTO p = tblPrijave.getSelectionModel().getSelectedItem();
            if (e.getCode() == KeyCode.ENTER && p != null) otvoriDetalje(p.idPrijava());
        });
    }

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
            stage.setOnHidden(e -> ucitajPrijave());     // izmjene se odmah vide u tabeli
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

    @FXML
    private void onNovaPrijava() {
        Ekrani.zamijeni(tblPrijave, "/views/novi-student.fxml", null);
    }

    // =====================================================================
    //  IZVOZ
    // =====================================================================

    @FXML
    private void onExportHtmlPdf() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Sačuvaj PDF");
        fc.setInitialFileName("Prijave.pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File file = fc.showSaveDialog(tblPrijave.getScene().getWindow());
        if (file == null) return;

        List<PrijavaListaDTO> podaci = List.copyOf(tblPrijave.getItems());   // kako admin vidi
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                new PrijavaHtmlExportService().exportData(podaci, file);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "PDF je sačuvan: " + file.getName());
            a.setHeaderText(null);
            a.showAndWait();
        });
        task.setOnFailed(e -> prikaziGresku(task.getException(), "Greška pri izvozu PDF-a."));
        pokreni(task);
    }

    // =====================================================================
    //  POMOĆNE
    // =====================================================================

    private static PrijavaListaDTO red(TableCell<PrijavaListaDTO, ?> c, boolean empty) {
        return empty || c.getTableRow() == null ? null : c.getTableRow().getItem();
    }

    static String statusKlasa(String s) {
        return switch (s == null ? "" : s.toLowerCase()) {
            case "na pregledu" -> "st-pregled";
            case "nepotpuna" -> "st-nepotpuna";
            case "na čekanju" -> "st-cekanje";
            case "odobreno" -> "st-odobreno";
            case "zaključeno" -> "st-zakljuceno";
            case "iseljen" -> "st-iseljen";
            case "odbijeno" -> "st-odbijeno";
            default -> "st-ostalo";
        };
    }

    private static String godinaTekst(Integer g) {
        if (g == null) return "—";
        return switch (g) {
            case 7 -> "Apsolvent";
            case 8 -> "Postdiplomac";
            default -> g + ". godina";
        };
    }

    /** 1 prijava, 2–4 prijave, 5+ prijava (21 prijava, 22 prijave…). */
    private static String prijava(long n) {
        long d = n % 10, s = n % 100;
        return d >= 2 && d <= 4 && (s < 12 || s > 14) ? "prijave" : "prijava";
    }

    private static String velikoPrvo(String s) {
        return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String spoji(String sep, String... dijelovi) {
        return Arrays.stream(dijelovi).filter(x -> x != null && !x.isBlank()).collect(Collectors.joining(sep));
    }

    private static double nz(Double d) {
        return d == null ? 0 : d;
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private void pokreni(Task<?> task) {
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