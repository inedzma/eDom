package controllers;

import api.ApiException;
import api.PrijavaApi;
import ba.unze.edom.common.dto.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import service.PdfService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DetaljiPrijaveController {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy.");
    private static final String ODOBRENO = "odobreno";

    private static final String NA_CEKANJU = "na čekanju";

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

    // provjera
    @FXML private VBox vboxProvjera;
    @FXML private Button btnSacuvajProvjeru;
    private CheckBox chkUspjeh;
    private CheckBox chkUdaljenost;
    private CheckBox chkPrimanja;
    private final Map<Integer, CheckBox> kriterijChk = new LinkedHashMap<>();

    // status
    @FXML private ComboBox<String> cmbStatusPrijave;
    @FXML private TextArea txtNapomenaAdmina;
    @FXML private Button btnSacuvajStatus;

    private PrijavaDetaljiDTO prijava;

    // novi izgled: bodovanje i dokumenti (prave ih paneli u kodu)
    private final VBox bodovanjeMjesto = new VBox();
    private final VBox dokumentiMjesto = new VBox();
    private final VBox napomenaMjesto = new VBox();

    @FXML
    public void initialize() {
        inicijalizujTabelaBodovanja();
        inicijalizujTabelaDokumenata();
        pripremiNoviIzgled();
    }

    // ---------- učitavanje ----------

    public void ucitaj(int idPrijave) {
        lblNaslov.setText("Učitavanje prijave #" + idPrijave + "…");
        izvrsi(() -> PrijavaApi.detalji(idPrijave), null);
    }

    private void prikazi(PrijavaDetaljiDTO p) {
        this.prijava = p;

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

        popuniProvjeru(p);
        popuniStatus(p);
        ucitajPanele(p.idPrijava());
        prikaziNapomenu(p);
    }

    /** Napomena koju je student napisao pri predaji; admin je može ispraviti. */
    private void prikaziNapomenu(PrijavaDetaljiDTO p) {
        Label naslov = new Label("Napomena studenta");
        naslov.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label opis = new Label("Student je upisuje pri predaji prijave. Administrator je može ispraviti ili dopuniti.");
        opis.setStyle("-fx-text-fill: #6b7a90;");
        opis.setWrapText(true);

        TextArea ta = new TextArea(p.napomena() != null ? p.napomena() : "");
        ta.setPromptText("Student nije ostavio napomenu.");
        ta.setWrapText(true);
        ta.setPrefRowCount(3);

        Button sacuvaj = new Button("Sačuvaj napomenu");
        sacuvaj.getStyleClass().add("btn-primary");
        int id = p.idPrijava();
        sacuvaj.setOnAction(e -> {
            String tekst = ta.getText();
            if (tekst != null && tekst.trim().length() > 500) {
                info("Napomena može imati najviše 500 znakova.");
                return;
            }
            uPozadini(() -> { PrijavaApi.sacuvajNapomenu(id, tekst); return true; },
                    x -> info("Napomena je sačuvana."));
        });

        HBox dno = new HBox(sacuvaj);
        dno.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        VBox kartica = new VBox(8, naslov, opis, ta, dno);
        kartica.setPadding(new Insets(16));
        kartica.setStyle("-fx-background-color: white; -fx-background-radius: 14; "
                + "-fx-border-color: #e3e8ef; -fx-border-radius: 14;");
        napomenaMjesto.getChildren().setAll(kartica);
    }

    // ---------- provjera stavki ----------

    private void popuniProvjeru(PrijavaDetaljiDTO p) {
        vboxProvjera.getChildren().clear();
        kriterijChk.clear();

        boolean moze = p.mozeProvjeravati();

        chkUspjeh     = provjeraChk("Uspjeh (prosjek i položeni ispiti)", p.uspjehVerifikovan(), moze);
        chkUdaljenost = provjeraChk("Udaljenost (CIPS)", p.udaljenostVerifikovana(), moze);
        chkPrimanja   = provjeraChk("Primanja domaćinstva (kućna lista i dokumenti članova)",
                p.primanjaVerifikovana(), moze);
        vboxProvjera.getChildren().addAll(chkUspjeh, chkUdaljenost, chkPrimanja);

        if (p.kriteriji() != null) {
            for (KriterijStanjeDTO k : p.kriteriji()) {
                String tekst = k.naziv() + (k.parametar() != null
                        ? " (" + k.nazivParametra() + ": " + k.parametar().stripTrailingZeros().toPlainString() + ")"
                        : "");
                CheckBox c = provjeraChk(tekst, k.verifikovan(), moze);
                kriterijChk.put(k.idPrijavaKriterij(), c);
                vboxProvjera.getChildren().add(c);
            }
        }

        btnSacuvajProvjeru.setDisable(!moze);
        if (!moze) {
            Label info = new Label("Provjera je moguća samo dok je prijava na pregledu.");
            info.setWrapText(true);
            info.getStyleClass().add("muted");
            vboxProvjera.getChildren().add(info);
        }
    }

    private CheckBox provjeraChk(String tekst, boolean oznaceno, boolean omoguceno) {
        CheckBox c = new CheckBox(tekst);
        c.setWrapText(true);
        c.setSelected(oznaceno);
        c.setDisable(!omoguceno);
        return c;
    }

    @FXML
    private void onSacuvajProvjeru() {
        if (prijava == null) return;

        List<KriterijVerifikacijaDTO> kriteriji = new ArrayList<>();
        kriterijChk.forEach((id, chk) -> kriteriji.add(new KriterijVerifikacijaDTO(id, chk.isSelected())));

        VerifikacijaDTO z = new VerifikacijaDTO(
                chkUspjeh.isSelected(), chkUdaljenost.isSelected(), chkPrimanja.isSelected(), kriteriji);

        int id = prijava.idPrijava();
        izvrsi(() -> PrijavaApi.verifikuj(id, z), "Provjera je sačuvana.");
    }



    private void popuniStatus(PrijavaDetaljiDTO p) {
        List<String> dozvoljeni = p.dozvoljeniStatusi() != null ? p.dozvoljeniStatusi() : List.of();
        boolean ima = !dozvoljeni.isEmpty();

        cmbStatusPrijave.setItems(FXCollections.observableArrayList(dozvoljeni));
        cmbStatusPrijave.setValue(null);
        cmbStatusPrijave.setPromptText(ima ? "Odaberite novi status" : "Nema dozvoljenih promjena");
        cmbStatusPrijave.setDisable(!ima);
        btnSacuvajStatus.setDisable(!ima);
        txtNapomenaAdmina.setText(nullSafe(p.napomenaAdmina()));
    }

    @FXML
    private void onSacuvajStatus() {
        if (prijava == null) return;

        String novi = cmbStatusPrijave.getValue();
        if (novi == null) {
            info("Odaberite novi status.");
            return;
        }

        String napomena = txtNapomenaAdmina.getText();
        boolean trebaNapomena = "odbijeno".equals(novi) || "nepotpuna".equals(novi);
        if (trebaNapomena && (napomena == null || napomena.isBlank())) {
            info("nepotpuna".equals(novi)
                    ? "Upišite u napomenu šta student treba dopuniti — dobija to na e-mail."
                    : "Unesite razlog odbijanja u napomenu — student ga dobija na e-mail.");
            return;
        }

        String pitanje = "Promijeniti status prijave u '" + novi + "'?";
        RezultatBodovanja r = prijava.bodovanje();
        boolean prihvacena = ODOBRENO.equals(novi) || NA_CEKANJU.equals(novi);
        if (prihvacena && r != null && r.cekaVerifikaciju()) {
            pitanje += String.format(
                    "%n%nPažnja: %.2f bodova nije provjereno i neće biti uračunato.%nPrijava se boduje sa %.2f bodova.",
                    r.potencijalno() - r.potvrdjeno(), r.potvrdjeno());
        }
        if (NA_CEKANJU.equals(novi)) {
            pitanje += "\n\nStudent ne ispunjava uslov za upis naredne godine i ide na listu čekanja."
                    + "\nKad donese uvjerenje, odobrite prijavu (ili \"Dokaz ✓\" na rang listi).";
        } else if (ODOBRENO.equals(novi) && prijava.trebaUslov()) {
            pitanje += "\n\nOdobravanjem potvrđujete da student ispunjava uslov za upis naredne godine.";
        }
        if ("nepotpuna".equals(novi)) {
            pitanje += "\n\nStudent dobija e-mail s napomenom i može dodati dokumente na webu."
                    + "\nKad pošalje dopunu, prijava se vraća na pregled.";
        } else if ("iseljen".equals(novi)) {
            pitanje += "\n\nStudent više nije u domu. Iseljenje sa sobom radite u Sobama (dugme \"Iseli\").";
        }

        Alert potvrda = new Alert(Alert.AlertType.CONFIRMATION, pitanje);
        potvrda.setHeaderText(null);
        if (potvrda.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        boolean saljeMail = java.util.Set.of(ODOBRENO, "odbijeno", "nepotpuna", NA_CEKANJU).contains(novi);
        int id = prijava.idPrijava();
        izvrsi(() -> PrijavaApi.promijeniStatus(id, new PromjenaStatusaDTO(novi, napomena)),
                saljeMail ? "Status je promijenjen. Studentu je poslan e-mail." : "Status je promijenjen.");
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
        task.setOnFailed(e -> greska(task.getException(), "Greška pri otvaranju dokumenta."));
        pokreni(task);
    }

    @FXML
    private void onZatvori() {
        ((Stage) lblNaslov.getScene().getWindow()).close();
    }

    // ---------- pomoćne ----------

    /** Pozove server u pozadini, prikaže novo stanje prijave i (opcionalno) poruku. */
    private void izvrsi(Supplier<PrijavaDetaljiDTO> poziv, String porukaUspjeha) {
        btnSacuvajProvjeru.setDisable(true);
        btnSacuvajStatus.setDisable(true);

        Task<PrijavaDetaljiDTO> task = new Task<>() {
            @Override
            protected PrijavaDetaljiDTO call() {
                return poziv.get();
            }
        };
        task.setOnSucceeded(e -> {
            prikazi(task.getValue());
            if (porukaUspjeha != null) info(porukaUspjeha);
        });
        task.setOnFailed(e -> {
            if (prijava != null) prikazi(prijava);   // vrati dugmad u ispravno stanje
            greska(task.getException(), "Greška u komunikaciji sa serverom.");
        });
        pokreni(task);
    }

    private String podaciTekst(PrijavaDetaljiDTO p) {
        List<String> redovi = new ArrayList<>(List.of(
                "Indeks: " + str(p.brojIndeksa()),
                "Fakultet: " + str(p.fakultet()),
                "Godina studija: " + str(p.godinaStudija())
        ));
        if (p.trebaUslov()) {
            redovi.add("Uslov za upis: " + str(p.uslovUpisa()));
        }
        redovi.addAll(List.of(
                "Prosjek: " + str(p.prosjek()),
                "Položeni ispiti: " + str(p.polozeniIspiti()),
                "Članova domaćinstva: " + str(p.brojClanovaDomacinstva()),
                "Ukupna primanja: " + (p.ukupnaPrimanja() != null ? p.ukupnaPrimanja() + " KM" : "—"),
                "Udaljenost: " + (p.udaljenostKm() != null ? p.udaljenostKm() + " km" : "—"),
                "Email: " + str(p.email()),
                "Telefon: " + str(p.telefon())
        ));
        if (p.napomena() != null && !p.napomena().isBlank()) {
            redovi.add("Napomena studenta: " + p.napomena());
        }
        return String.join("\n", redovi);
    }

    private void setStatusPillStyle(String status) {
        lblStatus.getStyleClass().removeAll("status-info", "status-success", "status-warning", "status-danger");
        String s = status.toLowerCase();
        if (s.contains("odob"))       lblStatus.getStyleClass().add("status-success");
        else if (s.contains("odbij")) lblStatus.getStyleClass().add("status-danger");
        else if (s.contains("čekanju") || s.contains("nepotpun")) lblStatus.getStyleClass().add("status-warning");
        else if (s.contains("iselj")) lblStatus.getStyleClass().add("status-info");
        else if (s.contains("zaklj")) lblStatus.getStyleClass().add("status-warning");
        else                          lblStatus.getStyleClass().add("status-info");
    }

    private void pokreni(Task<?> task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void greska(Throwable ex, String opsta) {
        if (!(ex instanceof ApiException)) ex.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR, ex instanceof ApiException ? ex.getMessage() : opsta);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void info(String poruka) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, poruka);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private String str(Object o) {
        return o == null ? "—" : o.toString();
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    /** Lijeva kartica: umjesto starih tabela dva panela; stara kartica "Provjera dokumenata" se sakrije. */
    private void pripremiNoviIzgled() {
        VBox lijevo = (VBox) tblBodovanje.getParent();
        VBox sadrzaj = new VBox(18, napomenaMjesto, bodovanjeMjesto, dokumentiMjesto);
        sadrzaj.setPadding(new Insets(2));
        ScrollPane sp = new ScrollPane(sadrzaj);
        sp.setFitToWidth(true);
        VBox.setVgrow(sp, Priority.ALWAYS);
        lijevo.getChildren().setAll(sp);

        Node provjera = vboxProvjera.getParent();
        provjera.setVisible(false);
        provjera.setManaged(false);
    }

    private void ucitajPanele(int idPrijave) {
        uPozadini(() -> Map.entry(PrijavaApi.bodovanje(idPrijave), api.PostavkeApi.ucitaj()),
                e -> bodovanjeMjesto.getChildren().setAll(new BodovanjePanel(e.getKey(), e.getValue(),
                        () -> izvrsi(() -> PrijavaApi.detalji(idPrijave), null))));
        uPozadini(() -> PrijavaApi.dokumentiSpisak(idPrijave),
                s -> dokumentiMjesto.getChildren().setAll(new DokumentiPanel(idPrijave, s,
                        () -> ucitajPanele(idPrijave))));
    }

    private <T> void uPozadini(Supplier<T> posao, Consumer<T> uspjeh) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return posao.get();
            }
        };
        task.setOnSucceeded(e -> uspjeh.accept(task.getValue()));
        task.setOnFailed(e -> greska(task.getException(), "Greška pri učitavanju."));
        pokreni(task);
    }
}