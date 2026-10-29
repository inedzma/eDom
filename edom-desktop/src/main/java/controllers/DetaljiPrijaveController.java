package controllers;

import api.ApiException;
import api.PostavkeApi;
import api.PrijavaApi;
import ba.unze.edom.common.dto.*;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Detalji prijave: zaglavlje sa sažetkom, bodovanje i dokumenti lijevo, status i podaci desno. */
public class DetaljiPrijaveController {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy.");
    private static final String ODOBRENO = "odobreno";
    private static final String NA_CEKANJU = "na čekanju";

    /** Šta se desi kad se izabere status — prikazuje se ispod padajuće liste. */
    private static final Map<String, String> POMOC_STATUS = Map.of(
            "odobreno", "Prijava ulazi na rang listu. Student dobija e-mail.",
            "na čekanju", "Student nema dokaz o uslovu za upis – ide na listu čekanja i dobija e-mail.",
            "nepotpuna", "Upišite u napomenu šta fali. Student dobija e-mail i može dopuniti prijavu na webu.",
            "odbijeno", "Upišite razlog odbijanja. Student ga dobija na e-mail.",
            "na pregledu", "Prijava se vraća na pregled.",
            "zaključeno", "Student je uselio u dom.",
            "iseljen", "Student više nije u domu. Iseljenje sa sobom radite u Sobama (dugme „Iseli“).");

    // zaglavlje
    @FXML private Label lblNaslov;
    @FXML private Label lblInfo;
    @FXML private Label lblPrijavaId;
    @FXML private Label lblStatus;
    @FXML private HBox boxStatistika;

    // lijevo
    @FXML private VBox lijevaKolona;

    // desno
    @FXML private ComboBox<String> cmbStatusPrijave;
    @FXML private Label lblStatusPomoc;
    @FXML private TextArea txtNapomenaAdmina;
    @FXML private Button btnSacuvajStatus;
    @FXML private VBox napomenaMjesto;
    @FXML private GridPane gridPodaci;

    private final VBox bodovanjeMjesto = new VBox();
    private final VBox dokumentiMjesto = new VBox();

    private PrijavaDetaljiDTO prijava;

    @FXML
    public void initialize() {
        lijevaKolona.getChildren().setAll(bodovanjeMjesto, dokumentiMjesto);

        cmbStatusPrijave.setCellFactory(l -> statusCelija());
        cmbStatusPrijave.setButtonCell(statusCelija());
        cmbStatusPrijave.valueProperty().addListener((o, a, n) -> {
            String pomoc = n == null ? null : POMOC_STATUS.get(n);
            lblStatusPomoc.setText(pomoc == null ? "" : pomoc);
            lblStatusPomoc.setVisible(pomoc != null);
            lblStatusPomoc.setManaged(pomoc != null);
        });
    }

    // =====================================================================
    //  UČITAVANJE I PRIKAZ
    // =====================================================================

    public void ucitaj(int idPrijave) {
        lblNaslov.setText("Učitavanje prijave #" + idPrijave + "…");
        izvrsi(() -> PrijavaApi.detalji(idPrijave), null);
    }

    private void prikazi(PrijavaDetaljiDTO p) {
        this.prijava = p;

        // zaglavlje
        Integer g = p.akademskaGodina();
        lblPrijavaId.setText("PRIJAVA #" + p.idPrijava() + (g != null ? "   ·   " + g + "/" + (g + 1) : ""));
        lblNaslov.setText(nullSafe(p.ime()) + " " + nullSafe(p.prezime()));
        lblInfo.setText(spoji("   ·   ",
                p.brojIndeksa(),
                p.fakultet() != null ? p.fakultet().replace(" UNZE", "") : null,
                godinaTekst(p.godinaStudija()),
                p.datumPrijave() != null ? "podnesena " + p.datumPrijave().format(DATUM) : null));

        String status = p.status() != null ? p.status() : "nepoznat";
        lblStatus.setText(velikoPrvo(status));
        lblStatus.getStyleClass().setAll("label", "pr-status", prijaveController.statusKlasa(status));

        prikaziStatistiku(p);
        popuniStatus(p);
        prikaziNapomenu(p);
        prikaziPodatke(p);
        ucitajPanele(p.idPrijava());
    }

    private void prikaziStatistiku(PrijavaDetaljiDTO p) {
        RezultatBodovanja r = p.bodovanje();
        List<DokumentDTO> docs = p.dokumenti() != null ? p.dokumenti() : List.of();
        long dostavljeno = docs.stream().filter(d -> Boolean.TRUE.equals(d.dostavljen())).count();

        VBox bodovi;
        VBox provjereno;
        if (r == null) {
            bodovi = stat("BODOVI", "—", "⚠ neispravni podaci", -1);
            if (p.greska() != null) Tooltip.install(bodovi, new Tooltip(p.greska()));
            provjereno = stat("PROVJERENO", "—", "", -1);
        } else {
            bodovi = stat("BODOVI", String.format("%.2f", r.potvrdjeno()),
                    String.format("potvrđeno od mogućih %.2f", r.potencijalno()),
                    r.potencijalno() > 0 ? r.potvrdjeno() / r.potencijalno() : 0);
            long ok = r.stavke().stream().filter(StavkaBodovanja::verifikovan).count();
            int n = r.stavke().size();
            provjereno = stat("PROVJERENO", ok + " / " + n, "stavki bodovanja", n == 0 ? 0 : (double) ok / n);
        }

        VBox dokumenti = stat("DOKUMENTI", dostavljeno + " / " + docs.size(),
                "dostavljeno od priloženih online", docs.isEmpty() ? -1 : (double) dostavljeno / docs.size());
        VBox prosjek = stat("PROSJEK", p.prosjek() != null ? broj(p.prosjek()) : "—",
                p.polozeniIspiti() != null ? p.polozeniIspiti() + " položenih ispita" : "", -1);

        boxStatistika.getChildren().setAll(bodovi, provjereno, dokumenti, prosjek);
    }

    /** Pločica u zaglavlju: naslov, veliki broj, opis i (ako ima) traka. */
    private VBox stat(String naslov, String vrijednost, String opis, double popunjeno) {
        Label n = new Label(naslov);
        n.getStyleClass().add("pr-hero-stat-naslov");
        Label b = new Label(vrijednost);
        b.getStyleClass().add("pr-hero-stat-broj");
        Label o = new Label(opis);
        o.getStyleClass().add("pr-hero-stat-opis");

        VBox v = new VBox(2, n, b, o);
        if (popunjeno >= 0) {
            ProgressBar traka = new ProgressBar(Math.min(1, popunjeno));
            traka.getStyleClass().add("pr-hero-traka");
            traka.setMaxWidth(Double.MAX_VALUE);
            VBox.setMargin(traka, new Insets(8, 0, 0, 0));
            v.getChildren().add(traka);
        }
        v.getStyleClass().add("pr-hero-stat");
        v.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(v, Priority.ALWAYS);
        return v;
    }

    private void prikaziPodatke(PrijavaDetaljiDTO p) {
        gridPodaci.getChildren().clear();
        int red = 0;
        red = podatak(red, "Broj indeksa", p.brojIndeksa());
        red = podatak(red, "Fakultet", p.fakultet());
        red = podatak(red, "Godina studija", godinaTekst(p.godinaStudija()));
        if (p.trebaUslov()) red = podatak(red, "Uslov za upis", p.uslovUpisa());
        red = podatak(red, "Prosjek", p.prosjek() != null ? broj(p.prosjek()) : null);
        red = podatak(red, "Položeni ispiti", p.polozeniIspiti() != null ? p.polozeniIspiti().toString() : null);
        red = podatak(red, "Članova domaćinstva",
                p.brojClanovaDomacinstva() != null ? p.brojClanovaDomacinstva().toString() : null);
        red = podatak(red, "Ukupna primanja", p.ukupnaPrimanja() != null ? broj(p.ukupnaPrimanja()) + " KM" : null);
        red = podatak(red, "Udaljenost", p.udaljenostKm() != null ? broj(p.udaljenostKm()) + " km" : null);
        red = podatak(red, "E-mail", p.email());
        podatak(red, "Telefon", p.telefon());
    }

    private int podatak(int red, String naziv, String vrijednost) {
        Label n = new Label(naziv);
        n.getStyleClass().add("pr-sitno");
        Label v = new Label(vrijednost == null || vrijednost.isBlank() ? "—" : vrijednost);
        v.getStyleClass().add("pr-vrijednost");
        v.setWrapText(true);
        gridPodaci.add(n, 0, red);
        gridPodaci.add(v, 1, red);
        return red + 1;
    }

    // =====================================================================
    //  NAPOMENA STUDENTA
    // =====================================================================

    /** Napomena koju je student napisao pri predaji; admin je može ispraviti. */
    private void prikaziNapomenu(PrijavaDetaljiDTO p) {
        Label naslov = new Label("Napomena studenta");
        naslov.getStyleClass().add("pr-kartica-naslov");
        Label opis = new Label("Student je upisuje pri predaji prijave. Administrator je može ispraviti ili dopuniti.");
        opis.getStyleClass().add("pr-sitno");
        opis.setWrapText(true);

        TextArea ta = new TextArea(p.napomena() != null ? p.napomena() : "");
        ta.getStyleClass().add("pr-textarea");
        ta.setPromptText("Student nije ostavio napomenu.");
        ta.setWrapText(true);
        ta.setPrefRowCount(3);

        Button sacuvaj = new Button("Sačuvaj napomenu");
        sacuvaj.getStyleClass().add("pr-btn-sekundarno");
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
        dno.setAlignment(Pos.CENTER_RIGHT);

        VBox kartica = new VBox(10, naslov, opis, ta, dno);
        kartica.getStyleClass().add("pr-kartica");
        napomenaMjesto.getChildren().setAll(kartica);
    }

    // =====================================================================
    //  STATUS
    // =====================================================================

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

        boolean saljeMail = Set.of(ODOBRENO, "odbijeno", "nepotpuna", NA_CEKANJU).contains(novi);
        int id = prijava.idPrijava();
        izvrsi(() -> PrijavaApi.promijeniStatus(id, new PromjenaStatusaDTO(novi, napomena)),
                saljeMail ? "Status je promijenjen. Studentu je poslan e-mail." : "Status je promijenjen.");
    }

    private ListCell<String> statusCelija() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? null : velikoPrvo(s));
            }
        };
    }

    // =====================================================================
    //  PANELI (bodovanje, dokumenti)
    // =====================================================================

    private void ucitajPanele(int idPrijave) {
        uPozadini(() -> Map.entry(PrijavaApi.bodovanje(idPrijave), PostavkeApi.ucitaj()),
                e -> bodovanjeMjesto.getChildren().setAll(new BodovanjePanel(e.getKey(), e.getValue(),
                        () -> izvrsi(() -> PrijavaApi.detalji(idPrijave), null))));
        uPozadini(() -> PrijavaApi.dokumentiSpisak(idPrijave),
                s -> dokumentiMjesto.getChildren().setAll(new DokumentiPanel(idPrijave, s,
                        () -> izvrsi(() -> PrijavaApi.detalji(idPrijave), null))));
    }

    @FXML
    private void onZatvori() {
        ((Stage) lblNaslov.getScene().getWindow()).close();
    }

    // =====================================================================
    //  POMOĆNE
    // =====================================================================

    /** Pozove server u pozadini, prikaže novo stanje prijave i (opcionalno) poruku. */
    private void izvrsi(Supplier<PrijavaDetaljiDTO> poziv, String porukaUspjeha) {
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

    private static String godinaTekst(Integer g) {
        if (g == null) return null;
        return switch (g) {
            case 7 -> "Apsolvent";
            case 8 -> "Postdiplomac";
            default -> g + ". godina";
        };
    }

    private static String broj(BigDecimal b) {
        return b.stripTrailingZeros().toPlainString();
    }

    private static String velikoPrvo(String s) {
        return s == null || s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String spoji(String sep, String... dijelovi) {
        StringJoiner j = new StringJoiner(sep);
        for (String d : dijelovi) if (d != null && !d.isBlank()) j.add(d);
        return j.toString();
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}