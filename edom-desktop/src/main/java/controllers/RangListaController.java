package controllers;

import api.ApiException;
import api.RangListaApi;
import ba.unze.edom.common.dto.RangListaAdminDTO;
import ba.unze.edom.common.dto.RangRedDTO;
import dto.RangListaDTO;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import service.export.RangListaHtmlExportService;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class RangListaController {

    @FXML private Label lblGodina;
    @FXML private ToggleButton btnBrucosi;
    @FXML private ToggleButton btnVisi;
    @FXML private Menu menuKolone;

    @FXML private TableView<RangRedDTO> tblRangLista;
    @FXML private TableColumn<RangRedDTO, Integer> colRedniBroj;
    @FXML private TableColumn<RangRedDTO, Integer> colIdPrijave;
    @FXML private TableColumn<RangRedDTO, String> colImePrezime;
    @FXML private TableColumn<RangRedDTO, Integer> colGodinaStudija;
    @FXML private TableColumn<RangRedDTO, Double> colUspjeh;
    @FXML private TableColumn<RangRedDTO, Double> colUdaljenost;
    @FXML private TableColumn<RangRedDTO, Double> colPrimanja;
    @FXML private TableColumn<RangRedDTO, Double> colBranioci;
    @FXML private TableColumn<RangRedDTO, Double> colDodatni;
    @FXML private TableColumn<RangRedDTO, Double> colUkupniBodovi;
    @FXML private TableColumn<RangRedDTO, Void> colDetalji;

    private RangListaAdminDTO lista;

    @FXML
    public void initialize() {
        initKolone();
        initMeniKolona();
        initGrupe();
        ucitaj();
    }

    // ---------- kolone ----------

    private void initKolone() {
        colRedniBroj.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().redniBroj()));
        colIdPrijave.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().idPrijava()));
        colImePrezime.setCellValueFactory(c -> new SimpleStringProperty(punoIme(c.getValue())));
        colGodinaStudija.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().godinaStudija()));

        brojKolona(colUspjeh, RangRedDTO::uspjeh);
        brojKolona(colUdaljenost, RangRedDTO::udaljenost);
        brojKolona(colPrimanja, RangRedDTO::primanja);
        brojKolona(colBranioci, RangRedDTO::branioci);
        brojKolona(colDodatni, RangRedDTO::dodatni);
        brojKolona(colUkupniBodovi, RangRedDTO::ukupno);

        // ukupno: upozorenje ako prijava ima neispravne podatke
        colUkupniBodovi.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setTooltip(null);
                RangRedDTO r = (empty || getTableRow() == null) ? null : getTableRow().getItem();
                if (r == null) {
                    setText(null);
                } else if (r.greska() != null) {
                    setText("⚠");
                    setTooltip(new Tooltip(r.greska()));
                } else {
                    setText(String.format("%.2f", v));
                }
            }
        });

        colDetalji.setCellFactory(tc -> new TableCell<>() {
            private final Button btn = new Button("Detalji");
            {
                btn.getStyleClass().add("det-btn");
                btn.setOnAction(e -> {
                    RangRedDTO r = getTableRow() != null ? getTableRow().getItem() : null;
                    if (r != null) otvoriDetalje(r.idPrijava());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        tblRangLista.getSelectionModel().setCellSelectionEnabled(false);
    }

    private void brojKolona(TableColumn<RangRedDTO, Double> kolona, Function<RangRedDTO, Double> polje) {
        kolona.setCellValueFactory(c -> new SimpleObjectProperty<>(polje.apply(c.getValue())));
        kolona.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f", v));
            }
        });
    }

    /** LinkedHashMap čuva redoslijed; Map.of ga je ranije miješao. */
    private void initMeniKolona() {
        Map<String, TableColumn<RangRedDTO, ?>> kolone = new LinkedHashMap<>();
        kolone.put("Redni broj", colRedniBroj);
        kolone.put("ID prijave", colIdPrijave);
        kolone.put("Ime i prezime", colImePrezime);
        kolone.put("Godina studija", colGodinaStudija);
        kolone.put("Uspjeh", colUspjeh);
        kolone.put("Udaljenost", colUdaljenost);
        kolone.put("Primanja", colPrimanja);
        kolone.put("Branioci", colBranioci);
        kolone.put("Dodatni", colDodatni);
        kolone.put("Ukupno", colUkupniBodovi);

        menuKolone.getItems().clear();
        kolone.forEach((naziv, kolona) -> {
            CheckMenuItem item = new CheckMenuItem(naziv);
            item.setSelected(true);
            item.selectedProperty().addListener((obs, o, n) -> kolona.setVisible(n));
            menuKolone.getItems().add(item);
        });
    }

    private void initGrupe() {
        ToggleGroup grupa = new ToggleGroup();
        btnBrucosi.setToggleGroup(grupa);
        btnVisi.setToggleGroup(grupa);
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
        Task<RangListaAdminDTO> task = new Task<>() {
            @Override
            protected RangListaAdminDTO call() {
                return RangListaApi.dohvati(null);
            }
        };

        task.setOnSucceeded(e -> {
            lista = task.getValue();
            lblGodina.setText(lista.akademskaGodina() != null
                    ? "Akademska godina " + lista.akademskaGodina()
                    : "Nema prijava");
            btnBrucosi.setText("Brucoši (" + lista.brucosi().size() + ")");
            btnVisi.setText("Više godine (" + lista.visegodisnji().size() + ")");
            prikaziGrupu();
        });
        task.setOnFailed(e -> prikaziGresku(task.getException(), "Greška pri učitavanju rang liste."));

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void prikaziGrupu() {
        if (lista == null) return;
        List<RangRedDTO> redovi = btnBrucosi.isSelected() ? lista.brucosi() : lista.visegodisnji();
        tblRangLista.setItems(FXCollections.observableArrayList(redovi));
        tblRangLista.getSortOrder().clear();
    }

    @FXML
    private void onRefresh() {
        ucitaj();
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
    public void onExportHtmlPdf(ActionEvent actionEvent) {
        if (lista == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Sačuvaj rang listu");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF fajl", "*.pdf"));
        fc.setInitialFileName("Rang_lista_" + lista.godina() + ".pdf");

        File file = fc.showSaveDialog(tblRangLista.getScene().getWindow());
        if (file == null) return;

        try {
            new RangListaHtmlExportService().exportSplitData(
                    zaExport(lista.brucosi()), zaExport(lista.visegodisnji()), file);
            new Alert(Alert.AlertType.INFORMATION, "PDF uspješno kreiran!", ButtonType.OK).showAndWait();
        } catch (RuntimeException ex) {
            prikaziGresku(ex, "Greška pri izvozu rang liste.");
        }
    }

    private List<RangListaDTO> zaExport(List<RangRedDTO> redovi) {
        return redovi.stream()
                .filter(r -> r.greska() == null)
                .map(r -> {
                    RangListaDTO dto = new RangListaDTO();
                    dto.addKolona("#", String.valueOf(r.redniBroj()));
                    dto.addKolona("Prezime (ime oca) i ime studenta", punoIme(r));
                    dto.addKolona("Bodovi", String.format("%.2f", r.ukupno()));
                    return dto;
                })
                .toList();
    }

    // ---------- pomoćne ----------

    /** Zvanični format: Prezime (ime oca) Ime */
    private String punoIme(RangRedDTO r) {
        String otac = r.imeOca() != null && !r.imeOca().isBlank() ? " (" + r.imeOca() + ") " : " ";
        return nullSafe(r.prezime()) + otac + nullSafe(r.ime());
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