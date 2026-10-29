package controllers;

import api.ApiException;
import api.PostavkeApi;
import ba.unze.edom.common.dto.PostavkeDTO;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Ekran "Postavke": konkurs, rang lista, uplatnica, Studentski centar, ugovor, kriteriji bodovanja. */
public class PostavkePanel extends VBox {

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd.MM.yyyy.");
    private static final RecordComponent[] KOMPONENTE = PostavkeDTO.class.getRecordComponents();
    private static final String KRITERIJI = "Kriteriji bodovanja";

    /** Dio (pilula), ključ (ime polja u PostavkeDTO), naziv, pomoć (može null). */
    private record Polje(String dio, String kljuc, String naziv, String pomoc) {}

    private static final List<Polje> POLJA = List.of(
            new Polje("Konkurs", "akademskaGodina", "Akademska godina konkursa", "Upišite prvu godinu, npr. 2026 za 2026/2027."),
            new Polje("Konkurs", "konkursObjavljen", "Konkurs objavljen", null),
            new Polje("Konkurs", "prijaveOd", "Prijave od", null),
            new Polje("Konkurs", "prijaveDo", "Krajnji rok za prijave", "Poslije ovog dana studenti na webu ne mogu podnijeti novu prijavu. Prazno = bez roka."),
            new Polje("Konkurs", "kapacitet", "Kapacitet doma (broj mjesta)", null),
            new Polje("Konkurs", "udioBrucosaPosto", "Udio mjesta za brucoše (%)", null),
            new Polje("Konkurs", "minUdaljenostKm", "Minimalna udaljenost (km)", "Studenti s prebivalištem bliže od ovoga nemaju pravo na smještaj."),

            new Polje("Rang lista i useljenje", "rokZalbeDana", "Rok za žalbe (dana od objave)", null),
            new Polje("Rang lista i useljenje", "useljenjeRadnoVrijeme", "Radno vrijeme useljenja", "npr. 8:30 – 14:00"),
            new Polje("Rang lista i useljenje", "uplataPriUseljenju", "Uplata pri useljenju (KM)", "Iznos za prvi mjesec koji student donosi pri useljenju, npr. 150,00"),

            new Polje("Uplatnica", "iznosObrade", "Iznos za obradu prijave (KM)", null),
            new Polje("Uplatnica", "racunUplate", "Račun (JRT)", null),
            new Polje("Uplatnica", "budzetskaOrganizacija", "Budžetska organizacija", null),
            new Polje("Uplatnica", "pozivNaBroj", "Poziv na broj", null),
            new Polje("Uplatnica", "vrstaPrihoda", "Vrsta prihoda", null),

            new Polje("Studentski centar", "direktor", "Direktor(ica)", "Ime s titulom, npr. mr. sc. Ime Prezime"),
            new Polje("Studentski centar", "direktorFunkcija", "Funkcija (za potpis)", "DIREKTORICA ili DIREKTOR"),
            new Polje("Studentski centar", "adresa", "Adresa", null),
            new Polje("Studentski centar", "telefon", "Telefon", null),
            new Polje("Studentski centar", "email", "E-mail za informacije", null),
            new Polje("Studentski centar", "brojOdlukeKonkurs", "Odluka o konkursu (broj i datum)", null),
            new Polje("Studentski centar", "brojOdlukeKriteriji", "Odluka o kriterijima (broj i datum)", null),
            new Polje("Studentski centar", "brojRangListe", "Broj za odluku o rang listi", "npr. 09-130-020"),

            new Polje("Ugovor", "cijenaSmjestaja", "Cijena smještaja i ishrane mjesečno (KM)", null),
            new Polje("Ugovor", "ugovorPeriodOd", "Period ugovora od", "npr. 01.10."),
            new Polje("Ugovor", "ugovorPeriodDo", "Period ugovora do", "npr. 15.07."),

            new Polje(KRITERIJI, "brucosMnozilac", "Brucoš: prosjek ×", "Uspjeh brucoša = prosjek × ovaj broj + dodatak (sada 9)"),
            new Polje(KRITERIJI, "brucosDodatak", "Brucoš: + dodatak", "sada 1"),
            new Polje(KRITERIJI, "prosjekMnozilac", "Ostali: prosjek ×", "Uspjeh = prosjek × ovaj broj + ispiti × množilac + bonus za godinu (sada 3,5)"),
            new Polje(KRITERIJI, "ispitMnozilac", "Ostali: položeni ispiti ×", "sada 1,8"),
            new Polje(KRITERIJI, "bonusGodine", "Bonus za godinu studija", "godina:bodovi, odvojeno sa ;   npr. 2:3; 3:5; 4:7; 5:9; 6:10; apsolvent:0; postdiplomac:10"),
            new Polje(KRITERIJI, "kaznaPonovniUpis", "Ponovni upis iste godine", "Bodovi za 2. upis i dalje (negativno), npr. -3. Upišite 0 da se ne oduzima."),
            new Polje(KRITERIJI, "udaljenostBodovi", "Udaljenost (km)", "< manje od, <= do i uključivo, * sve ostalo.   npr. <50:0; <80:4; <120:8; <160:12; *:15"),
            new Polje(KRITERIJI, "primanjaBodovi", "Primanja po članu (KM)", "npr. <110:20; <=150:15; <=200:8; <=300:4; <=400:2; *:0")
    );

    /** Jedna rečenica ispod naslova svakog dijela. */
    private static final Map<String, String> OPISI = Map.of(
            "Konkurs", "Godina konkursa, rok za prijave i kapacitet. Rok određuje kada studenti na webu mogu podnijeti prijavu.",
            "Rang lista i useljenje", "Tekstovi koji se prikazuju na web rang listi i u PDF-u odluke o rang listi.",
            "Uplatnica", "Podaci za uplatu troškova obrade prijave (prijavni obrazac i upute na webu).",
            "Studentski centar", "Kontakt i potpis koji se ispisuju na odlukama, rang listi i ugovorima.",
            "Ugovor", "Cijena i period koji se upisuju u ugovor o smještaju i ishrani.");

    private final Map<String, Control> kontrole = new HashMap<>();
    private final Map<String, Node> dijelovi = new LinkedHashMap<>();
    private final Label lblStatus = new Label("Učitavanje…");
    private final Label lblInfo = new Label();
    private final Button btnSacuvaj = new Button("Sačuvaj postavke");
    private final FlowPane pilule = new FlowPane(8, 8);
    private final ToggleGroup grupa = new ToggleGroup();
    private final ScrollPane sadrzaj = new ScrollPane();
    private boolean ucitavam = true;

    public PostavkePanel() {
        super(16);
        setPadding(new Insets(24, 28, 24, 28));
        getStyleClass().add("pr-root");
        var css = getClass().getResource("/styles/prijave.css");
        if (css != null) getStylesheets().add(css.toExternalForm());

        // ---------- zaglavlje ----------
        Label eyebrow = new Label("ADMINISTRACIJA");
        eyebrow.getStyleClass().add("pr-hero-eyebrow");
        Label naslov = new Label("Postavke");
        naslov.getStyleClass().add("pr-hero-naslov");
        Label opis = new Label("Konkurs, rang lista, uplate, Studentski centar i bodovanje. "
                + "Koristi se na webu, u rang listi, prijavnom obrascu i ugovorima.");
        opis.getStyleClass().add("pr-hero-podnaslov");
        opis.setWrapText(true);

        btnSacuvaj.getStyleClass().add("pr-btn-bijelo");
        btnSacuvaj.setDisable(true);
        btnSacuvaj.setTooltip(new Tooltip("Čuva polja iz svih dijelova. Tabela kriterija se čuva odmah, pri svakoj izmjeni."));
        btnSacuvaj.setOnAction(e -> sacuvaj());

        Region r1 = new Region();
        HBox.setHgrow(r1, Priority.ALWAYS);
        HBox gore = new HBox(12, new VBox(2, eyebrow, naslov, opis), r1, btnSacuvaj);
        gore.setAlignment(Pos.CENTER_LEFT);

        lblStatus.getStyleClass().add("pr-hero-objava");
        lblInfo.getStyleClass().add("pr-hero-podnaslov");
        HBox dole = new HBox(14, lblStatus, lblInfo);
        dole.setAlignment(Pos.CENTER_LEFT);

        VBox hero = new VBox(14, gore, dole);
        hero.getStyleClass().add("pr-hero");

        // ---------- dijelovi ----------
        napraviDijelove();

        sadrzaj.getStyleClass().add("pr-scroll");
        sadrzaj.setFitToWidth(true);
        sadrzaj.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(sadrzaj, Priority.ALWAYS);

        getChildren().addAll(hero, pilule, sadrzaj);
        prikaziDio("Konkurs");
        ucitaj();
    }

    // =====================================================================
    //  IZGLED
    // =====================================================================

    private void napraviDijelove() {
        Map<String, Class<?>> tipovi = new HashMap<>();
        for (RecordComponent rc : KOMPONENTE) tipovi.put(rc.getName(), rc.getType());

        Map<String, GridPane> mreze = new LinkedHashMap<>();
        for (Polje p : POLJA) {
            GridPane g = mreze.computeIfAbsent(p.dio(), k -> novaMreza());
            int red = g.getRowCount();

            Label n = new Label(p.naziv());
            n.getStyleClass().add("pr-polje-naslov");
            n.setWrapText(true);

            Control c;
            if (tipovi.get(p.kljuc()) == LocalDate.class) {
                DatePicker dp = new DatePicker();
                dp.setPrefWidth(220);
                dp.valueProperty().addListener((o, a, b) -> izmijenjeno());
                c = dp;
            } else {
                TextField tf = new TextField();
                tf.setMaxWidth(520);
                tf.textProperty().addListener((o, a, b) -> izmijenjeno());
                c = tf;
            }
            c.getStyleClass().add("pr-polje");
            kontrole.put(p.kljuc(), c);

            g.add(n, 0, red);
            g.add(c, 1, red);
            if (p.pomoc() != null) {
                Label h = new Label(p.pomoc());
                h.getStyleClass().add("pr-sitno");
                h.setWrapText(true);
                h.setMaxWidth(520);
                GridPane.setMargin(h, new Insets(-4, 0, 6, 0));
                g.add(h, 1, red + 1);
            }
        }

        for (Map.Entry<String, GridPane> e : mreze.entrySet()) {
            String ime = e.getKey();
            if (KRITERIJI.equals(ime)) continue;
            Label n = new Label(ime);
            n.getStyleClass().add("pr-kartica-naslov");
            Label o = new Label(OPISI.getOrDefault(ime, ""));
            o.getStyleClass().add("pr-sitno");
            o.setWrapText(true);
            VBox kartica = new VBox(10, n, o, new Separator(), e.getValue());
            kartica.getStyleClass().add("pr-kartica");
            dijelovi.put(ime, kartica);
        }
        dijelovi.put(KRITERIJI, new KriterijiTab(mreze.get(KRITERIJI)));

        for (String ime : dijelovi.keySet()) {
            ToggleButton t = new ToggleButton(ime);
            t.getStyleClass().add("pr-chip");
            t.setToggleGroup(grupa);
            t.setUserData(ime);
            t.setOnAction(e -> prikaziDio(ime));
            pilule.getChildren().add(t);
        }
    }

    private GridPane novaMreza() {
        GridPane g = new GridPane();
        g.setHgap(20);
        g.setVgap(10);
        ColumnConstraints c0 = new ColumnConstraints(220, 250, 300);
        ColumnConstraints c1 = new ColumnConstraints(220, 460, Double.MAX_VALUE);
        c1.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c0, c1);
        return g;
    }

    private void prikaziDio(String ime) {
        sadrzaj.setContent(dijelovi.get(ime));
        sadrzaj.setVvalue(0);
        for (Toggle t : grupa.getToggles()) t.setSelected(ime.equals(t.getUserData()));
    }

    private void izmijenjeno() {
        if (ucitavam) return;
        lblInfo.setText("●  Imate nesačuvane izmjene");
        lblInfo.getStyleClass().setAll("label", "pr-hero-izmjena");
    }

    // =====================================================================
    //  UČITAVANJE / PRIKAZ
    // =====================================================================

    private void ucitaj() {
        pozadina(new Task<>() {
            @Override
            protected PostavkeDTO call() {
                return PostavkeApi.ucitaj();
            }
        }, "Postavke nisu učitane.", null);
    }

    private void popuni(PostavkeDTO p) {
        ucitavam = true;
        for (RecordComponent rc : KOMPONENTE) {
            Control c = kontrole.get(rc.getName());
            if (c == null) continue;
            Object v = vrijednost(rc, p);
            if (c instanceof DatePicker dp) dp.setValue((LocalDate) v);
            else if (c instanceof TextField tf) tf.setText(v == null ? "" : v.toString());
        }
        ucitavam = false;

        lblInfo.getStyleClass().setAll("label", "pr-hero-podnaslov");
        lblInfo.setText(p.izmijenjeno() != null
                ? "Zadnja izmjena: " + p.izmijenjeno() + " (" + p.izmijenio() + ")"
                : "Postavke još nisu mijenjane – prikazane su podrazumijevane vrijednosti.");
        prikaziStatus(p);
        btnSacuvaj.setDisable(false);
    }

    private void prikaziStatus(PostavkeDTO p) {
        LocalDate danas = LocalDate.now();
        boolean prije = p.prijaveOd() != null && danas.isBefore(p.prijaveOd());
        boolean poslije = p.prijaveDo() != null && danas.isAfter(p.prijaveDo());
        String godina = p.akademskaGodina() != null ? p.akademskaGodina() + "/" + (p.akademskaGodina() + 1) : "";

        lblStatus.getStyleClass().removeAll("pr-hero-objava--da", "pr-hero-objava--ne", "pr-hero-objava--uskoro");
        if (prije) {
            lblStatus.setText("●  Konkurs " + godina + ": prijave počinju " + p.prijaveOd().format(D));
            lblStatus.getStyleClass().add("pr-hero-objava--uskoro");
        } else if (poslije) {
            lblStatus.setText("●  Konkurs " + godina + ": prijave su zatvorene (rok je istekao " + p.prijaveDo().format(D) + ")");
            lblStatus.getStyleClass().add("pr-hero-objava--ne");
        } else {
            lblStatus.setText("●  Konkurs " + godina + ": prijave su otvorene"
                    + (p.prijaveDo() != null ? " do " + p.prijaveDo().format(D) : " (krajnji rok nije postavljen)"));
            lblStatus.getStyleClass().add("pr-hero-objava--da");
        }
    }

    // =====================================================================
    //  SNIMANJE
    // =====================================================================

    private void sacuvaj() {
        Object[] v = new Object[KOMPONENTE.length];
        for (int i = 0; i < KOMPONENTE.length; i++) {
            RecordComponent rc = KOMPONENTE[i];
            Control c = kontrole.get(rc.getName());
            if (c == null) continue;                       // izmijenio / izmijenjeno puni server
            if (c instanceof DatePicker dp) {
                v[i] = dp.getValue();
            } else {
                String s = ((TextField) c).getText() == null ? "" : ((TextField) c).getText().trim();
                if (rc.getType() == Integer.class) {
                    if (s.isEmpty()) {
                        v[i] = null;
                    } else {
                        try {
                            v[i] = Integer.valueOf(s);
                        } catch (NumberFormatException e) {
                            upozori("Polje „" + naziv(rc.getName()) + "“ mora biti cijeli broj.");
                            prikaziDio(dioPolja(rc.getName()));
                            c.requestFocus();
                            return;
                        }
                    }
                } else {
                    v[i] = s.isEmpty() ? null : s;
                }
            }
        }

        PostavkeDTO z;
        try {
            Class<?>[] tipovi = Arrays.stream(KOMPONENTE).map(RecordComponent::getType).toArray(Class<?>[]::new);
            z = PostavkeDTO.class.getDeclaredConstructor(tipovi).newInstance(v);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            upozori("Postavke se ne mogu pripremiti za slanje.");
            return;
        }

        btnSacuvaj.setDisable(true);
        pozadina(new Task<>() {
            @Override
            protected PostavkeDTO call() {
                return PostavkeApi.sacuvaj(z);
            }
        }, "Postavke nisu sačuvane.", "Postavke su sačuvane.");
    }

    // =====================================================================
    //  POMOĆNE
    // =====================================================================

    private void pozadina(Task<PostavkeDTO> task, String greska, String uspjeh) {
        task.setOnSucceeded(e -> {
            popuni(task.getValue());
            if (uspjeh != null) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, uspjeh);
                a.setHeaderText(null);
                a.showAndWait();
            }
        });
        task.setOnFailed(e -> {
            btnSacuvaj.setDisable(false);
            Throwable ex = task.getException();
            if (!(ex instanceof ApiException)) ex.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR, ex instanceof ApiException ? ex.getMessage() : greska);
            a.setHeaderText(null);
            a.showAndWait();
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private static Object vrijednost(RecordComponent rc, PostavkeDTO p) {
        try {
            return rc.getAccessor().invoke(p);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static String naziv(String kljuc) {
        return POLJA.stream().filter(p -> p.kljuc().equals(kljuc)).map(Polje::naziv).findFirst().orElse(kljuc);
    }

    private static String dioPolja(String kljuc) {
        return POLJA.stream().filter(p -> p.kljuc().equals(kljuc)).map(Polje::dio).findFirst().orElse("Konkurs");
    }

    private static void upozori(String poruka) {
        Alert a = new Alert(Alert.AlertType.WARNING, poruka);
        a.setHeaderText(null);
        a.showAndWait();
    }
}