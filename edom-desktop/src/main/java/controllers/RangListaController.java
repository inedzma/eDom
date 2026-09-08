package controllers;

import dao.PrijavaDAO;
import dto.RangListaDTO;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import model.Prijava;
import service.export.RangListaHtmlExportService;
import util.BodoviUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RangListaController {

    @FXML private TableView<Prijava> tblRangLista;
    @FXML private TableColumn<Prijava, Number> colRedniBroj;
    @FXML private TableColumn<Prijava, Integer> colIdPrijave;
    @FXML private TableColumn<Prijava, String> colImePrezime;
    @FXML private TableColumn<Prijava, Double> colUkupniBodovi;
    @FXML private TableColumn<Prijava, Double> colGodinaStudija;
    @FXML private TableColumn<Prijava, Double> colProsjek;
    @FXML private TableColumn<Prijava, Double> colOsvojeneNagrade;
    @FXML private TableColumn<Prijava, Double> colSocijalniStatus;
    @FXML private TableColumn<Prijava, Double> colUdaljenost;
    @FXML private TableColumn<Prijava, Double> colDodatniBodovi;
    @FXML private TableColumn<Prijava, Void> colDetalji;
    @FXML private Menu menuKolone;

    private final PrijavaDAO prijavaDAO = new PrijavaDAO();
    private ObservableList<Prijava> rangLista;

    @FXML
    public void initialize() {

        Map<String, TableColumn<Prijava, ?>> koloneMap = Map.of(
                "Redni broj", colRedniBroj,
                "ID prijave", colIdPrijave,
                "Ime i prezime", colImePrezime,
                "Ukupni bodovi", colUkupniBodovi,
                "Godina studija", colGodinaStudija,
                "Prosjek", colProsjek,
                "Osvojene nagrade", colOsvojeneNagrade,
                "Socijalni status", colSocijalniStatus,
                "Udaljenost", colUdaljenost,
                "Dodatni bodovi", colDodatniBodovi
        );

        menuKolone.getItems().clear();
        koloneMap.forEach((naziv, kolona) -> {
            CheckMenuItem item = new CheckMenuItem(naziv);
            item.setSelected(true); // default sve kolone vidljive
            item.selectedProperty().addListener((obs, wasSelected, isSelected) -> kolona.setVisible(isSelected));
            menuKolone.getItems().add(item);
        });


        colRedniBroj.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(tblRangLista.getItems().indexOf(cd.getValue()) + 1)
        );

        colIdPrijave.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getIdPrijava())
        );

        colImePrezime.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getImeStudenta() + " " + cd.getValue().getPrezimeStudenta())
        );

        colGodinaStudija.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getBodoviMap().getOrDefault("godina", 0.0))
        );

        colProsjek.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getBodoviMap().getOrDefault("uspjeh", 0.0))
        );

        colOsvojeneNagrade.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getBodoviMap().getOrDefault("nagrade", 0.0))
        );

        colSocijalniStatus.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getBodoviMap().getOrDefault("socijalni", 0.0))
        );

        colUdaljenost.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getBodoviMap().getOrDefault("udaljenost", 0.0))
        );

        colDodatniBodovi.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getBodoviMap().getOrDefault("dodatni", 0.0))
        );

        colUkupniBodovi.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().getBodoviMap().getOrDefault("ukupno", 0.0))
        );

        colDetalji.setCellFactory(tc -> new TableCell<>() {
            private final Button btn = new Button("Detalji");
            {
                btn.getStyleClass().add("det-btn");
                btn.setOnAction(e -> {
                    Prijava prijava = getTableView().getItems().get(getIndex());
                    otvoriDetalje(prijava);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        rangLista = FXCollections.observableArrayList();
        for (Prijava p : prijavaDAO.dohvatiSvePrijave()) {
            if (p.getStatusPrijave() != null && "odobreno".equalsIgnoreCase(p.getStatusPrijave().getNaziv())) {
                Map<String, Double> bodovi = BodoviUtil.izracunajBodoveZaPrijavu(p);
                p.setBodoviMap(bodovi);
                rangLista.add(p);
            }
        }

        SortedList<Prijava> sorted = new SortedList<>(rangLista);
        sorted.comparatorProperty().bind(tblRangLista.comparatorProperty());
        tblRangLista.setItems(sorted);

        colUkupniBodovi.setSortType(TableColumn.SortType.DESCENDING);
        tblRangLista.getSortOrder().clear();
        tblRangLista.getSortOrder().add(colUkupniBodovi);

        tblRangLista.getSelectionModel().setCellSelectionEnabled(false);
    }

    private void otvoriDetalje(Prijava prijava) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/detalji-prijave.fxml"));
            Scene scene = new Scene(loader.load());
            controllers.DetaljiPrijaveController controller = loader.getController();
            controller.setPrijava(prijava);
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
    public void onExportHtmlPdf(ActionEvent actionEvent) {
        try {
            // 1. Inicijalizacija dvije prazne liste
            List<RangListaDTO> brucosiList = new ArrayList<>();
            List<RangListaDTO> visiList = new ArrayList<>();

            int rbBrucosi = 1;
            int rbVisi = 1;


            for (Prijava p : tblRangLista.getItems()) {

                Double godDbl = p.getBodoviMap().getOrDefault("godina", 0.0);
                int godina = godDbl.intValue();

                if (godina == 1) {
                    brucosiList.add(pripremiDto(p, rbBrucosi++));
                } else {
                    visiList.add(pripremiDto(p, rbVisi++));
                }
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Sačuvaj konačnu rang listu");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF fajl", "*.pdf"));
            fileChooser.setInitialFileName("Konacna_Rang_Lista.pdf");

            File file = fileChooser.showSaveDialog(tblRangLista.getScene().getWindow());

            if (file != null) {
                RangListaHtmlExportService service = new RangListaHtmlExportService();
                service.exportSplitData(brucosiList, visiList, file);

                Alert alert = new Alert(Alert.AlertType.INFORMATION, "PDF uspješno kreiran!", ButtonType.OK);
                alert.showAndWait();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Greška: " + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
        }
    }

    private RangListaDTO pripremiDto(Prijava p, int redniBroj) {
        RangListaDTO dto = new RangListaDTO();

        dto.addKolona("#", String.valueOf(redniBroj));


        String imeOca = (p.getImeRoditelja() != null && !p.getImeRoditelja().isEmpty()) ? " (" + p.getImeRoditelja() + ") " : " ";
        String prezimeIme = p.getPrezimeStudenta() + imeOca + p.getImeStudenta();

        dto.addKolona("Prezime (ime oca) i ime studenta", prezimeIme);

        double bodovi = p.getBodoviMap().getOrDefault("ukupno", 0.0);
        dto.addKolona("Bodovi", String.format("%.2f", bodovi));


        return dto;
    }
}