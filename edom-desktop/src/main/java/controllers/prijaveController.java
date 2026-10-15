package controllers;

import api.ApiException;
import api.PrijavaApi;
import ba.unze.edom.common.dto.PrijavaListaDTO;
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
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
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

public class prijaveController {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

    @FXML private TableView<PrijavaListaDTO> tblPrijave;

    @FXML private TableColumn<PrijavaListaDTO, Integer> colId;
    @FXML private TableColumn<PrijavaListaDTO, String> colIme;
    @FXML private TableColumn<PrijavaListaDTO, String> colPrezime;
    @FXML private TableColumn<PrijavaListaDTO, LocalDate> colDatum;
    @FXML private TableColumn<PrijavaListaDTO, Integer> colAkGod;
    @FXML private TableColumn<PrijavaListaDTO, Double> colUkupniBodovi;
    @FXML private TableColumn<PrijavaListaDTO, String> colStatus;
    @FXML private TableColumn<PrijavaListaDTO, String> colNapomena;

    @FXML private TextField txtSearch;
    @FXML private Menu menuFakulteti;
    @FXML private Menu menuStatusPrijave;
    @FXML private Menu menuGodineStudija;

    private final ObservableList<PrijavaListaDTO> masterList = FXCollections.observableArrayList();
    private FilteredList<PrijavaListaDTO> filteredList;

    private final Set<String> selectedFakulteti = new HashSet<>();
    private final Set<String> selectedStatusiPrijave = new HashSet<>();
    private final Set<Integer> selectedGodineStudija = new HashSet<>();

    @FXML
    public void initialize() {
        initTableColumns();
        initStatusBadges();
        setupFilteringAndSorting();
        setupRowDoubleClick();
        ucitajPrijave();
    }

    // ---------- kolone ----------

    private void initTableColumns() {
        colId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().idPrijava()));
        colIme.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().ime())));
        colPrezime.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().prezime())));
        colAkGod.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().akademskaGodina()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().status())));
        colNapomena.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().napomena())));

        // datum: vrijednost je LocalDate (ispravno sortiranje), prikaz dd.MM.yyyy.
        colDatum.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().datumPrijave()));
        colDatum.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(DATUM));
            }
        });

        // bodovi: sortira po potvrđenim, prikazuje "potvrđeno / moguće"
        colUkupniBodovi.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().potvrdjeniBodovi()));
        colUkupniBodovi.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                getStyleClass().removeAll("bodovi-greska", "bodovi-ceka");
                setTooltip(null);

                PrijavaListaDTO p = (empty || getTableRow() == null) ? null : getTableRow().getItem();
                if (p == null) {
                    setText(null);
                    return;
                }

                if (p.greska() != null) {
                    setText("⚠ neispravni podaci");
                    setTooltip(new Tooltip(p.greska()));
                    getStyleClass().add("bodovi-greska");
                    return;
                }

                setText(String.format("%.2f / %.2f", p.potvrdjeniBodovi(), p.potencijalniBodovi()));
                if (p.cekaVerifikaciju()) {
                    getStyleClass().add("bodovi-ceka");
                    setTooltip(new Tooltip("Dio bodova čeka provjeru dokumenata."));
                }
            }
        });
    }

    private void initStatusBadges() {
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                getStyleClass().removeAll("status-approved", "status-rejected", "status-review", "status-closed");

                if (empty || status == null || status.isBlank()) {
                    setText(null);
                    return;
                }
                setText(status);

                String s = status.toLowerCase();
                if (s.contains("odob"))       getStyleClass().add("status-approved");
                else if (s.contains("odbi"))  getStyleClass().add("status-rejected");
                else if (s.contains("pregl")) getStyleClass().add("status-review");
                else if (s.contains("zaklj")) getStyleClass().add("status-closed");
            }
        });
    }

    // ---------- učitavanje ----------

    private void ucitajPrijave() {
        Task<List<PrijavaListaDTO>> task = new Task<>() {
            @Override
            protected List<PrijavaListaDTO> call() {
                return PrijavaApi.lista(null);
            }
        };

        task.setOnSucceeded(e -> {
            masterList.setAll(task.getValue());

            selectedFakulteti.clear();
            selectedStatusiPrijave.clear();
            selectedGodineStudija.clear();

            napuniMeni(menuFakulteti, skupi(PrijavaListaDTO::fakultet), selectedFakulteti);
            napuniMeni(menuStatusPrijave, skupi(PrijavaListaDTO::status), selectedStatusiPrijave);
            napuniMeni(menuGodineStudija, skupi(PrijavaListaDTO::godinaStudija), selectedGodineStudija);

            applyAllFilters();
        });

        task.setOnFailed(e -> prikaziGresku(task.getException(), "Greška pri učitavanju prijava."));

        pokreni(task);
    }

    @FXML
    private void onRefresh() {
        ucitajPrijave();
    }

    // ---------- sortiranje ----------

    @FXML private void sortImeAZ()         { sortiraj(colIme, TableColumn.SortType.ASCENDING); }
    @FXML private void sortImeZA()         { sortiraj(colIme, TableColumn.SortType.DESCENDING); }
    @FXML private void sortIdUzlazno()     { sortiraj(colId, TableColumn.SortType.ASCENDING); }
    @FXML private void sortIdSilazno()     { sortiraj(colId, TableColumn.SortType.DESCENDING); }
    @FXML private void sortBodoviUzlazno() { sortiraj(colUkupniBodovi, TableColumn.SortType.ASCENDING); }
    @FXML private void sortBodoviSilazno() { sortiraj(colUkupniBodovi, TableColumn.SortType.DESCENDING); }

    private void sortiraj(TableColumn<PrijavaListaDTO, ?> kolona, TableColumn.SortType smjer) {
        kolona.setSortType(smjer);
        tblPrijave.getSortOrder().setAll(kolona);
    }

    // ---------- filteri ----------

    private <T extends Comparable<T>> Set<T> skupi(Function<PrijavaListaDTO, T> polje) {
        return masterList.stream()
                .map(polje)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private <T> void napuniMeni(Menu meni, Set<T> vrijednosti, Set<T> odabrane) {
        meni.getItems().clear();
        for (T v : vrijednosti) {
            CheckMenuItem item = new CheckMenuItem(String.valueOf(v));
            item.setOnAction(e -> {
                if (item.isSelected()) odabrane.add(v);
                else odabrane.remove(v);
                applyAllFilters();
            });
            meni.getItems().add(item);
        }
    }

    private void setupFilteringAndSorting() {
        filteredList = new FilteredList<>(masterList, p -> true);
        txtSearch.textProperty().addListener((obs, o, n) -> applyAllFilters());

        SortedList<PrijavaListaDTO> sorted = new SortedList<>(filteredList);
        sorted.comparatorProperty().bind(tblPrijave.comparatorProperty());
        tblPrijave.setItems(sorted);
    }

    private void applyAllFilters() {
        String q = txtSearch.getText() == null ? "" : txtSearch.getText().toLowerCase().trim();

        filteredList.setPredicate(p -> {
            String imePrezime = nullSafe(p.ime()) + " " + nullSafe(p.prezime());

            boolean searchOk = q.isEmpty()
                    || imePrezime.toLowerCase().contains(q)
                    || nullSafe(p.brojIndeksa()).toLowerCase().contains(q)
                    || nullSafe(p.status()).toLowerCase().contains(q)
                    || nullSafe(p.fakultet()).toLowerCase().contains(q)
                    || nullSafe(p.napomena()).toLowerCase().contains(q)
                    || Objects.toString(p.idPrijava(), "").contains(q);

            boolean fakultetOk = selectedFakulteti.isEmpty() || selectedFakulteti.contains(p.fakultet());
            boolean statusOk   = selectedStatusiPrijave.isEmpty() || selectedStatusiPrijave.contains(p.status());
            boolean godinaOk   = selectedGodineStudija.isEmpty() || selectedGodineStudija.contains(p.godinaStudija());

            return searchOk && fakultetOk && statusOk && godinaOk;
        });
    }

    // ---------- export ----------

    @FXML
    private void onExportHtmlPdf() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Spremi PDF (HTML)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fc.showSaveDialog(tblPrijave.getScene().getWindow());
        if (file == null) return;

        // ono što je trenutno u tabeli: filtrirano i sortirano kako admin vidi
        List<PrijavaListaDTO> podaci = List.copyOf(tblPrijave.getItems());

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                new PrijavaHtmlExportService().exportData(podaci, file);
                return null;
            }
        };
        task.setOnFailed(e -> prikaziGresku(task.getException(), "Greška pri izvozu PDF-a."));
        pokreni(task);
    }
    private void setupRowDoubleClick() {
        tblPrijave.setRowFactory(tv -> {
            TableRow<PrijavaListaDTO> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty() && e.getClickCount() == 2) {
                    otvoriDetalje(row.getItem().idPrijava());
                }
            });
            return row;
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
            stage.show();

            Platform.runLater(() -> maximize(stage));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void maximize(Stage stage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
    }

    @FXML
    private void onNovaPrijava() {
        Ekrani.zamijeni(tblPrijave, "/views/novi-student.fxml", null);
    }

    // ---------- pomoćne ----------

    private void pokreni(Task<?> task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void prikaziGresku(Throwable ex, String opsta) {
        if (!(ex instanceof ApiException)) ex.printStackTrace();
        String poruka = ex instanceof ApiException ? ex.getMessage() : opsta;
        new Alert(Alert.AlertType.ERROR, poruka).showAndWait();
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }
}