package controllers;

import api.ApiException;
import api.SobeApi;
import ba.unze.edom.common.dto.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sobe i raspored (samo desktop).
 * Kartice soba po spratovima, brojevi kapaciteta, prijedlog rasporeda i otvaranje detalja sobe.
 */
public class SobeController {

    @FXML private ComboBox<Integer> cmbGodina;
    @FXML private ComboBox<String> cmbSpol;
    @FXML private CheckBox chkSlobodne;
    @FXML private HBox boxBrojevi;
    @FXML private HBox boxLegenda;
    @FXML private Label lblStatus;
    @FXML private VBox vboxSpratovi;
    @FXML private Button btnPrijedlog;
    @FXML private Button btnPotvrdi;
    @FXML private Button btnPonisti;

    @FXML private TextField txtPretraga;
    @FXML private ComboBox<String> cmbSprat;
    @FXML private ComboBox<String> cmbRedoslijed;
    @FXML private Label lblPogodaka;

    @FXML private Button btnZahtjevi;

    private static final String SVI_SPRATOVI = "Svi spratovi";
    private static final String OD_VRHA = "Od IV sprata naniže";
    private static final String OD_DNA = "Od I sprata naviše";

    private static final String SVI = "Sve sobe";
    private static final String MUSKE = "Muške sobe";
    private static final String ZENSKE = "Ženske sobe";

    private SobePregledDTO pregled;
    private List<OznakaStanaraDTO> oznake = List.of();

    @FXML
    public void initialize() {
        int sada = tekucaAkademskaGodina();
        cmbGodina.setItems(FXCollections.observableArrayList(sada + 1, sada, sada - 1, sada - 2));
        cmbGodina.setCellFactory(c -> godinaCell());
        cmbGodina.setButtonCell(godinaCell());
        cmbGodina.setValue(sada);
        cmbGodina.valueProperty().addListener((o, a, b) -> ucitaj());

        cmbSpol.setItems(FXCollections.observableArrayList(SVI, MUSKE, ZENSKE));
        cmbSpol.setValue(SVI);
        cmbSpol.valueProperty().addListener((o, a, b) -> prikazi());
        chkSlobodne.selectedProperty().addListener((o, a, b) -> prikazi());

        cmbSprat.setItems(FXCollections.observableArrayList(SVI_SPRATOVI));
        cmbSprat.setValue(SVI_SPRATOVI);
        cmbSprat.valueProperty().addListener((o, a, b) -> prikazi());

        cmbRedoslijed.setItems(FXCollections.observableArrayList(OD_VRHA, OD_DNA));
        cmbRedoslijed.setValue(OD_VRHA);
        cmbRedoslijed.valueProperty().addListener((o, a, b) -> prikazi());

        txtPretraga.textProperty().addListener((o, a, b) -> prikazi());

        napraviLegendu();
        ucitajOznake();
        ucitaj();
    }

    /** Akademska godina počinje u julu: oktobar 2026 -> 2026 (2026/27). */
    private static int tekucaAkademskaGodina() {
        LocalDate d = LocalDate.now();
        return d.getMonthValue() >= 7 ? d.getYear() : d.getYear() - 1;
    }

    private int godina() {
        return cmbGodina.getValue() != null ? cmbGodina.getValue() : tekucaAkademskaGodina();
    }

    // =====================================================================
    //  učitavanje
    // =====================================================================

    private void ucitaj() {
        ucitaj(null);
    }

    /** otvoriSobu: poslije učitavanja ponovo otvori tu sobu (nakon izmjene u detaljima). */
    void ucitaj(Integer otvoriSobu) {
        lblStatus.setText("Učitavam sobe…");
        int g = godina();
        pozovi(() -> SobeApi.pregled(g), p -> {
            pregled = p;
            napuniSpratove();
            osvjeziBrojZahtjeva();
            lblStatus.setText("");
            prikazi();
            if (otvoriSobu != null) {
                p.sobe().stream().filter(s -> s.idSoba().equals(otvoriSobu)).findFirst().ifPresent(this::otvori);
            }
        });
    }

    private void ucitajOznake() {
        pozovi(SobeApi::oznake, o -> oznake = o);
    }

    // =====================================================================
    //  prikaz
    // =====================================================================

    private void prikazi() {
        if (pregled == null) return;
        prikaziBrojeve();

        vboxSpratovi.getChildren().clear();
        String filterSpol = cmbSpol.getValue();
        String filterSprat = cmbSprat.getValue();
        boolean samoSlobodne = chkSlobodne.isSelected();
        String trazi = bezKvacica(txtPretraga.getText());

        Comparator<SobaPregledDTO> poSpratu = Comparator.comparingInt(SobaPregledDTO::redoslijedSprata);
        if (!OD_DNA.equals(cmbRedoslijed.getValue())) poSpratu = poSpratu.reversed();

        Map<String, List<SobaPregledDTO>> grupe = pregled.sobe().stream()
                .sorted(poSpratu.thenComparingInt(SobaPregledDTO::broj))
                .collect(Collectors.groupingBy(SobaPregledDTO::sprat, LinkedHashMap::new, Collectors.toList()));

        int pogodaka = 0;

        for (Map.Entry<String, List<SobaPregledDTO>> e : grupe.entrySet()) {
            String sprat = e.getKey();
            List<SobaPregledDTO> sobe = e.getValue();
            if (filterSprat != null && !SVI_SPRATOVI.equals(filterSprat) && !filterSprat.equals(sprat)) continue;

            List<SobaPregledDTO> vidljive = sobe.stream()
                    .filter(s -> MUSKE.equals(filterSpol) ? "M".equals(s.spol())
                            : ZENSKE.equals(filterSpol) ? "Z".equals(s.spol()) : true)
                    .filter(s -> !samoSlobodne || (s.zaStudente() && s.aktivna() && s.slobodno() > 0))
                    .filter(s -> odgovara(s, trazi))
                    .toList();
            if (vidljive.isEmpty()) continue;
            pogodaka += vidljive.size();

            int mjesta = sobe.stream().filter(s -> s.zaStudente() && s.aktivna()).mapToInt(SobaPregledDTO::kapacitet).sum();
            int slobodno = sobe.stream().filter(s -> s.zaStudente() && s.aktivna()).mapToInt(SobaPregledDTO::slobodno).sum();
            Set<String> spolovi = sobe.stream().map(SobaPregledDTO::spol).filter(Objects::nonNull).collect(Collectors.toSet());

            Label naslov = new Label(sprat);
            naslov.getStyleClass().add("sprat-naslov");
            Label opis = new Label(spolTekst(spolovi) + " · " + mjesta + " mjesta · " + slobodno + " slobodno");
            opis.getStyleClass().add("sprat-opis");
            HBox glava = new HBox(12, naslov, opis);
            glava.setAlignment(Pos.BASELINE_LEFT);

            FlowPane kartice = new FlowPane(10, 10);
            for (SobaPregledDTO s : vidljive) kartice.getChildren().add(kartica(s, trazi));

            VBox blok = new VBox(12, glava, kartice);
            blok.getStyleClass().add("sprat-blok");
            vboxSpratovi.getChildren().add(blok);
        }

        lblPogodaka.setText(trazi.isEmpty() ? "" : pogodaka == 0 ? "Ništa nije pronađeno"
                : pogodaka == 1 ? "1 soba" : pogodaka + " soba");

        if (vboxSpratovi.getChildren().isEmpty()) {
            Label l = new Label(trazi.isEmpty() ? "Nema soba za odabrani filter."
                    : "Nema sobe ni studenta za \"" + txtPretraga.getText().trim() + "\".");
            l.getStyleClass().add("sobe-podnaslov");
            vboxSpratovi.getChildren().add(l);
        }

        boolean imaPrijedloga = pregled.sobe().stream()
                .flatMap(s -> s.stanari().stream())
                .anyMatch(st -> "PREDLOZEN".equals(st.status()));
        btnPotvrdi.setDisable(!imaPrijedloga);
        btnPonisti.setDisable(!imaPrijedloga);
    }

    /** Spratovi u padajućem meniju, odozgo naniže; izbor se čuva kad se osvježi. */
    private void napuniSpratove() {
        String bio = cmbSprat.getValue();
        List<String> spratovi = new ArrayList<>();
        spratovi.add(SVI_SPRATOVI);
        pregled.sobe().stream()
                .sorted(Comparator.comparingInt(SobaPregledDTO::redoslijedSprata).reversed())
                .map(SobaPregledDTO::sprat)
                .distinct()
                .forEach(spratovi::add);
        cmbSprat.setItems(FXCollections.observableArrayList(spratovi));
        cmbSprat.setValue(spratovi.contains(bio) ? bio : SVI_SPRATOVI);
    }

    /** Broj sobe (tačno ili početak) ili dio imena/prezimena stanara. */
    private boolean odgovara(SobaPregledDTO s, String trazi) {
        if (trazi.isEmpty()) return true;
        if (trazi.chars().allMatch(Character::isDigit)) {
            return String.valueOf(s.broj()).startsWith(trazi);
        }
        return s.stanari().stream().anyMatch(st -> bezKvacica(st.imePrezime()).contains(trazi));
    }

    /** "Šahinović Đana" -> "sahinovic dana", da pretraga radi i bez kvačica. */
    private static String bezKvacica(String s) {
        if (s == null) return "";
        String n = java.text.Normalizer.normalize(s.trim().toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return n.replace('đ', 'd');
    }

    private void prikaziBrojeve() {
        boxBrojevi.getChildren().setAll(
                broj("Mjesta za studente", String.valueOf(pregled.ukupnoMjesta()), null, null),
                broj("Zauzeto", String.valueOf(pregled.zauzeto()), null, null),
                broj("Slobodno · muške", String.valueOf(pregled.slobodnoM()), null, null),
                broj("Slobodno · ženske", String.valueOf(pregled.slobodnoZ()), null, null),
                broj("Odobreni bez sobe", String.valueOf(pregled.bezSobe()),
                        pregled.bezSobe() > 0 ? "broj-upozorenje" : null, this::prikaziBezSobe));
    }

    private VBox broj(String naziv, String vrijednost, String dodatnaKlasa, Runnable klik) {
        Label v = new Label(vrijednost);
        v.getStyleClass().add("broj-vrijednost");
        Label n = new Label(naziv);
        n.getStyleClass().add("broj-naziv");
        VBox b = new VBox(2, v, n);
        b.getStyleClass().add("broj-kartica");
        if (dodatnaKlasa != null) b.getStyleClass().add(dodatnaKlasa);
        HBox.setHgrow(b, Priority.ALWAYS);
        b.setMaxWidth(Double.MAX_VALUE);
        if (klik != null) {
            b.setCursor(Cursor.HAND);
            b.setOnMouseClicked(e -> klik.run());
            n.setText(naziv + " ›");
        }
        return b;
    }

    /** Kartica sobe: broj, popunjenost, imena; boja po stanju, traka u boji kategorije. */
    private VBox kartica(SobaPregledDTO s, String trazi) {
        Label broj = new Label(String.valueOf(s.broj()));
        broj.getStyleClass().add("soba-broj");
        Label pop = new Label(s.zauzeto() + "/" + s.kapacitet());
        pop.getStyleClass().add("soba-popunjenost");
        Region razmak = new Region();
        HBox.setHgrow(razmak, Priority.ALWAYS);
        Label spol = new Label(s.spol() == null ? "" : "M".equals(s.spol()) ? "M" : "Ž");
        spol.getStyleClass().addAll("soba-spol", "M".equals(s.spol()) ? "soba-spol-m" : "soba-spol-z");
        if (s.spol() == null) spol.setVisible(false);
        HBox glava = new HBox(6, broj, razmak, pop, spol);
        glava.setAlignment(Pos.CENTER_LEFT);

        VBox k = new VBox(6, glava);
        k.getStyleClass().addAll("soba-kartica", stanjeKlasa(s));
        k.setPrefWidth(170);
        k.setMinHeight(96);

        if (!s.zaStudente()) {
            k.getChildren().add(malo(s.kategorija()));
        } else if (!s.aktivna()) {
            k.getChildren().add(malo("Van upotrebe" + (s.napomena() != null ? ": " + s.napomena() : "")));
        }

        for (StanarDTO st : s.stanari()) {
            Label ime = new Label(st.imePrezime());
            ime.getStyleClass().add("soba-stanar");
            ime.setMaxWidth(150);
            if ("PREDLOZEN".equals(st.status())) ime.getStyleClass().add("soba-stanar-prijedlog");
            if ("USELJEN".equals(st.status())) ime.getStyleClass().add("soba-stanar-uselio");
            if (!trazi.isEmpty() && !trazi.chars().allMatch(Character::isDigit)
                    && bezKvacica(st.imePrezime()).contains(trazi)) {
                ime.getStyleClass().add("soba-stanar-pogodak");
            }
            if (st.bojaOznake() != null) {
                Region tacka = new Region();
                tacka.getStyleClass().add("soba-tacka");
                tacka.setStyle("-fx-background-color: " + st.bojaOznake() + ";");
                HBox red = new HBox(6, tacka, ime);
                red.setAlignment(Pos.CENTER_LEFT);
                k.getChildren().add(red);
            } else {
                k.getChildren().add(ime);
            }
        }
        for (int i = s.zauzeto(); i < s.kapacitet() && s.zaStudente() && s.aktivna(); i++) {
            Label slob = new Label("slobodno");
            slob.getStyleClass().add("soba-slobodno-mjesto");
            k.getChildren().add(slob);
        }

        // traka u boji kategorije (crvena = gosti...)
        if (s.bojaKategorije() != null) {
            k.setStyle("-fx-border-color: " + s.bojaKategorije() + " transparent transparent transparent;"
                    + "-fx-border-width: 5 0 0 0;");
        }

        k.setCursor(Cursor.HAND);
        k.setOnMouseClicked(e -> otvori(s));
        Tooltip.install(k, new Tooltip("Soba " + s.broj() + " · " + s.sprat() + " · " + s.kategorija()));
        return k;
    }

    private String stanjeKlasa(SobaPregledDTO s) {
        if (!s.zaStudente()) return "soba-gosti";
        if (!s.aktivna()) return "soba-van";
        if (s.zauzeto() == 0) return "soba-prazna";
        if (s.slobodno() == 0) return "soba-puna";
        return "soba-djelimicno";
    }

    private void napraviLegendu() {
        boxLegenda.getChildren().setAll(
                legenda("soba-prazna", "prazna"),
                legenda("soba-djelimicno", "ima slobodnih mjesta"),
                legenda("soba-puna", "puna"),
                legenda("soba-van", "van upotrebe"),
                legenda("soba-gosti", "za goste"),
                legendaTekst("kurziv = prijedlog (nije potvrđen)"),
                legendaTekst("podebljano = uselio"));
    }

    private HBox legenda(String klasa, String tekst) {
        Region r = new Region();
        r.getStyleClass().addAll("legenda-kvadrat", klasa);
        Label l = new Label(tekst);
        l.getStyleClass().add("legenda-tekst");
        HBox h = new HBox(6, r, l);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private Label legendaTekst(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("legenda-tekst");
        return l;
    }

    // =====================================================================
    //  akcije
    // =====================================================================

    private void otvori(SobaPregledDTO s) {
        SobaDetaljiDijalog.prikazi(s, pregled, godina(), oznake, () -> ucitaj(s.idSoba()));
    }

    @FXML
    private void onZahtjevi() {
        ZahtjeviDijalog.prikazi(godina(), this::ucitaj);
    }

    /** Broj zahtjeva na čekanju na dugmetu. */
    private void osvjeziBrojZahtjeva() {
        int g = godina();
        pozovi(() -> SobeApi.zahtjevi(g), lista -> {
            long n = lista.stream().filter(z -> "NA_CEKANJU".equals(z.status())).count();
            btnZahtjevi.setText("Zahtjevi za promjenu sobe" + (n > 0 ? " (" + n + ")" : ""));
            btnZahtjevi.getStyleClass().remove("sobe-btn-glavno");
            if (n > 0) btnZahtjevi.getStyleClass().add("sobe-btn-glavno");
        });
    }

    @FXML
    private void onOsvjezi() {
        ucitaj();
    }

    @FXML
    private void onPrijedlog() {
        if (!potvrda("Napraviti prijedlog rasporeda?",
                "Svi odobreni studenti bez sobe dobiće prijedlog sobe po pravilima:\n"
                        + "• mjesta po rang listi, posebno za muške i ženske sobe\n"
                        + "• dvokrevetne: prvo apsolventi, master i 4. godina i više, pa 3. godina, pa ostali\n"
                        + "• obostrana želja za cimerom i željena soba se poštuju kad ima mjesta\n\n"
                        + "Stari nepotvrđeni prijedlog se briše. Potvrđene dodjele i useljeni ostaju gdje jesu.")) {
            return;
        }
        int g = godina();
        btnPrijedlog.setDisable(true);
        lblStatus.setText("Pravim prijedlog…");
        pozovi(() -> SobeApi.napraviPrijedlog(g), r -> {
            btnPrijedlog.setDisable(false);
            prikaziPrijedlog(r);
            ucitaj();
        }, () -> btnPrijedlog.setDisable(false));
    }

    @FXML
    private void onPotvrdi() {
        if (!potvrda("Potvrditi prijedlog?",
                "Predložene sobe postaju dodjele. Studenti se i dalje mogu premjestiti ručno.")) return;
        int g = godina();
        pozovi(() -> SobeApi.potvrdiPrijedlog(g), n -> {
            info("Potvrđeno " + n + " dodjela.");
            ucitaj();
        });
    }

    @FXML
    private void onPonisti() {
        if (!potvrda("Poništiti prijedlog?", "Brišu se samo nepotvrđeni prijedlozi.")) return;
        int g = godina();
        pozovi(() -> SobeApi.ponistiPrijedlog(g), n -> {
            info("Obrisano " + n + " predloženih dodjela.");
            ucitaj();
        });
    }

    // ---------- tabele ----------

    private void prikaziPrijedlog(PrijedlogRasporedaDTO r) {
        TableView<PrijedlogRasporedaDTO.Stavka> t = new TableView<>(FXCollections.observableArrayList(r.stavke()));
        t.getColumns().add(kolona("Rang", 60, x -> String.valueOf(x.rang())));
        t.getColumns().add(kolona("Student", 200, PrijedlogRasporedaDTO.Stavka::imePrezime));
        t.getColumns().add(kolona("Spol", 50, x -> "Z".equals(x.spol()) ? "Ž" : x.spol()));
        t.getColumns().add(kolona("Godina", 80, x -> godinaTekst(x.godinaStudija())));
        t.getColumns().add(kolona("Soba", 70, x -> x.brojSobe() != null ? String.valueOf(x.brojSobe()) : "—"));
        t.getColumns().add(kolona("Zašto", 420, PrijedlogRasporedaDTO.Stavka::razlog));
        t.setPrefSize(900, 520);

        Label sazetak = new Label("Raspoređeno: " + r.rasporedjeno() + " · bez mjesta: " + r.bezMjesta()
                + ".  Prijedlog je upisan na pregled soba (kurzivom); pregledajte ga i kliknite \"Potvrdi prijedlog\".");
        sazetak.setWrapText(true);

        Dialog<Void> d = new Dialog<>();
        d.setTitle("Prijedlog rasporeda");
        d.setHeaderText("Prijedlog rasporeda za " + godina() + "/" + (godina() + 1));
        d.getDialogPane().setContent(new VBox(10, sazetak, t));
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        d.setResizable(true);
        d.showAndWait();
    }

    private void prikaziBezSobe() {
        int g = godina();
        pozovi(() -> SobeApi.bezSobe(g), lista -> {
            TableView<StudentBezSobeDTO> t = new TableView<>(FXCollections.observableArrayList(lista));
            t.getColumns().add(kolona("Rang", 60, x -> String.valueOf(x.rang())));
            t.getColumns().add(kolona("Student", 220, StudentBezSobeDTO::imePrezime));
            t.getColumns().add(kolona("Indeks", 80, x -> x.brojIndeksa() != null ? x.brojIndeksa() : ""));
            t.getColumns().add(kolona("Spol", 50, x -> "Z".equals(x.spol()) ? "Ž" : x.spol()));
            t.getColumns().add(kolona("Godina", 90, x -> godinaTekst(x.godinaStudija())));
            t.getColumns().add(kolona("Bodovi", 70, x -> String.format("%.2f", x.bodovi())));
            t.getColumns().add(kolona("Želja", 300, x -> x.zelja() != null ? x.zelja() : ""));
            t.setPrefSize(820, 460);

            t.setRowFactory(tv -> {
                TableRow<StudentBezSobeDTO> row = new TableRow<>();
                row.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2 && !row.isEmpty()) StudentDetaljiDijalog.otvori(row.getItem().idPrijava());
                });
                return row;
            });

            Dialog<Void> d = new Dialog<>();
            d.setTitle("Odobreni bez sobe");
            d.setHeaderText(lista.size() + " odobrenih prijava bez sobe. Dvoklik na red otvara detalje studenta.");
            d.getDialogPane().setContent(t);
            d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            d.setResizable(true);
            d.showAndWait();
        });
    }

    static <T> TableColumn<T, String> kolona(String naziv, double sirina, Function<T, String> vrijednost) {
        TableColumn<T, String> c = new TableColumn<>(naziv);
        c.setPrefWidth(sirina);
        c.setCellValueFactory(cd -> new SimpleStringProperty(vrijednost.apply(cd.getValue())));
        return c;
    }

    // =====================================================================
    //  pomoćne
    // =====================================================================

    static String godinaTekst(Integer g) {
        if (g == null) return "";
        return switch (g) {
            case 7 -> "apsolvent";
            case 8 -> "master";
            default -> g + ". godina";
        };
    }

    private static String spolTekst(Set<String> spolovi) {
        if (spolovi.size() == 1) return spolovi.contains("M") ? "muški" : "ženski";
        if (spolovi.isEmpty()) return "spol nije određen";
        return "muške i ženske sobe";
    }

    private Label malo(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("soba-napomena");
        l.setWrapText(true);
        l.setMaxWidth(150);
        return l;
    }

    private ListCell<Integer> godinaCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Integer g, boolean empty) {
                super.updateItem(g, empty);
                setText(empty || g == null ? "" : g + "/" + (g + 1));
            }
        };
    }

    /** Poziv servera u pozadini; greška se prikaže porukom. */
    static <T> void pozovi(java.util.concurrent.Callable<T> poziv, Consumer<T> uspjeh) {
        pozovi(poziv, uspjeh, null);
    }

    static <T> void pozovi(java.util.concurrent.Callable<T> poziv, Consumer<T> uspjeh, Runnable greskaPoslije) {
        Task<T> t = new Task<>() {
            @Override
            protected T call() throws Exception {
                return poziv.call();
            }
        };
        t.setOnSucceeded(e -> uspjeh.accept(t.getValue()));
        t.setOnFailed(e -> {
            Throwable ex = t.getException();
            if (!(ex instanceof ApiException)) ex.printStackTrace();
            info(ex instanceof ApiException ? ex.getMessage()
                    : "Greška u komunikaciji sa serverom." + (ex.getMessage() != null ? "\n" + ex.getMessage() : ""));
            if (greskaPoslije != null) greskaPoslije.run();
        });
        Thread th = new Thread(t);
        th.setDaemon(true);
        th.start();
    }

    static boolean potvrda(String naslov, String tekst) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, tekst);
        a.setHeaderText(naslov);
        return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    static void info(String poruka) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, poruka);
        a.setHeaderText(null);
        a.showAndWait();
    }
}