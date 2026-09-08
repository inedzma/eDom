package controllers;

import dao.PrijavaDAO;
import dao.StudentDAO;
import javafx.application.Platform;
import javafx.beans.property.*;
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
import javafx.stage.Screen;
import javafx.stage.Stage;
import model.Prijava;
import model.Student;
import service.export.PrijavaHtmlExportService;
import dto.PrijavaExportDTo;

import java.util.*;
import java.util.stream.Collectors;

public class prijaveController {

    @FXML private TableView<Prijava> tblPrijave;

    @FXML private TableColumn<Prijava, Integer> colId;
    @FXML private TableColumn<Prijava, String> colIme;
    @FXML private TableColumn<Prijava, String> colPrezime;
    @FXML private TableColumn<Prijava, String> colDatum;
    @FXML private TableColumn<Prijava, Integer> colAkGod;
    @FXML private TableColumn<Prijava, Double> colUkupniBodovi;
    @FXML private TableColumn<Prijava, String> colStatus;
    @FXML private TableColumn<Prijava, String> colNapomena;

    @FXML private TextField txtSearch;
    @FXML private Menu menuFakulteti;
    @FXML private Menu menuStatusPrijave;
    @FXML private Menu menuGodineStudija;

    private final PrijavaDAO prijavaDAO = new PrijavaDAO();
    private final StudentDAO studentDAO = new StudentDAO();

    private ObservableList<Prijava> masterList;
    private FilteredList<Prijava> filteredList;

    private Map<Integer, Student> studentMap;

    private final Set<String> selectedFakulteti = new HashSet<>();
    private final Set<String> selectedStatusiPrijave = new HashSet<>();
    private final Set<Integer> selectedGodineStudija = new HashSet<>();

    @FXML
    public void initialize() {
        initStudentMap();
        initTableColumns();
        initStatusBadges();

        masterList = FXCollections.observableArrayList(prijavaDAO.dohvatiSvePrijave());

        initFakultetiFilter();
        initStatusPrijaveFilter();
        initGodinaStudijaFilter();

        setupFilteringAndSorting();
        setupRowDoubleClick();
    }

    private void initTableColumns() {
        colId.setCellValueFactory(cd ->
                new SimpleIntegerProperty(cd.getValue().getIdPrijava()).asObject());

        colIme.setCellValueFactory(cd ->
                new SimpleStringProperty(nullSafe(cd.getValue().getImeStudenta())));

        colPrezime.setCellValueFactory(cd ->
                new SimpleStringProperty(nullSafe(cd.getValue().getPrezimeStudenta())));

        colDatum.setCellValueFactory(cd ->
                new SimpleStringProperty(
                        cd.getValue().getDatumPrijava() != null
                                ? cd.getValue().getDatumPrijava().toString()
                                : ""
                ));

        colAkGod.setCellValueFactory(cd ->
                new SimpleIntegerProperty(cd.getValue().getAkademskaGodina()).asObject());

        colUkupniBodovi.setCellValueFactory(cd ->
                new SimpleDoubleProperty(cd.getValue().getUkupniBodovi()).asObject());

        colStatus.setCellValueFactory(cd ->
                new SimpleStringProperty(
                        cd.getValue().getStatusPrijave() != null
                                ? cd.getValue().getStatusPrijave().getNaziv()
                                : ""
                ));

        colNapomena.setCellValueFactory(cd ->
                new SimpleStringProperty(nullSafe(cd.getValue().getNapomena())));
    }

    private void initStatusBadges() {
        colStatus.setCellFactory(column -> new TableCell<Prijava, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null || status.isBlank()) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().removeAll("status-approved", "status-rejected", "status-review", "status-closed");
                    return;
                }

                setText(status);
                setGraphic(null);

                getStyleClass().removeAll("status-approved", "status-rejected", "status-review", "status-closed");

                String s = status.toLowerCase();

                if (s.contains("odob")) {
                    getStyleClass().add("status-approved");
                } else if (s.contains("odbi")) {
                    getStyleClass().add("status-rejected");
                } else if (s.contains("pregl")) {
                    getStyleClass().add("status-review");
                } else if (s.contains("zaklj")) {
                    getStyleClass().add("status-closed");
                }
            }
        });
    }

    @FXML
    private void sortImeAZ() {
        colIme.setSortType(TableColumn.SortType.ASCENDING);
        tblPrijave.getSortOrder().setAll(colIme);
    }

    @FXML
    private void sortImeZA() {
        colIme.setSortType(TableColumn.SortType.DESCENDING);
        tblPrijave.getSortOrder().setAll(colIme);
    }

    @FXML
    private void sortIdUzlazno() {
        colId.setSortType(TableColumn.SortType.ASCENDING);
        tblPrijave.getSortOrder().setAll(colId);
    }

    @FXML
    private void sortIdSilazno() {
        colId.setSortType(TableColumn.SortType.DESCENDING);
        tblPrijave.getSortOrder().setAll(colId);
    }

    @FXML
    private void sortBodoviUzlazno() {
        colUkupniBodovi.setSortType(TableColumn.SortType.ASCENDING);
        tblPrijave.getSortOrder().setAll(colUkupniBodovi);
    }

    @FXML
    private void sortBodoviSilazno() {
        colUkupniBodovi.setSortType(TableColumn.SortType.DESCENDING);
        tblPrijave.getSortOrder().setAll(colUkupniBodovi);
    }

    @FXML
    private void onExportHtmlPdf() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Spremi PDF (HTML)");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );

        java.io.File file = fileChooser.showSaveDialog(tblPrijave.getScene().getWindow());
        if (file == null) return;

        List<PrijavaExportDTo> dataZaExport = pripremiPrijaveZaExport();

        Task<Void> exportTask = new Task<>() {
            @Override
            protected Void call() {
                PrijavaHtmlExportService service = new PrijavaHtmlExportService();
                service.exportData(dataZaExport, file);
                return null;
            }
        };
        new Thread(exportTask).start();
    }

    private List<PrijavaExportDTo> pripremiPrijaveZaExport() {
        return tblPrijave.getItems().stream().map(p -> {
            Student s = studentMap.get(p.getIdStudent());

            return new PrijavaExportDTo(
                    p.getIdPrijava(),
                    s != null ? nullSafe(s.getIme()) : "",
                    s != null ? nullSafe(s.getPrezime()) : "",
                    s != null ? nullSafe(s.getFakultet()) : "",
                    p.getDatumPrijava() != null ? p.getDatumPrijava().toString() : "",
                    p.getUkupniBodovi(),
                    p.getStatusPrijave() != null ? p.getStatusPrijave().getNaziv() : ""
            );
        }).toList();
    }

    private void setupFilteringAndSorting() {
        filteredList = new FilteredList<>(masterList, p -> true);

        txtSearch.textProperty().addListener((obs, o, n) -> applyAllFilters());

        SortedList<Prijava> sorted = new SortedList<>(filteredList);
        sorted.comparatorProperty().bind(tblPrijave.comparatorProperty());

        tblPrijave.setItems(sorted);
    }

    private void applyAllFilters() {
        filteredList.setPredicate(p -> {

            String q = txtSearch.getText() == null ? "" : txtSearch.getText().toLowerCase().trim();

            Student s = studentMap.get(p.getIdStudent());
            String fakultet = s != null ? nullSafe(s.getFakultet()) : "";
            Integer godinaStudija = s != null ? s.getGodinaStudija() : null;

            String status = (p.getStatusPrijave() != null) ? nullSafe(p.getStatusPrijave().getNaziv()) : "";

            boolean searchOk = q.isEmpty()
                    || nullSafe(p.getImeStudenta()).toLowerCase().contains(q)
                    || nullSafe(p.getPrezimeStudenta()).toLowerCase().contains(q)
                    || status.toLowerCase().contains(q)
                    || fakultet.toLowerCase().contains(q)
                    || String.valueOf(p.getIdPrijava()).contains(q)
                    || nullSafe(p.getNapomena()).toLowerCase().contains(q);

            boolean fakultetOk = selectedFakulteti.isEmpty()
                    || selectedFakulteti.contains(fakultet);

            boolean statusOk = selectedStatusiPrijave.isEmpty()
                    || selectedStatusiPrijave.contains(status);

            boolean godinaOk = selectedGodineStudija.isEmpty()
                    || (godinaStudija != null && selectedGodineStudija.contains(godinaStudija));

            return searchOk && fakultetOk && statusOk && godinaOk;
        });
    }

    private void initFakultetiFilter() {
        Set<String> fakulteti = studentMap.values().stream()
                .map(Student::getFakultet)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));

        menuFakulteti.getItems().clear();

        for (String f : fakulteti) {
            CheckMenuItem item = new CheckMenuItem(f);
            item.setOnAction(e -> {
                if (item.isSelected()) selectedFakulteti.add(f);
                else selectedFakulteti.remove(f);
                applyAllFilters();
            });
            menuFakulteti.getItems().add(item);
        }
    }

    private void initGodinaStudijaFilter() {
        Set<Integer> godine = studentMap.values().stream()
                .map(Student::getGodinaStudija)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));

        menuGodineStudija.getItems().clear();

        for (Integer g : godine) {
            CheckMenuItem item = new CheckMenuItem(String.valueOf(g));
            item.setOnAction(e -> {
                if (item.isSelected()) selectedGodineStudija.add(g);
                else selectedGodineStudija.remove(g);
                applyAllFilters();
            });
            menuGodineStudija.getItems().add(item);
        }
    }

    private void initStatusPrijaveFilter() {
        Set<String> statusi = masterList.stream()
                .map(p -> p.getStatusPrijave() != null ? p.getStatusPrijave().getNaziv() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));

        menuStatusPrijave.getItems().clear();

        for (String status : statusi) {
            CheckMenuItem item = new CheckMenuItem(status);
            item.setOnAction(e -> {
                if (item.isSelected()) selectedStatusiPrijave.add(status);
                else selectedStatusiPrijave.remove(status);
                applyAllFilters();
            });
            menuStatusPrijave.getItems().add(item);
        }
    }

    private void initStudentMap() {
        studentMap = studentDAO.dohvatiSveStudente()
                .stream()
                .collect(Collectors.toMap(Student::getIdStudent, s -> s));
    }

    private void setupRowDoubleClick() {
        tblPrijave.setRowFactory(tv -> {
            TableRow<Prijava> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty() && e.getClickCount() == 2) {
                    otvoriDetalje(row.getItem());
                }
            });
            return row;
        });
    }

    // ✅ ISPRAVKA: više nema new Stage() ovdje
    @FXML
    private void onNovaPrijava() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/novi-student.fxml")
            );
            Parent root = loader.load();

            NoviStudentController controller = loader.getController();
            controller.setPreviousView("prijave"); // (nije obavezno, ali korisno)

            Stage currentStage = (Stage) tblPrijave.getScene().getWindow();
            currentStage.setScene(new Scene(root));
            currentStage.setMaximized(true);
            currentStage.setResizable(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void otvoriDetalje(Prijava prijava) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/detalji-prijave.fxml"));
            Scene scene = new Scene(loader.load());

            DetaljiPrijaveController controller = loader.getController();
            controller.setPrijava(prijava);

            Stage stage = new Stage(); // ovo može ostati (popup)
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

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    public void onRefresh() {
        masterList.setAll(prijavaDAO.dohvatiSvePrijave());
        applyAllFilters();
    }
}
