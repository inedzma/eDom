package controllers;

import dao.StudentDAO;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import model.Student;
import service.export.StudentExportService;
import service.export.StudentHtmlExportService;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class studentiController {

    @FXML private TableView<Student> tblStudenti;

    @FXML private TableColumn<Student, Integer> colId;
    @FXML private TableColumn<Student, String> colIme;
    @FXML private TableColumn<Student, String> colPrezime;
    @FXML private TableColumn<Student, String> colIndeks;
    @FXML private TableColumn<Student, String> colFakultet;
    @FXML private TableColumn<Student, Integer> colGodina;
    @FXML private TableColumn<Student, Double> colProsjek;
    @FXML private TableColumn<Student, String> colStatus;

    @FXML private javafx.scene.control.Menu menuFakulteti;
    @FXML private javafx.scene.control.Menu menuStatus;
    @FXML private javafx.scene.control.Menu menuGodina;

    @FXML private TextField txtSearch;

    private final StudentDAO studentDAO = new StudentDAO();

    private ObservableList<Student> masterList;
    private FilteredList<Student> filteredList;

    private final Set<String> selectedFakulteti = new HashSet<>();
    private final Set<String> selectedStatusi = new HashSet<>();
    private final Set<Integer> selectedGodine = new HashSet<>();

    @FXML
    public void initialize() {

        colId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getIdStudent()).asObject());
        colIme.setCellValueFactory(cell -> new SimpleStringProperty(nullSafe(cell.getValue().getIme())));
        colPrezime.setCellValueFactory(cell -> new SimpleStringProperty(nullSafe(cell.getValue().getPrezime())));
        colIndeks.setCellValueFactory(cell -> new SimpleStringProperty(nullSafe(cell.getValue().getBrojIndeksa())));
        colFakultet.setCellValueFactory(cell -> new SimpleStringProperty(nullSafe(cell.getValue().getFakultet())));
        colGodina.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getGodinaStudija()).asObject());
        colProsjek.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getProsjek()).asObject());
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getSocijalniStatus() != null
                        ? nullSafe(cell.getValue().getSocijalniStatus().getNaziv())
                        : ""
        ));

        masterList = FXCollections.observableArrayList(studentDAO.dohvatiSveStudente());

        initFakultetiFilter();
        initStatusFilter();
        initGodinaFilter();

        setupFilteringAndSorting();
    }

    @FXML
    private void sortImeAZ() {
        colIme.setSortType(TableColumn.SortType.ASCENDING);
        tblStudenti.getSortOrder().setAll(colIme);
    }

    @FXML
    private void sortImeZA() {
        colIme.setSortType(TableColumn.SortType.DESCENDING);
        tblStudenti.getSortOrder().setAll(colIme);
    }

    @FXML
    private void sortIdUzlazno() {
        colId.setSortType(TableColumn.SortType.ASCENDING);
        tblStudenti.getSortOrder().setAll(colId);
    }

    @FXML
    private void sortIdSilazno() {
        colId.setSortType(TableColumn.SortType.DESCENDING);
        tblStudenti.getSortOrder().setAll(colId);
    }

    @FXML
    private void sortProsjekDesc() {
        colProsjek.setSortType(TableColumn.SortType.DESCENDING);
        tblStudenti.getSortOrder().setAll(colProsjek);
    }

    @FXML
    private void sortProsjekAsc() {
        colProsjek.setSortType(TableColumn.SortType.ASCENDING);
        tblStudenti.getSortOrder().setAll(colProsjek);
    }

    private void initFakultetiFilter() {

        Set<String> fakulteti = masterList.stream()
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

    private void applyAllFilters(){
        String q = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase();

        filteredList.setPredicate(s -> {
            boolean searchOk = q.isEmpty()
                    || nullSafe(s.getIme()).toLowerCase().contains(q)
                    || nullSafe(s.getPrezime()).toLowerCase().contains(q)
                    || (nullSafe(s.getIme()) + " " + nullSafe(s.getPrezime())).toLowerCase().contains(q)
                    || nullSafe(s.getBrojIndeksa()).toLowerCase().contains(q)
                    || nullSafe(s.getFakultet()).toLowerCase().contains(q)
                    || (s.getSocijalniStatus() != null && nullSafe(s.getSocijalniStatus().getNaziv()).toLowerCase().contains(q))
                    || String.valueOf(s.getGodinaStudija()).contains(q)
                    || String.valueOf(s.getIdStudent()).contains(q);

            boolean fakultetOk = selectedFakulteti.isEmpty() || selectedFakulteti.contains(s.getFakultet());

            boolean statusOk = selectedStatusi.isEmpty() || (s.getSocijalniStatus() != null && selectedStatusi.contains(s.getSocijalniStatus().getNaziv()));

            boolean godinaOk = selectedGodine.isEmpty() || selectedGodine.contains(s.getGodinaStudija());

            return searchOk && fakultetOk && statusOk && godinaOk;
        });
    }

    private void initStatusFilter() {
        Set<String> statusi = masterList.stream()
                .map(Student::getSocijalniStatus)
                .filter(Objects::nonNull)
                .map(status -> status.getNaziv())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new)); // ✅ sortirano

        menuStatus.getItems().clear();

        for (String s : statusi) {
            CheckMenuItem item = new CheckMenuItem(s);
            item.setOnAction(e -> {
                if (item.isSelected()) selectedStatusi.add(s);
                else selectedStatusi.remove(s);
                applyAllFilters();
            });
            menuStatus.getItems().add(item);
        }
    }

    private void initGodinaFilter() {
        Set<Integer> godine = masterList.stream()
                .map(Student::getGodinaStudija)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new)); // ✅ sortirano

        menuGodina.getItems().clear();

        for (Integer g : godine) {
            CheckMenuItem item = new CheckMenuItem(String.valueOf(g));
            item.setOnAction(e -> {
                if (item.isSelected()) selectedGodine.add(g);
                else selectedGodine.remove(g);
                applyAllFilters();
            });
            menuGodina.getItems().add(item);
        }
    }

    private void setupFilteringAndSorting(){
        filteredList = new FilteredList<>(masterList, s -> true);

        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> applyAllFilters());

        SortedList<Student> sorted = new SortedList<>(filteredList);
        sorted.comparatorProperty().bind(tblStudenti.comparatorProperty());

        tblStudenti.setItems(sorted);
    }

    @FXML
    private void onExportPdf() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Spremi PDF (iText)");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        java.io.File file = fileChooser.showSaveDialog(tblStudenti.getScene().getWindow());
        if (file != null) {
            // Koristi tvoj postojeći StudentPdfExportService
            StudentExportService service = new StudentExportService();
            service.exportData(filteredList.stream().toList(), file); // koristi samo filtrirane studente
        }
    }

    @FXML
    private void onExportHtmlPdf() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Spremi PDF (HTML)");
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
        java.io.File file = fileChooser.showSaveDialog(tblStudenti.getScene().getWindow());
        if (file != null) {
            // Koristi HTML+CSS servis
            StudentHtmlExportService service = new StudentHtmlExportService();
            service.exportData(filteredList.stream().toList(), file); // filtrirani studenti
        }
    }

    @FXML
    private void onRefresh() {
        // Dohvati nove studente iz baze i zamijeni trenutni masterList
        masterList.setAll(studentDAO.dohvatiSveStudente());
        // Ponovno primijeni filtere da se lista odmah ažurira
        applyAllFilters();
    }



    private String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
