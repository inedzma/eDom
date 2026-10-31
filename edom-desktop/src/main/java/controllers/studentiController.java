package controllers;

import api.ApiException;
import api.EvidencijaApi;
import ba.unze.edom.common.dto.student.EvidencijaDTO;
import ba.unze.edom.common.dto.student.EvidencijaRedDTO;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import service.export.XlsxPisac;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Centralna evidencija: brzi pogledi, filteri, kolone po izboru, Excel, detalji studenta. */
public class studentiController {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

    @FXML private TableView<EvidencijaRedDTO> tblStudenti;
    @FXML private TextField txtSearch;
    @FXML private MenuButton btnFiltriraj;
    @FXML private MenuButton btnKolone;
    @FXML private ComboBox<Integer> cmbGodina;
    @FXML private FlowPane boxPogledi;
    @FXML private Label lblPodnaslov;
    @FXML private Label lblBroj;
    @FXML private Button btnOcisti;

    private final ObservableList<EvidencijaRedDTO> masterList = FXCollections.observableArrayList();
    private FilteredList<EvidencijaRedDTO> filteredList;
    private SortedList<EvidencijaRedDTO> sortedList;
    private Integer godina;
    private Integer najnovijaGodina;

    // =====================================================================
    //  KOLONE
    // =====================================================================

    /** Kako se vrijednost prikazuje u ćeliji (u Excel ide uvijek kao tekst/broj). */
    private enum Prikaz { TEKST, SIFRA, STATUS, KATEGORIJA, DA_NE, KVACICA, KARTICA, BROJ }

    private record Kolona(String naslov, Function<EvidencijaRedDTO, Object> vrijednost,
                          double sirina, boolean vidljiva, Prikaz prikaz) {}

    private final List<Kolona> definicije = List.of(
            new Kolona("Šifra", EvidencijaRedDTO::sifra, 115, true, Prikaz.SIFRA),
            new Kolona("Prezime", EvidencijaRedDTO::prezime, 130, true, Prikaz.TEKST),
            new Kolona("Ime", EvidencijaRedDTO::ime, 110, true, Prikaz.TEKST),
            new Kolona("Ime roditelja", EvidencijaRedDTO::imeRoditelja, 110, false, Prikaz.TEKST),
            new Kolona("JMBG", EvidencijaRedDTO::jmbg, 130, false, Prikaz.TEKST),
            new Kolona("Lična karta", EvidencijaRedDTO::brojLicneKarte, 100, false, Prikaz.TEKST),
            new Kolona("Indeks", EvidencijaRedDTO::brojIndeksa, 90, true, Prikaz.TEKST),
            new Kolona("Fakultet", r -> r.fakultet() == null ? null : r.fakultet().replace(" UNZE", ""), 170, true, Prikaz.TEKST),
            new Kolona("Godina", EvidencijaRedDTO::godinaStudija, 70, true, Prikaz.BROJ),
            new Kolona("Kanton", EvidencijaRedDTO::kanton, 75, false, Prikaz.TEKST),
            new Kolona("Općina", EvidencijaRedDTO::opcina, 110, true, Prikaz.TEKST),
            new Kolona("Adresa", EvidencijaRedDTO::adresa, 170, false, Prikaz.TEKST),
            new Kolona("Spol", EvidencijaRedDTO::spol, 55, false, Prikaz.BROJ),
            new Kolona("E-mail", EvidencijaRedDTO::email, 190, false, Prikaz.TEKST),
            new Kolona("Telefon", EvidencijaRedDTO::telefon, 110, false, Prikaz.TEKST),
            new Kolona("Tel. roditelja", EvidencijaRedDTO::telefonRoditelja, 110, false, Prikaz.TEKST),
            new Kolona("Datum prijave", EvidencijaRedDTO::datumPrijave, 105, false, Prikaz.BROJ),
            new Kolona("Status", EvidencijaRedDTO::status, 120, true, Prikaz.STATUS),
            new Kolona("Kategorija", EvidencijaRedDTO::kategorija, 120, true, Prikaz.KATEGORIJA),
            new Kolona("Primljen", EvidencijaRedDTO::primljen, 80, true, Prikaz.DA_NE),
            new Kolona("Bodovi", EvidencijaRedDTO::bodovi, 75, true, Prikaz.BROJ),
            new Kolona("Soba", EvidencijaRedDTO::soba, 60, true, Prikaz.BROJ),
            new Kolona("Useljen", EvidencijaRedDTO::datumUseljenja, 95, true, Prikaz.BROJ),
            new Kolona("Iseljen", EvidencijaRedDTO::datumIseljenja, 95, false, Prikaz.BROJ),
            new Kolona("Ključ", EvidencijaDetaljiDijalog::kljuc, 100, true, Prikaz.TEKST),
            new Kolona("Kartica", studentiController::kartica, 95, true, Prikaz.KARTICA),
            new Kolona("Ljekarsko", r -> r.ljekarskoPredano() != null, 80, true, Prikaz.KVACICA),
            new Kolona("Uvjerenje fak.", r -> r.uvjerenjeUzPrijavu() || r.uvjerenjeFakultetPredano() != null, 100, true, Prikaz.KVACICA),
            new Kolona("Napomena", EvidencijaRedDTO::napomena, 200, false, Prikaz.TEKST)
    );

    private final Map<TableColumn<EvidencijaRedDTO, Object>, Kolona> kolone = new LinkedHashMap<>();

    // =====================================================================
    //  FILTERI I BRZI POGLEDI
    // =====================================================================

    private record Filter(String naziv, Function<EvidencijaRedDTO, String> kljuc) {}

    private final List<Filter> filteri = List.of(
            new Filter("Kategorija", EvidencijaRedDTO::kategorija),
            new Filter("Status prijave", EvidencijaRedDTO::status),
            new Filter("Rang lista", r -> r.primljen() ? "Primljen" : "Nije primljen"),
            new Filter("Soba", r -> r.soba() != null && !"ISELJEN".equals(r.statusSmjestaja()) ? "Ima sobu" : "Nema sobu"),
            new Filter("Fakultet", EvidencijaRedDTO::fakultet),
            new Filter("Godina studija", r -> r.godinaStudija() == null ? null : r.godinaStudija() + ". godina"),
            new Filter("Kanton", EvidencijaRedDTO::kanton),
            new Filter("Općina", EvidencijaRedDTO::opcina),
            new Filter("Spol", EvidencijaRedDTO::spol)
    );

    private final Map<String, Set<String>> odabrano = new HashMap<>();

    private record Pogled(String naziv, String klasa, Predicate<EvidencijaRedDTO> uslov) {}

    private final List<Pogled> pogledi = List.of(
            new Pogled("Sve", null, r -> true),
            new Pogled("Primljeni", "st-odobreno", EvidencijaRedDTO::primljen),
            new Pogled("U domu", "st-zakljuceno", r -> "USELJEN".equals(r.statusSmjestaja())),
            new Pogled("Čeka useljenje", "st-pregled",
                    r -> "PREDLOZEN".equals(r.statusSmjestaja()) || "DODIJELJEN".equals(r.statusSmjestaja())),
            new Pogled("Primljeni bez sobe", "st-nepotpuna", r -> r.primljen() && r.soba() == null),
            new Pogled("Lista čekanja", "st-cekanje", r -> "Lista čekanja".equals(r.kategorija())),
            new Pogled("Iseljeni", "st-iseljen",
                    r -> "ISELJEN".equals(r.statusSmjestaja()) || "iseljen".equals(r.status()))
    );

    private Pogled odabraniPogled = pogledi.get(0);
    private final ToggleGroup pogledGrupa = new ToggleGroup();

    // =====================================================================
    //  POČETAK
    // =====================================================================

    @FXML
    public void initialize() {
        napraviKolone();

        filteredList = new FilteredList<>(masterList, r -> true);
        sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(tblStudenti.comparatorProperty());
        tblStudenti.setItems(sortedList);
        tblStudenti.setPlaceholder(new Label("Nema studenata za odabrane filtere."));

        txtSearch.textProperty().addListener((o, a, n) -> primijeniFiltere());

        cmbGodina.setPromptText("Godina");
        cmbGodina.setOnAction(e -> {
            Integer g = cmbGodina.getValue();
            if (g != null && !g.equals(godina)) ucitaj(g);
        });

        tblStudenti.setRowFactory(tv -> {
            TableRow<EvidencijaRedDTO> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) otvoriDetalje(row.getItem());
            });
            MenuItem detalji = new MenuItem("Detalji studenta");
            detalji.setOnAction(e -> { if (!row.isEmpty()) otvoriDetalje(row.getItem()); });
            row.contextMenuProperty().bind(Bindings.when(row.emptyProperty())
                    .then((ContextMenu) null).otherwise(new ContextMenu(detalji)));
            return row;
        });
        tblStudenti.setOnKeyPressed(e -> {
            EvidencijaRedDTO r = tblStudenti.getSelectionModel().getSelectedItem();
            if (e.getCode() == KeyCode.ENTER && r != null) otvoriDetalje(r);
        });

        ucitaj(null);
    }

    private void otvoriDetalje(EvidencijaRedDTO r) {
        new EvidencijaDetaljiDijalog(r, () -> ucitaj(godina)).prikazi();
    }

    private void napraviKolone() {
        tblStudenti.getColumns().clear();
        for (Kolona k : definicije) {
            TableColumn<EvidencijaRedDTO, Object> col = new TableColumn<>(k.naslov().toUpperCase());
            col.setPrefWidth(k.sirina());
            col.setVisible(k.vidljiva());
            col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(k.vrijednost().apply(c.getValue())));
            col.setCellFactory(tc -> celija(k.prikaz()));
            col.setComparator(studentiController::uporedi);
            if (k.prikaz() == Prikaz.BROJ || k.prikaz() == Prikaz.KVACICA || k.prikaz() == Prikaz.DA_NE) {
                col.setStyle("-fx-alignment: CENTER;");
            }
            kolone.put(col, k);
            tblStudenti.getColumns().add(col);
        }

        btnKolone.getItems().clear();
        kolone.forEach((col, k) -> {
            CheckMenuItem item = new CheckMenuItem(k.naslov());
            item.setSelected(col.isVisible());
            col.visibleProperty().bindBidirectional(item.selectedProperty());
            btnKolone.getItems().add(item);
        });
    }

    /** Ćelija prema vrsti prikaza: pilule za status/kategoriju, ✓ za predano itd. */
    private TableCell<EvidencijaRedDTO, Object> celija(Prikaz prikaz) {
        return new TableCell<>() {
            private final Label oznaka = new Label();

            @Override
            protected void updateItem(Object v, boolean empty) {
                super.updateItem(v, empty);
                setText(null);
                setGraphic(null);
                if (empty) return;

                switch (prikaz) {
                    case SIFRA -> {
                        oznaka.setText(tekst(v));
                        oznaka.getStyleClass().setAll("label", "pr-sifra");
                        setGraphic(oznaka);
                    }
                    case STATUS -> pilula(v == null ? null : tekst(v),
                            v == null ? null : prijaveController.statusKlasa(tekst(v)));
                    case KATEGORIJA -> pilula(v == null || "—".equals(v) ? null : tekst(v), kategorijaKlasa(tekst(v)));
                    case DA_NE -> pilula(Boolean.TRUE.equals(v) ? "Da" : "Ne",
                            Boolean.TRUE.equals(v) ? "st-odobreno" : "st-iseljen");
                    case KARTICA -> pilula(v == null ? null : tekst(v),
                            "Preuzeta".equals(v) ? "st-odobreno" : "st-iseljen");
                    case KVACICA -> {
                        boolean da = Boolean.TRUE.equals(v);
                        oznaka.setText(da ? "✓" : "—");
                        oznaka.getStyleClass().setAll("label", da ? "pr-kvacica" : "pr-sitno");
                        setGraphic(oznaka);
                    }
                    default -> setText(tekst(v));
                }
            }

            private void pilula(String tekst, String klasa) {
                if (tekst == null || tekst.isBlank()) {
                    setText("—");
                    return;
                }
                oznaka.setText(Character.toUpperCase(tekst.charAt(0)) + tekst.substring(1));
                oznaka.getStyleClass().setAll("label", "pr-status", klasa != null ? klasa : "st-ostalo");
                setGraphic(oznaka);
            }
        };
    }

    private static String kategorijaKlasa(String k) {
        return switch (k == null ? "" : k) {
            case "Brucoš" -> "st-pregled";
            case "S uslovom" -> "st-odobreno";
            case "Lista čekanja" -> "st-cekanje";
            default -> "st-ostalo";
        };
    }

    // =====================================================================
    //  UČITAVANJE
    // =====================================================================

    private void ucitaj(Integer g) {
        Task<EvidencijaDTO> task = new Task<>() {
            @Override
            protected EvidencijaDTO call() {
                return EvidencijaApi.lista(g);
            }
        };
        task.setOnSucceeded(e -> {
            EvidencijaDTO d = task.getValue();
            godina = d.godina();
            masterList.setAll(d.redovi());
            azurirajGodine();
            napuniFiltere();
            napraviPoglede();
            osvjeziPodnaslov();
            primijeniFiltere();
        });
        task.setOnFailed(e -> prikaziGresku(task.getException(), "Greška pri učitavanju evidencije."));
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onRefresh() {
        ucitaj(godina);
    }

    private void osvjeziPodnaslov() {
        long uDomu = masterList.stream().filter(r -> "USELJEN".equals(r.statusSmjestaja())).count();
        List<String> d = new ArrayList<>();
        d.add("Centralna evidencija");
        if (godina != null) d.add(godina + "/" + (godina + 1));
        d.add(masterList.size() + " " + studenata(masterList.size()));
        if (uDomu > 0) d.add(uDomu + " u domu");
        lblPodnaslov.setText(String.join("   ·   ", d));
    }

    // =====================================================================
    //  POGLEDI (pilule)
    // =====================================================================

    private void napraviPoglede() {
        boxPogledi.getChildren().clear();
        pogledGrupa.getToggles().clear();

        for (Pogled p : pogledi) {
            long n = masterList.stream().filter(p.uslov()).count();
            if (n == 0 && p != pogledi.get(0) && p != odabraniPogled) continue;

            ToggleButton t = new ToggleButton(p.naziv() + "   " + n);
            t.getStyleClass().add("pr-chip");
            t.setToggleGroup(pogledGrupa);
            if (p.klasa() != null) {
                Circle tacka = new Circle(4);
                tacka.getStyleClass().addAll("pr-tacka", p.klasa());
                t.setGraphic(tacka);
            }
            t.setSelected(p == odabraniPogled);
            t.setOnAction(e -> {
                odabraniPogled = p;
                t.setSelected(true);
                primijeniFiltere();
            });
            boxPogledi.getChildren().add(t);
        }
    }

    // =====================================================================
    //  FILTERI
    // =====================================================================

    private void napuniFiltere() {
        btnFiltriraj.getItems().clear();
        for (Filter f : filteri) {
            Set<String> vrijednosti = masterList.stream()
                    .map(r -> kljucFiltera(f, r))
                    .collect(Collectors.toCollection(TreeSet::new));
            Set<String> sel = odabrano.computeIfAbsent(f.naziv(), k -> new HashSet<>());
            sel.retainAll(vrijednosti);

            Menu meni = new Menu(f.naziv());
            for (String v : vrijednosti) {
                CheckMenuItem item = new CheckMenuItem(v);
                item.setSelected(sel.contains(v));
                item.setOnAction(e -> {
                    if (item.isSelected()) sel.add(v); else sel.remove(v);
                    primijeniFiltere();
                });
                meni.getItems().add(item);
            }
            btnFiltriraj.getItems().add(meni);
        }
    }

    private String kljucFiltera(Filter f, EvidencijaRedDTO r) {
        String v = f.kljuc().apply(r);
        return v == null || v.isBlank() ? "—" : v;
    }

    private void primijeniFiltere() {
        String q = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase();

        filteredList.setPredicate(r -> {
            if (!odabraniPogled.uslov().test(r)) return false;
            if (!q.isEmpty()) {
                String sve = String.join(" ",
                        n(r.sifra()), n(r.ime()), n(r.prezime()), n(r.brojIndeksa()), n(r.jmbg()),
                        n(r.email()), n(r.fakultet()), n(r.opcina()),
                        r.soba() != null ? "soba " + r.soba() : "").toLowerCase();
                if (!sve.contains(q)) return false;
            }
            for (Filter f : filteri) {
                Set<String> sel = odabrano.get(f.naziv());
                if (sel != null && !sel.isEmpty() && !sel.contains(kljucFiltera(f, r))) return false;
            }
            return true;
        });

        long aktivnih = odabrano.values().stream().mapToLong(Set::size).sum();
        btnFiltriraj.setText(aktivnih > 0 ? "Filtriraj (" + aktivnih + ")" : "Filtriraj");

        int n = filteredList.size();
        lblBroj.setText(n == masterList.size()
                ? n + " " + studenata(n)
                : "Prikazano " + n + " od " + masterList.size() + " " + studenata(masterList.size()));

        boolean aktivno = !q.isEmpty() || aktivnih > 0 || odabraniPogled != pogledi.get(0);
        btnOcisti.setVisible(aktivno);
        btnOcisti.setManaged(aktivno);
    }

    @FXML
    private void onOcisti() {
        txtSearch.clear();
        odabrano.values().forEach(Set::clear);
        odabraniPogled = pogledi.get(0);
        napuniFiltere();
        napraviPoglede();
        primijeniFiltere();
    }

    // =====================================================================
    //  SORTIRANJE
    // =====================================================================

    @FXML private void sortPrezimeAZ()     { sortiraj("Prezime", TableColumn.SortType.ASCENDING); }
    @FXML private void sortPrezimeZA()     { sortiraj("Prezime", TableColumn.SortType.DESCENDING); }
    @FXML private void sortBodoviSilazno() { sortiraj("Bodovi", TableColumn.SortType.DESCENDING); }
    @FXML private void sortBodoviUzlazno() { sortiraj("Bodovi", TableColumn.SortType.ASCENDING); }
    @FXML private void sortSifra()         { sortiraj("Šifra", TableColumn.SortType.ASCENDING); }
    @FXML private void sortSoba()          { sortiraj("Soba", TableColumn.SortType.ASCENDING); }

    private void sortiraj(String naslov, TableColumn.SortType smjer) {
        kolone.forEach((c, k) -> {
            if (k.naslov().equals(naslov)) {
                c.setVisible(true);
                c.setSortType(smjer);
                tblStudenti.getSortOrder().setAll(c);
            }
        });
    }

    // =====================================================================
    //  IZVOZ U EXCEL
    // =====================================================================

    @FXML
    private void onExport() {
        if (sortedList.isEmpty()) return;

        List<TableColumn<EvidencijaRedDTO, ?>> vidljive = tblStudenti.getColumns().stream()
                .filter(TableColumn::isVisible)
                .filter(kolone::containsKey)
                .toList();

        List<String> naslovi = vidljive.stream().map(c -> kolone.get(c).naslov()).toList();
        List<Integer> sirine = vidljive.stream().map(c -> (int) Math.max(8, c.getWidth() / 7)).toList();

        List<List<Object>> redovi = new ArrayList<>();
        for (EvidencijaRedDTO r : sortedList) {
            List<Object> red = new ArrayList<>();
            for (TableColumn<EvidencijaRedDTO, ?> c : vidljive) {
                Object v = kolone.get(c).vrijednost().apply(r);
                red.add(v instanceof Number ? v : (v == null ? null : tekst(v)));
            }
            redovi.add(red);
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Sačuvaj evidenciju");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel fajl", "*.xlsx"));
        fc.setInitialFileName("Evidencija_studenata_" + godina + ".xlsx");
        File file = fc.showSaveDialog(tblStudenti.getScene().getWindow());
        if (file == null) return;

        try {
            String imeLista = "Studenti " + godina + "-" + String.format("%02d", (godina + 1) % 100);
            XlsxPisac.zapisi(file, List.of(new XlsxPisac.Tabela(imeLista, naslovi, sirine, redovi)));
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Izvezeno " + redovi.size() + " studenata.", ButtonType.OK);
            a.setHeaderText(null);
            a.showAndWait();
        } catch (RuntimeException ex) {
            prikaziGresku(ex, "Greška pri izvozu u Excel.");
        }
    }

    // =====================================================================
    //  POMOĆNE
    // =====================================================================

    static String kartica(EvidencijaRedDTO r) {
        if (r.karticaRazduzena() != null) return "Razdužena";
        if (r.karticaPreuzeta() != null) return "Preuzeta";
        return null;
    }

    /** 1 student, 2–4 studenta, 5+ studenata (21 student, 22 studenta…). */
    private static String studenata(long n) {
        long d = n % 10, s = n % 100;
        if (d == 1 && s != 11) return "student";
        if (d >= 2 && d <= 4 && (s < 12 || s > 14)) return "studenta";
        return "studenata";
    }

    private static String tekst(Object v) {
        if (v == null) return "";
        if (v instanceof LocalDate d) return d.format(DATUM);
        if (v instanceof Double d) return String.format("%.2f", d);
        if (v instanceof Boolean b) return b ? "Da" : "Ne";
        return v.toString();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int uporedi(Object a, Object b) {
        if (a == null) return b == null ? 0 : 1;
        if (b == null) return -1;
        if (a instanceof Comparable ca && a.getClass() == b.getClass()) return ca.compareTo(b);
        return tekst(a).compareToIgnoreCase(tekst(b));
    }

    private static String n(String s) {
        return s == null ? "" : s;
    }

    private void prikaziGresku(Throwable ex, String opsta) {
        if (!(ex instanceof ApiException)) ex.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR, ex instanceof ApiException ? ex.getMessage() : opsta);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void azurirajGodine() {
        if (godina == null) return;
        if (najnovijaGodina == null || godina > najnovijaGodina) najnovijaGodina = godina;

        List<Integer> godine = new ArrayList<>();
        for (int g = najnovijaGodina; g >= najnovijaGodina - 5; g--) godine.add(g);
        if (!godine.contains(godina)) godine.add(godina);    // za svaki slučaj, ako je izabrana starija

        if (!cmbGodina.getItems().equals(godine)) cmbGodina.getItems().setAll(godine);
        cmbGodina.setValue(godina);
    }
}