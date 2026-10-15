package controllers;

import api.ApiException;
import api.RucniUnosApi;
import ba.unze.edom.common.dto.*;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import unos.NacrtPrijave;
import util.Ekrani;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class DodajDokumenteController {

    // ---- uskladi s tabelom vrsta_dokumenta ----
    private static final String KAT_USPJEH      = "Uspjeh";
    private static final String KAT_DOMACINSTVO = "Domaćinstvo";
    private static final String KAT_KRITERIJI   = "Dodatni kriteriji";
    private static final String NAZIV_KUCNA     = "Uvjerenje o zajedničkom domaćinstvu";
    private static final String NAZIV_CIPS      = "CIPS";

    /** Glavni dokument o uspjehu — bira se jedan od ovih; ostali iz kategorije "Uspjeh" su dodatni. */
    private static final Set<String> GLAVNI_USPJEH = Set.of(
            "svjedodzba o zavrsetku srednje skole",
            "uvjerenje o polozenim ispitima",
            "ovjerena kopija indeksa sa ocjenama");

    @FXML private Accordion accordionDokumenti;
    @FXML private Button btnPodnesi;

    private NacrtPrijave nacrt;

    // uspjeh
    private TextField txtIspiti;
    private ComboBox<VrstaDokumentaDTO> cmbUspjeh;
    private CheckBox chkUspjeh;

    // domaćinstvo
    private VrstaDokumentaDTO vrstaKucna;
    private CheckBox chkKucna;
    private record ClanPolja(TextField ime, TextField primanja,
                             ComboBox<VrstaDokumentaDTO> vrsta, CheckBox dostavljen) {}
    private final List<ClanPolja> clanovi = new ArrayList<>();

    // dokazi za kriterije
    private record KriterijDokaz(String sifra, String naziv,
                                 Integer idDokaza, String nazivDokaza, CheckBox dostavljen) {}
    private final List<KriterijDokaz> kriterijDokazi = new ArrayList<>();

    // svi ostali dokumenti
    private record OstaliDokument(VrstaDokumentaDTO vrsta, CheckBox dostavljen) {}
    private final List<OstaliDokument> ostali = new ArrayList<>();

    public void setNacrt(NacrtPrijave n) {
        this.nacrt = n;
        napraviSekcije();
    }

    // ---------- izgradnja ekrana ----------

    private void napraviSekcije() {
        List<VrstaDokumentaDTO> sve = nacrt.sifarnici.vrsteDokumenata();

        List<VrstaDokumentaDTO> uspjehVrste = poKategoriji(sve, KAT_USPJEH);
        List<VrstaDokumentaDTO> uspjehGlavni = uspjehVrste.stream()
                .filter(v -> GLAVNI_USPJEH.contains(v.naziv().toLowerCase())).toList();
        List<VrstaDokumentaDTO> uspjehDodatni = uspjehVrste.stream()
                .filter(v -> !GLAVNI_USPJEH.contains(v.naziv().toLowerCase())).toList();
        List<VrstaDokumentaDTO> clanVrste = poKategoriji(sve, KAT_DOMACINSTVO).stream()
                .filter(v -> !NAZIV_KUCNA.equalsIgnoreCase(v.naziv())).toList();
        List<VrstaDokumentaDTO> kriterijVrste = poKategoriji(sve, KAT_KRITERIJI);
        vrstaKucna = sve.stream().filter(v -> NAZIV_KUCNA.equalsIgnoreCase(v.naziv())).findFirst().orElse(null);

        accordionDokumenti.getPanes().clear();
        accordionDokumenti.getPanes().add(sekcijaDomacinstvo(clanVrste));
        if (!nacrt.kriteriji.isEmpty()) {
            accordionDokumenti.getPanes().add(sekcijaKriteriji());
        }
        accordionDokumenti.getPanes().add(sekcijaUspjeh(uspjehGlavni, uspjehDodatni));

        // sve ostalo, grupisano po kategoriji iz baze
        Set<String> posebne = Set.of(KAT_USPJEH, KAT_DOMACINSTVO, KAT_KRITERIJI);
        Map<String, List<VrstaDokumentaDTO>> ostaleKategorije = sve.stream()
                .filter(v -> !posebne.contains(v.kategorija()))
                .filter(v -> !NAZIV_KUCNA.equalsIgnoreCase(v.naziv()))
                .collect(Collectors.groupingBy(VrstaDokumentaDTO::kategorija,
                        LinkedHashMap::new, Collectors.toList()));

        ostaleKategorije.forEach((kategorija, vrste) -> {
            VBox box = new VBox(10);
            for (VrstaDokumentaDTO v : vrste) {
                CheckBox chk = new CheckBox(v.naziv() + " — dostavljeno");
                chk.getStyleClass().add("checkbox");
                chk.setWrapText(true);
                ostali.add(new OstaliDokument(v, chk));
                box.getChildren().add(chk);
            }
            accordionDokumenti.getPanes().add(0, sekcija(kategorija, box));
        });

        if (!accordionDokumenti.getPanes().isEmpty()) {
            accordionDokumenti.setExpandedPane(accordionDokumenti.getPanes().get(0));
        }
    }

    private TitledPane sekcijaUspjeh(List<VrstaDokumentaDTO> glavni, List<VrstaDokumentaDTO> dodatni) {
        boolean brucos = nacrt.student.godinaStudija() == 1;

        cmbUspjeh = combo(glavni, brucos ? "Svjedodzba" : "Uvjerenje o polozenim ispitima");

        txtIspiti = new TextField();
        txtIspiti.setPromptText("Broj položenih ispita");
        txtIspiti.getStyleClass().addAll("field", "field-small");

        chkUspjeh = new CheckBox("Original dostavljen");
        chkUspjeh.getStyleClass().add("checkbox");

        VBox box = new VBox(12,
                opis(brucos ? "Svjedodžba o završetku srednje škole" : "Dokument sa fakulteta"),
                cmbUspjeh);
        if (!brucos) {
            Label lbl = new Label("Broj položenih ispita:");
            lbl.getStyleClass().add("doc-subtitle");
            box.getChildren().add(red(lbl, txtIspiti));
        }
        box.getChildren().add(chkUspjeh);

        // dodatni dokumenti o uspjehu (npr. nagrade) — svaki sa svojom kvačicom
        if (!dodatni.isEmpty()) {
            box.getChildren().add(new Separator());
            for (VrstaDokumentaDTO v : dodatni) {
                CheckBox chk = new CheckBox(v.naziv() + " — dostavljeno");
                chk.getStyleClass().add("checkbox");
                chk.setWrapText(true);
                ostali.add(new OstaliDokument(v, chk));
                box.getChildren().add(chk);
            }
        }

        return sekcija("Uspjeh (prosjek " + nacrt.prosjek.toPlainString() + ")", box);
    }
    private TitledPane sekcijaDomacinstvo(List<VrstaDokumentaDTO> clanVrste) {
        VBox box = new VBox(14);

        if (vrstaKucna != null) {
            chkKucna = new CheckBox(vrstaKucna.naziv() + " — dostavljeno");
            chkKucna.getStyleClass().add("checkbox");
            box.getChildren().add(chkKucna);
        }

        int ostalihClanova = nacrt.brojClanova - 1;   // student se ne navodi
        if (ostalihClanova > 0) {
            box.getChildren().add(opis("Unesite ostale članove domaćinstva (bez studenta): " + ostalihClanova));
        }

        for (int i = 1; i <= ostalihClanova; i++) {
            TextField ime = new TextField();
            ime.setPromptText("Ime / uloga (npr. majka)");
            ime.getStyleClass().add("field");

            TextField primanja = new TextField();
            primanja.setPromptText("Mjesečna primanja (KM)");
            primanja.getStyleClass().add("field");
            primanja.setPrefWidth(220);

            ComboBox<VrstaDokumentaDTO> vrsta = combo(clanVrste, null);
            vrsta.setPromptText("Dokument za ovog člana");

            CheckBox dostavljen = new CheckBox("Dokument dostavljen");
            dostavljen.getStyleClass().add("checkbox");

            Label naslov = new Label("Član " + i);
            naslov.getStyleClass().add("member-title");

            VBox kartica = new VBox(10, naslov, ime, red(primanja, vrsta), dostavljen);
            kartica.getStyleClass().add("household-member-card");
            box.getChildren().add(kartica);

            clanovi.add(new ClanPolja(ime, primanja, vrsta, dostavljen));
        }

        return sekcija("Domaćinstvo (" + nacrt.brojClanova + " članova)", box);
    }

    private TitledPane sekcijaKriteriji() {
        VBox box = new VBox(14);

        for (String sifra : nacrt.kriteriji.keySet()) {
            VrstaKriterijaDTO k = nacrt.sifarnici.kriteriji().stream()
                    .filter(x -> x.sifra().equals(sifra))
                    .findFirst().orElse(null);

            String naziv = k != null ? k.naziv() : sifra;
            boolean imaDokaz = k != null && k.idDokaza() != null;

            Label lbl = new Label(naziv);
            lbl.getStyleClass().add("doc-title");
            lbl.setWrapText(true);

            CheckBox chk = new CheckBox(imaDokaz
                    ? k.nazivDokaza() + " — dostavljeno"
                    : "Provjereno");
            chk.getStyleClass().add("checkbox");
            chk.setWrapText(true);

            VBox kartica = new VBox(8, lbl, chk);
            kartica.getStyleClass().add("doc-card");
            box.getChildren().add(kartica);

            kriterijDokazi.add(new KriterijDokaz(sifra, naziv,
                    imaDokaz ? k.idDokaza() : null,
                    imaDokaz ? k.nazivDokaza() : null,
                    chk));
        }

        return sekcija("Dokazi za dodatne kriterije", box);
    }
    // ---------- podnošenje ----------

    @FXML
    private void onPodnesiPrijavu() {
        boolean brucos = nacrt.student.godinaStudija() == 1;
        List<DokumentUnosDTO> dokumenti = new ArrayList<>();

        // uspjeh
        Integer ispiti = null;
        if (!brucos) {
            ispiti = cijeli(txtIspiti);
            if (ispiti == null) {
                alert("Unesite broj položenih ispita (0 ako nema).");
                return;
            }
        }
        if (chkUspjeh.isSelected()) {
            if (cmbUspjeh.getValue() == null) {
                alert("Odaberite dokument o uspjehu.");
                return;
            }
            dokumenti.add(new DokumentUnosDTO(cmbUspjeh.getValue().id(), null, true, null));
        }

        // domaćinstvo
        boolean kucnaDostavljena = chkKucna != null && chkKucna.isSelected();
        if (kucnaDostavljena) {
            dokumenti.add(new DokumentUnosDTO(vrstaKucna.id(), null, true, null));
        }

        BigDecimal ukupnaPrimanja = BigDecimal.ZERO;
        boolean sviClanoviDokumentovani = true;
        for (int i = 0; i < clanovi.size(); i++) {
            ClanPolja c = clanovi.get(i);

            if (!c.primanja().getText().isBlank()) {
                BigDecimal p = decimalni(c.primanja());
                if (p == null) {
                    alert("Primanja za člana " + (i + 1) + " moraju biti broj.");
                    return;
                }
                ukupnaPrimanja = ukupnaPrimanja.add(p);
            }

            if (c.dostavljen().isSelected()) {
                if (c.vrsta().getValue() == null) {
                    alert("Odaberite dokument za člana " + (i + 1) + ".");
                    return;
                }
                String ko = c.ime().getText().isBlank() ? "član " + (i + 1) : c.ime().getText().trim();
                dokumenti.add(new DokumentUnosDTO(c.vrsta().getValue().id(),
                        c.vrsta().getValue().naziv() + " - " + ko, true, null));
            } else {
                sviClanoviDokumentovani = false;
            }
        }

        // kriteriji: dokaz se upisuje samo ako kriterij ima svoj dokument
        List<KriterijUnosDTO> kriteriji = new ArrayList<>();
        for (KriterijDokaz kd : kriterijDokazi) {
            boolean provjereno = kd.dostavljen().isSelected();
            if (provjereno && kd.idDokaza() != null) {
                dokumenti.add(new DokumentUnosDTO(kd.idDokaza(),
                        kd.nazivDokaza() + " – " + kd.naziv(), true, null));
            }
            kriteriji.add(new KriterijUnosDTO(kd.sifra(), nacrt.kriteriji.get(kd.sifra()), provjereno));
        }

        // ostali dokumenti
        boolean cipsDostavljen = false;
        for (OstaliDokument o : ostali) {
            if (!o.dostavljen().isSelected()) continue;
            dokumenti.add(new DokumentUnosDTO(o.vrsta().id(), null, true, null));
            if (NAZIV_CIPS.equalsIgnoreCase(o.vrsta().naziv())) cipsDostavljen = true;
        }

        PrijavaUnosDTO prijava = new PrijavaUnosDTO(
                nacrt.akademskaGodina, nacrt.prosjek, ispiti,
                nacrt.brojClanova, ukupnaPrimanja, nacrt.udaljenostKm, nacrt.napomena,
                chkUspjeh.isSelected(),                              // uspjeh verifikovan
                cipsDostavljen,                                      // udaljenost verifikovana
                kucnaDostavljena && sviClanoviDokumentovani);        // primanja verifikovana

        RucniUnosDTO zahtjev = new RucniUnosDTO(nacrt.student, prijava, kriteriji, dokumenti);

        Alert potvrda = new Alert(Alert.AlertType.CONFIRMATION,
                "Podnijeti prijavu za " + nacrt.student.ime() + " " + nacrt.student.prezime()
                        + " (" + nacrt.akademskaGodina + "/" + (nacrt.akademskaGodina + 1) + ")?");
        potvrda.setHeaderText(null);
        if (potvrda.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        posalji(zahtjev);
    }

    private void posalji(RucniUnosDTO zahtjev) {
        btnPodnesi.setDisable(true);

        Task<RucniUnosRezultatDTO> t = new Task<>() {
            @Override
            protected RucniUnosRezultatDTO call() {
                return RucniUnosApi.posalji(zahtjev);
            }
        };

        t.setOnSucceeded(e -> {
            RucniUnosRezultatDTO r = t.getValue();
            String poruka = "Prijava #" + r.idPrijava() + " je podnesena.\n\n"
                    + String.format("Bodovi: %.2f potvrđeno / %.2f moguće", r.potvrdjeniBodovi(), r.potencijalniBodovi())
                    + (r.noviNalog()
                    ? "\n\nZa studenta je kreiran nalog. Na " + zahtjev.student().email()
                    + " je poslana obavijest s uputom za postavljanje lozinke."
                    : "\n\nStudentu je poslana obavijest na " + zahtjev.student().email() + ".");
            alert(poruka);
            otvoriPrijave();
        });

        t.setOnFailed(e -> {
            btnPodnesi.setDisable(false);
            Throwable ex = t.getException();
            if (!(ex instanceof ApiException)) ex.printStackTrace();
            alert(ex instanceof ApiException
                    ? "Prijava nije podnesena:\n\n" + ex.getMessage()
                    : "Greška pri podnošenju prijave.");
        });

        Thread th = new Thread(t);
        th.setDaemon(true);
        th.start();
    }

    // ---------- navigacija ----------

    @FXML
    private void onNazad() {
        Ekrani.<NovaPrijavaController>zamijeni(accordionDokumenti, "/views/nova-prijava.fxml",
                c -> c.setNacrt(nacrt));
    }

    private void otvoriPrijave() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin-main-view.fxml"));
            Parent root = loader.load();
            AdminController admin = loader.getController();
            admin.loadViewPublic("/views/prijave.fxml");

            Stage stage = (Stage) accordionDokumenti.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------- pomoćne ----------

    private List<VrstaDokumentaDTO> poKategoriji(List<VrstaDokumentaDTO> sve, String kategorija) {
        return sve.stream().filter(v -> kategorija.equalsIgnoreCase(v.kategorija())).toList();
    }

    private ComboBox<VrstaDokumentaDTO> combo(List<VrstaDokumentaDTO> vrste, String podrazumijevanoSadrzi) {
        ComboBox<VrstaDokumentaDTO> cmb = new ComboBox<>(FXCollections.observableArrayList(vrste));
        cmb.getStyleClass().add("combo");
        cmb.setMaxWidth(Double.MAX_VALUE);
        cmb.setCellFactory(c -> vrstaCell());
        cmb.setButtonCell(vrstaCell());
        if (podrazumijevanoSadrzi != null) {
            vrste.stream()
                    .filter(v -> v.naziv().toLowerCase().contains(podrazumijevanoSadrzi.toLowerCase()))
                    .findFirst()
                    .ifPresent(cmb::setValue);
        }
        return cmb;
    }

    private ListCell<VrstaDokumentaDTO> vrstaCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(VrstaDokumentaDTO v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? "" : v.naziv());
            }
        };
    }

    private TitledPane sekcija(String naslov, Node sadrzaj) {
        TitledPane p = new TitledPane(naslov, sadrzaj);
        p.setAnimated(false);
        return p;
    }

    private Label opis(String tekst) {
        Label l = new Label(tekst);
        l.getStyleClass().add("doc-subtitle");
        l.setWrapText(true);
        return l;
    }

    private HBox red(Node... dijelovi) {
        HBox h = new HBox(12, dijelovi);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private Integer cijeli(TextField t) {
        try {
            return Integer.parseInt(t.getText().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal decimalni(TextField t) {
        try {
            return new BigDecimal(t.getText().trim().replace(',', '.'));
        } catch (Exception e) {
            return null;
        }
    }

    private void alert(String poruka) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, poruka);
        a.setHeaderText(null);
        a.showAndWait();
    }
}