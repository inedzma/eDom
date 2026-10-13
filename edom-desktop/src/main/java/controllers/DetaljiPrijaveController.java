package controllers;

import api.ApiException;
import api.PrijavaApi;
import ba.unze.edom.common.dto.DokumentDTO;
import ba.unze.edom.common.dto.PrijavaDetaljiDTO;
import ba.unze.edom.common.dto.RezultatBodovanja;
import ba.unze.edom.common.dto.StavkaBodovanja;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import service.PdfService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DetaljiPrijaveController {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

    // zaglavlje
    @FXML private Label lblNaslov;
    @FXML private Label lblInfo;
    @FXML private Label lblPrijavaId;
    @FXML private Label lblStatus;

    // bodovanje
    @FXML private Label lblKompletiranostText;
    @FXML private ProgressBar pbKompletiranost;
    @FXML private TableView<StavkaBodovanja> tblBodovanje;
    @FXML private TableColumn<StavkaBodovanja, String> colStavka;
    @FXML private TableColumn<StavkaBodovanja, String> colObrazlozenje;
    @FXML private TableColumn<StavkaBodovanja, Double> colStavkaBodovi;
    @FXML private TableColumn<StavkaBodovanja, Boolean> colVerifikovan;

    // dokumenti
    @FXML private TableView<DokumentDTO> tblDokumenti;
    @FXML private TableColumn<DokumentDTO, String> colNaziv;
    @FXML private TableColumn<DokumentDTO, String> colVrsta;
    @FXML private TableColumn<DokumentDTO, LocalDate> colDatum;
    @FXML private TableColumn<DokumentDTO, Boolean> colDostavljen;
    @FXML private TableColumn<DokumentDTO, Void> colPregled;
    @FXML private VBox emptyState;

    // desni panel
    @FXML private Label lblUkupniBodoviValue;
    @FXML private Label lblDostavljeniCount;
    @FXML private Label lblStatusObrade;
    @FXML private Label lblPodaci;

    // akcije (zasad onemogućene)
    @FXML private ComboBox<String> cmbStatusPrijave;
    @FXML private Button btnSacuvajStatus;
    @FXML private Button btnPosaljiEmail;
    @FXML private Button btnObrisi;

    private PrijavaDetaljiDTO prijava;

    @FXML
    public void initialize() {
        inicijalizujTabelaBodovanja();
        inicijalizujTabelaDokumenata();
        onemoguciAkcije();
    }

    // ---------- učitavanje ----------

    /** Poziva se iz liste prijava nakon otvaranja prozora. */
    public void ucitaj(int idPrijave) {
        lblNaslov.setText("Učitavanje prijave #" + idPrijave + "…");

        Task<PrijavaDetaljiDTO> task = new Task<>() {
            @Override
            protected PrijavaDetaljiDTO call() {
                return PrijavaApi.detalji(idPrijave);
            }
        };
        task.setOnSucceeded(e -> prikazi(task.getValue()));
        task.setOnFailed(e -> prikaziGresku(task.getException(), "Greška pri učitavanju prijave."));
        pokreni(task);
    }

    private void prikazi(PrijavaDetaljiDTO p) {
        this.prijava = p;

        // zaglavlje
        lblNaslov.setText("Detalji prijave – " + nullSafe(p.ime()) + " " + nullSafe(p.prezime()));
        lblInfo.setText("Akademska godina: " + p.akademskaGodina()
                + (p.datumPrijave() != null ? "   ·   podnesena " + p.datumPrijave().format(DATUM) : ""));
        lblPrijavaId.setText("Prijava #" + p.idPrijava());

        String status = p.status() != null ? p.status() : "nepoznat";
        lblStatus.setText(status);
        lblStatusObrade.setText(status);
        setStatusPillStyle(status);

        lblPodaci.setText(podaciTekst(p));

        // bodovanje
        RezultatBodovanja r = p.bodovanje();
        if (r == null) {
            lblUkupniBodoviValue.setText("—");
            tblBodovanje.getItems().clear();
            pbKompletiranost.setProgress(0);
            lblKompletiranostText.setText(p.greska() != null ? "⚠ " + p.greska() : "");
        } else {
            lblUkupniBodoviValue.setText(String.format("%.2f / %.2f", r.potvrdjeno(), r.potencijalno()));
            tblBodovanje.setItems(FXCollections.observableArrayList(r.stavke()));

            long provjereno = r.stavke().stream().filter(StavkaBodovanja::verifikovan).count();
            int ukupno = r.stavke().size();
            pbKompletiranost.setProgress(ukupno == 0 ? 0 : (double) provjereno / ukupno);
            lblKompletiranostText.setText("Provjereno: " + provjereno + " / " + ukupno + " stavki");
        }

        // dokumenti
        List<DokumentDTO> docs = p.dokumenti() != null ? p.dokumenti() : List.of();
        tblDokumenti.setItems(FXCollections.observableArrayList(docs));
        lblDostavljeniCount.setText(String.valueOf(docs.size()));
        emptyState.setVisible(docs.isEmpty());
        emptyState.setManaged(docs.isEmpty());
    }

    private String podaciTekst(PrijavaDetaljiDTO p) {
        List<String> redovi = new ArrayList<>(List.of(
                "Indeks: " + str(p.brojIndeksa()),
                "Fakultet: " + str(p.fakultet()),
                "Godina studija: " + str(p.godinaStudija()),
                "Prosjek: " + str(p.prosjek()),
                "Položeni ispiti: " + str(p.polozeniIspiti()),
                "Članova domaćinstva: " + str(p.brojClanovaDomacinstva()),
                "Ukupna primanja: " + (p.ukupnaPrimanja() != null ? p.ukupnaPrimanja() + " KM" : "—"),
                "Udaljenost: " + (p.udaljenostKm() != null ? p.udaljenostKm() + " km" : "—"),
                "Email: " + str(p.email()),
                "Telefon: " + str(p.telefon())
        ));
        if (p.napomena() != null && !p.napomena().isBlank()) {
            redovi.add("Napomena: " + p.napomena());
        }
        return String.join("\n", redovi);
    }

    // ---------- tabele ----------

    private void inicijalizujTabelaBodovanja() {
        colStavka.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().naziv()));
        colObrazlozenje.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().obrazlozenje())));

        colStavkaBodovi.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().bodovi()));
        colStavkaBodovi.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("%.2f", v));
            }
        });

        colVerifikovan.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().verifikovan()));
        colVerifikovan.setCellFactory(col -> badgeCell());
    }

    private void inicijalizujTabelaDokumenata() {
        colNaziv.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().naziv())));
        colVrsta.setCellValueFactory(c -> new SimpleStringProperty(nullSafe(c.getValue().vrsta())));

        colDatum.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().datumUpload()));
        colDatum.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : d.format(DATUM));
            }
        });

        colDostavljen.setCellValueFactory(c -> new SimpleObjectProperty<>(
                Boolean.TRUE.equals(c.getValue().dostavljen())));
        colDostavljen.setCellFactory(col -> badgeCell());

        colPregled.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Pregledaj");
            {
                btn.getStyleClass().add("btn-action");
                btn.setOnAction(e -> {
                    DokumentDTO d = getTableRow() != null ? getTableRow().getItem() : null;
                    if (d != null) otvoriDokument(d);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                DokumentDTO d = (empty || getTableRow() == null) ? null : getTableRow().getItem();
                if (d == null) {
                    setGraphic(null);
                    return;
                }
                btn.setDisable(!d.imaSadrzaj());
                btn.getStyleClass().remove("btn-action-disabled");
                if (!d.imaSadrzaj()) btn.getStyleClass().add("btn-action-disabled");
                setGraphic(btn);
            }
        });
    }

    /** Zelena "DA" / crvena "NE" značka, ista kao u starom ekranu. */
    private <S> TableCell<S, Boolean> badgeCell() {
        return new TableCell<>() {
            private final Label badge = new Label();
            { badge.getStyleClass().add("badge"); }

            @Override
            protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setGraphic(null);
                    return;
                }
                badge.setText(v ? "DA" : "NE");
                badge.getStyleClass().removeAll("badge-yes", "badge-no");
                badge.getStyleClass().add(v ? "badge-yes" : "badge-no");
                setGraphic(badge);
            }
        };
    }

    // ---------- dokument ----------

    private void otvoriDokument(DokumentDTO d) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return PrijavaApi.sadrzajDokumenta(prijava.idPrijava(), d.idDokument());
            }
        };
        task.setOnSucceeded(e -> PdfService.prikaziPdf(task.getValue(), d.naziv()));
        task.setOnFailed(e -> prikaziGresku(task.getException(), "Greška pri otvaranju dokumenta."));
        pokreni(task);
    }

    // ---------- akcije: dolaze nakon sastanka s domom ----------

    private void onemoguciAkcije() {
        cmbStatusPrijave.setDisable(true);
        cmbStatusPrijave.setPromptText("Uskoro");
        btnSacuvajStatus.setDisable(true);
        btnPosaljiEmail.setDisable(true);
        btnObrisi.setDisable(true);
    }

    @FXML private void onSacuvajStatus() { /* TODO: PUT /api/prijave/{id}/status */ }
    @FXML private void onPosaljiEmail()  { /* TODO: email šalje server pri promjeni statusa */ }
    @FXML private void onObrisiPrijavu() { /* TODO: ovisi o pravilima čuvanja dokumentacije */ }

    @FXML
    private void onZatvori() {
        ((Stage) lblNaslov.getScene().getWindow()).close();
    }

    // ---------- pomoćne ----------

    private void setStatusPillStyle(String status) {
        lblStatus.getStyleClass().removeAll("status-info", "status-success", "status-warning", "status-danger");
        String s = status.toLowerCase();
        if (s.contains("odob"))       lblStatus.getStyleClass().add("status-success");
        else if (s.contains("odbij")) lblStatus.getStyleClass().add("status-danger");
        else if (s.contains("zaklj")) lblStatus.getStyleClass().add("status-warning");
        else                          lblStatus.getStyleClass().add("status-info");
    }

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

    private String str(Object o) {
        return o == null ? "—" : o.toString();
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }
}