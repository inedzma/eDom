package controllers;

import api.ApiException;
import api.StudentApi;
import ba.unze.edom.common.dto.StudentListaDTO;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import service.export.StudentExportService;
import service.export.StudentHtmlExportService;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class studentiController {

    @FXML private TableView<StudentListaDTO> tblStudenti;

    @FXML private TableColumn<StudentListaDTO, Integer> colId;
    @FXML private TableColumn<StudentListaDTO, String> colIme;
    @FXML private TableColumn<StudentListaDTO, String> colPrezime;
    @FXML private TableColumn<StudentListaDTO, String> colIndeks;
    @FXML private TableColumn<StudentListaDTO, String> colFakultet;
    @FXML private TableColumn<StudentListaDTO, Integer> colGodina;
    @FXML private TableColumn<StudentListaDTO, BigDecimal> colProsjek;
    @FXML private TableColumn<StudentListaDTO, String> colStatus;

    @FXML private Menu menuFakulteti;
    @FXML private Menu menuStatus;
    @FXML private Menu menuGodina;

    @FXML private TextField txtSearch;

    private final ObservableList<StudentListaDTO> masterList = FXCollections.observableArrayList();
    private FilteredList<StudentListaDTO> filteredList;

    private final Set<String> selectedFakulteti = new HashSet<>();
    private final Set<String> selectedStatusi = new HashSet<>();
    private final Set<Integer> selectedGodine = new HashSet<>();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().idStudent()));
        colIme.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().ime())));
        colPrezime.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().prezime())));
        colIndeks.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().brojIndeksa())));
        colFakultet.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().fakultet())));
        colGodina.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().godinaStudija()));
        colProsjek.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().prosjek()));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(statusNaziv(c.getValue()))));

        colProsjek.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty ? null : v == null ? "—" : String.format("%.2f", v));
            }
        });

        setupFilteringAndSorting();
        ucitajStudente();
    }

    // ---------- učitavanje ----------

    private void ucitajStudente() {
        Task<List<StudentListaDTO>> task = new Task<>() {
            @Override
            protected List<StudentListaDTO> call() {
                return StudentApi.dohvatiSve();
            }
        };

        task.setOnSucceeded(e -> {
            masterList.setAll(task.getValue());

            selectedFakulteti.clear();
            selectedStatusi.clear();
            selectedGodine.clear();
            initFakultetiFilter();
            initStatusFilter();
            initGodinaFilter();
            applyAllFilters();
        });

        task.setOnFailed(e -> prikaziGresku(task.getException(), "Greška pri učitavanju studenata."));

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onRefresh() {
        ucitajStudente();
    }

    // ---------- sortiranje ----------

    @FXML private void sortImeAZ()      { sortiraj(colIme, TableColumn.SortType.ASCENDING); }
    @FXML private void sortImeZA()      { sortiraj(colIme, TableColumn.SortType.DESCENDING); }
    @FXML private void sortIdUzlazno()  { sortiraj(colId, TableColumn.SortType.ASCENDING); }
    @FXML private void sortIdSilazno()  { sortiraj(colId, TableColumn.SortType.DESCENDING); }
    @FXML private void sortProsjekDesc(){ sortiraj(colProsjek, TableColumn.SortType.DESCENDING); }
    @FXML private void sortProsjekAsc() { sortiraj(colProsjek, TableColumn.SortType.ASCENDING); }

    private void sortiraj(TableColumn<StudentListaDTO, ?> kolona, TableColumn.SortType smjer) {
        kolona.setSortType(smjer);
        tblStudenti.getSortOrder().setAll(kolona);
    }

    // ---------- filteri ----------

    private void initFakultetiFilter() {
        Set<String> fakulteti = masterList.stream()
                .map(StudentListaDTO::fakultet)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        napuniMeni(menuFakulteti, fakulteti, selectedFakulteti, String::valueOf);
    }

    private void initStatusFilter() {
        Set<String> statusi = masterList.stream()
                .map(this::statusNaziv)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        napuniMeni(menuStatus, statusi, selectedStatusi, String::valueOf);
    }

    private void initGodinaFilter() {
        Set<Integer> godine = masterList.stream()
                .map(StudentListaDTO::godinaStudija)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        napuniMeni(menuGodina, godine, selectedGodine, String::valueOf);
    }

    private <T> void napuniMeni(Menu meni, Set<T> vrijednosti, Set<T> odabrane,
                                java.util.function.Function<T, String> tekst) {
        meni.getItems().clear();
        for (T v : vrijednosti) {
            CheckMenuItem item = new CheckMenuItem(tekst.apply(v));
            item.setOnAction(e -> {
                if (item.isSelected()) odabrane.add(v);
                else odabrane.remove(v);
                applyAllFilters();
            });
            meni.getItems().add(item);
        }
    }

    private void applyAllFilters() {
        String q = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase();

        filteredList.setPredicate(s -> {
            String imePrezime = nullSafe(s.ime()) + " " + nullSafe(s.prezime());

            boolean searchOk = q.isEmpty()
                    || imePrezime.toLowerCase().contains(q)
                    || nullSafe(s.brojIndeksa()).toLowerCase().contains(q)
                    || nullSafe(s.fakultet()).toLowerCase().contains(q)
                    || nullSafe(statusNaziv(s)).toLowerCase().contains(q)
                    || Objects.toString(s.godinaStudija(), "").contains(q)
                    || Objects.toString(s.idStudent(), "").contains(q);

            boolean fakultetOk = selectedFakulteti.isEmpty() || selectedFakulteti.contains(s.fakultet());
            boolean statusOk   = selectedStatusi.isEmpty()   || selectedStatusi.contains(statusNaziv(s));
            boolean godinaOk   = selectedGodine.isEmpty()    || selectedGodine.contains(s.godinaStudija());

            return searchOk && fakultetOk && statusOk && godinaOk;
        });
    }

    private void setupFilteringAndSorting() {
        filteredList = new FilteredList<>(masterList, s -> true);
        txtSearch.textProperty().addListener((obs, o, n) -> applyAllFilters());

        SortedList<StudentListaDTO> sorted = new SortedList<>(filteredList);
        sorted.comparatorProperty().bind(tblStudenti.comparatorProperty());
        tblStudenti.setItems(sorted);
    }

    // ---------- export ----------

    @FXML
    private void onExportPdf() {
        File file = odaberiFajl("Spremi PDF");
        if (file == null) return;
        try {
            new StudentExportService().exportData(List.copyOf(filteredList), file);
        } catch (RuntimeException ex) {
            prikaziGresku(ex, "Greška pri izvozu PDF-a.");
        }
    }

    @FXML
    private void onExportHtmlPdf() {
        File file = odaberiFajl("Spremi PDF (HTML)");
        if (file == null) return;
        try {
            new StudentHtmlExportService().exportData(List.copyOf(filteredList), file);
        } catch (RuntimeException ex) {
            prikaziGresku(ex, "Greška pri izvozu PDF-a.");
        }
    }

    private File odaberiFajl(String naslov) {
        FileChooser fc = new FileChooser();
        fc.setTitle(naslov);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        return fc.showSaveDialog(tblStudenti.getScene().getWindow());
    }

    // ---------- pomoćne ----------

    private String statusNaziv(StudentListaDTO s) {
        return s.socijalniStatus() == null ? null : s.socijalniStatus().naziv();
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