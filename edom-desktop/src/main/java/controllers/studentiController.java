package controllers;

import api.ApiException;
import api.EvidencijaApi;
import ba.unze.edom.common.dto.EvidencijaDTO;
import ba.unze.edom.common.dto.EvidencijaRedDTO;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import service.export.XlsxPisac;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Centralna evidencija: sve prijave godine, filteri, kolone, Excel, detalji. */
public class studentiController {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

    @FXML private TableView<EvidencijaRedDTO> tblStudenti;
    @FXML private TextField txtSearch;
    @FXML private MenuButton btnSortiraj;
    @FXML private MenuButton btnFiltriraj;
    @FXML private Button btnExportHtmlPdf;

    private final ComboBox<Integer> cmbGodina = new ComboBox<>();
    private final MenuButton btnKolone = new MenuButton("Kolone");
    private final Label lblBroj = new Label();

    private final ObservableList<EvidencijaRedDTO> masterList = FXCollections.observableArrayList();
    private FilteredList<EvidencijaRedDTO> filteredList;
    private SortedList<EvidencijaRedDTO> sortedList;
    private Integer godina;

    // ---------- kolone ----------

    private record Kolona(String naslov, Function<EvidencijaRedDTO, Object> vrijednost, double sirina, boolean vidljiva) {}

    private final List<Kolona> definicije = List.of(
            new Kolona("Šifra", EvidencijaRedDTO::sifra, 110, true),
            new Kolona("Prezime", EvidencijaRedDTO::prezime, 130, true),
            new Kolona("Ime", EvidencijaRedDTO::ime, 110, true),
            new Kolona("Ime roditelja", EvidencijaRedDTO::imeRoditelja, 110, false),
            new Kolona("JMBG", EvidencijaRedDTO::jmbg, 130, false),
            new Kolona("Lična karta", EvidencijaRedDTO::brojLicneKarte, 100, false),
            new Kolona("Indeks", EvidencijaRedDTO::brojIndeksa, 90, true),
            new Kolona("Fakultet", EvidencijaRedDTO::fakultet, 180, true),
            new Kolona("Godina", EvidencijaRedDTO::godinaStudija, 65, true),
            new Kolona("Kanton", EvidencijaRedDTO::kanton, 75, true),
            new Kolona("Općina", EvidencijaRedDTO::opcina, 110, true),
            new Kolona("Adresa", EvidencijaRedDTO::adresa, 170, false),
            new Kolona("Spol", EvidencijaRedDTO::spol, 55, false),
            new Kolona("E-mail", EvidencijaRedDTO::email, 190, false),
            new Kolona("Telefon", EvidencijaRedDTO::telefon, 110, false),
            new Kolona("Tel. roditelja", EvidencijaRedDTO::telefonRoditelja, 110, false),
            new Kolona("Datum prijave", EvidencijaRedDTO::datumPrijave, 105, false),
            new Kolona("Status", EvidencijaRedDTO::status, 100, true),
            new Kolona("Kategorija", EvidencijaRedDTO::kategorija, 105, true),
            new Kolona("Primljen", EvidencijaRedDTO::primljen, 75, true),
            new Kolona("Bodovi", EvidencijaRedDTO::bodovi, 70, true),
            new Kolona("Soba", EvidencijaRedDTO::soba, 60, true),
            new Kolona("Useljen", EvidencijaRedDTO::datumUseljenja, 95, true),
            new Kolona("Iseljen", EvidencijaRedDTO::datumIseljenja, 95, false),
            new Kolona("Ključ", EvidencijaDetaljiDijalog::kljuc, 95, true),
            new Kolona("Kartica", studentiController::kartica, 90, true),
            new Kolona("Ljekarsko", r -> r.ljekarskoPredano() != null, 80, true),
            new Kolona("Uvjerenje fak.", r -> r.uvjerenjeUzPrijavu() || r.uvjerenjeFakultetPredano() != null, 100, true),
            new Kolona("Napomena", EvidencijaRedDTO::napomena, 200, false)
    );

    private final Map<TableColumn<EvidencijaRedDTO, Object>, Kolona> kolone = new LinkedHashMap<>();

    // ---------- filteri ----------

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

    // ---------- početak ----------

    @FXML
    public void initialize() {
        napraviKolone();
        napraviTraku();
        preimenujSortiranje();
        btnExportHtmlPdf.setText("Izvoz u Excel");

        filteredList = new FilteredList<>(masterList, r -> true);
        sortedList = new SortedList<>(filteredList);
        sortedList.comparatorProperty().bind(tblStudenti.comparatorProperty());
        tblStudenti.setItems(sortedList);

        txtSearch.setPromptText("Pretraga: ime, prezime, šifra, indeks, JMBG, soba…");
        txtSearch.textProperty().addListener((o, a, n) -> primijeniFiltere());

        // dvoklik na red → detalji studenta
        tblStudenti.setRowFactory(tv -> {
            TableRow<EvidencijaRedDTO> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    new EvidencijaDetaljiDijalog(row.getItem(), () -> ucitaj(godina)).prikazi();
                }
            });
            return row;
        });

        ucitaj(null);
    }

    private void napraviKolone() {
        tblStudenti.getColumns().clear();
        for (Kolona k : definicije) {
            TableColumn<EvidencijaRedDTO, Object> col = new TableColumn<>(k.naslov());
            col.setPrefWidth(k.sirina());
            col.setVisible(k.vidljiva());
            col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(k.vrijednost().apply(c.getValue())));
            col.setCellFactory(tc -> new TableCell<>() {
                @Override
                protected void updateItem(Object v, boolean empty) {
                    super.updateItem(v, empty);
                    setText(empty ? null : tekst(v));
                }
            });
            col.setComparator(studentiController::uporedi);
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

    /** Akademska godina i broj redova u gornji red; "Kolone" pored "Filtriraj". */
    private void napraviTraku() {
        btnKolone.getStyleClass().add("menu-btn");
        ((HBox) btnFiltriraj.getParent()).getChildren().add(0, btnKolone);

        cmbGodina.setPromptText("Godina");
        cmbGodina.setOnAction(e -> {
            Integer g = cmbGodina.getValue();
            if (g != null && !g.equals(godina)) ucitaj(g);
        });

        HBox gornja = (HBox) txtSearch.getParent();
        gornja.getChildren().add(1, new Label("Akademska godina:"));
        gornja.getChildren().add(2, cmbGodina);
        gornja.getChildren().add(3, lblBroj);
    }

    private void preimenujSortiranje() {
        List<String> nazivi = List.of("Prezime A–Z", "Prezime Z–A", "Šifra uzlazno",
                "Šifra silazno", "Bodovi uzlazno", "Bodovi silazno");
        for (int i = 0; i < Math.min(nazivi.size(), btnSortiraj.getItems().size()); i++) {
            btnSortiraj.getItems().get(i).setText(nazivi.get(i));
        }
    }

    // ---------- učitavanje ----------

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

    private void azurirajGodine() {
        if (godina == null) return;
        List<Integer> godine = new ArrayList<>();
        for (int g = godina; g >= godina - 4; g--) godine.add(g);
        if (!cmbGodina.getItems().equals(godine)) cmbGodina.getItems().setAll(godine);
        cmbGodina.setValue(godina);
    }

    // ---------- filteri ----------

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

        MenuItem ponisti = new MenuItem("Poništi sve filtere");
        ponisti.setOnAction(e -> {
            odabrano.values().forEach(Set::clear);
            napuniFiltere();
            primijeniFiltere();
        });
        btnFiltriraj.getItems().addAll(new SeparatorMenuItem(), ponisti);
    }

    private String kljucFiltera(Filter f, EvidencijaRedDTO r) {
        String v = f.kljuc().apply(r);
        return v == null || v.isBlank() ? "—" : v;
    }

    private void primijeniFiltere() {
        String q = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase();

        filteredList.setPredicate(r -> {
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
        lblBroj.setText(filteredList.size() + " od " + masterList.size());
    }

    // ---------- sortiranje (stavke iz FXML-a) ----------

    @FXML private void sortImeAZ()       { sortiraj("Prezime", TableColumn.SortType.ASCENDING); }
    @FXML private void sortImeZA()       { sortiraj("Prezime", TableColumn.SortType.DESCENDING); }
    @FXML private void sortIdUzlazno()   { sortiraj("Šifra", TableColumn.SortType.ASCENDING); }
    @FXML private void sortIdSilazno()   { sortiraj("Šifra", TableColumn.SortType.DESCENDING); }
    @FXML private void sortProsjekAsc()  { sortiraj("Bodovi", TableColumn.SortType.ASCENDING); }
    @FXML private void sortProsjekDesc() { sortiraj("Bodovi", TableColumn.SortType.DESCENDING); }

    private void sortiraj(String naslov, TableColumn.SortType smjer) {
        kolone.keySet().stream()
                .filter(c -> c.getText().equals(naslov))
                .findFirst()
                .ifPresent(c -> {
                    c.setSortType(smjer);
                    tblStudenti.getSortOrder().setAll(c);
                });
    }

    // ---------- izvoz u Excel (dugme iz FXML-a) ----------

    @FXML
    private void onExportHtmlPdf() {
        if (sortedList.isEmpty()) return;

        List<TableColumn<EvidencijaRedDTO, ?>> vidljive = tblStudenti.getColumns().stream()
                .filter(TableColumn::isVisible)
                .filter(kolone::containsKey)
                .toList();

        List<String> naslovi = vidljive.stream().map(TableColumn::getText).toList();
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
            new Alert(Alert.AlertType.INFORMATION, "Izvezeno " + redovi.size() + " studenata.", ButtonType.OK)
                    .showAndWait();
        } catch (RuntimeException ex) {
            prikaziGresku(ex, "Greška pri izvozu u Excel.");
        }
    }

    // ---------- pomoćne ----------

    static String kartica(EvidencijaRedDTO r) {
        if (r.karticaRazduzena() != null) return "Razdužena";
        if (r.karticaPreuzeta() != null) return "Preuzeta";
        return null;
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
        String poruka = ex instanceof ApiException ? ex.getMessage() : opsta;
        new Alert(Alert.AlertType.ERROR, poruka).showAndWait();
    }
}