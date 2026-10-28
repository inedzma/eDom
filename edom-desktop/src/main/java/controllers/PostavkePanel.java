package controllers;

import api.ApiException;
import api.PostavkeApi;
import ba.unze.edom.common.dto.PostavkeDTO;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Ekran "Postavke": konkurs, rang lista, uplatnica, Studentski centar, ugovor. */
public class PostavkePanel extends VBox {

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd.MM.yyyy.");
    private static final RecordComponent[] KOMPONENTE = PostavkeDTO.class.getRecordComponents();

    /** Kartica, ključ (ime polja u PostavkeDTO), naziv, pomoć (može null). */
    private record Polje(String kartica, String kljuc, String naziv, String pomoc) {}

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

            new Polje("Kriteriji bodovanja", "brucosMnozilac", "Brucoš: prosjek ×", "Uspjeh brucoša = prosjek × ovaj broj + dodatak (sada 9)"),
            new Polje("Kriteriji bodovanja", "brucosDodatak", "Brucoš: + dodatak", "sada 1"),
            new Polje("Kriteriji bodovanja", "prosjekMnozilac", "Ostali: prosjek ×", "Uspjeh = prosjek × ovaj broj + ispiti × množilac + bonus za godinu (sada 3,5)"),
            new Polje("Kriteriji bodovanja", "ispitMnozilac", "Ostali: položeni ispiti ×", "sada 1,8"),
            new Polje("Kriteriji bodovanja", "bonusGodine", "Bonus za godinu studija", "godina:bodovi, odvojeno sa ;   npr. 2:3; 3:5; 4:7; 5:9; 6:10; apsolvent:0; postdiplomac:10"),
            new Polje("Kriteriji bodovanja", "kaznaPonovniUpis", "Ponovni upis iste godine", "Bodovi za 2. upis i dalje (negativno), npr. -3. Upišite 0 da se ne oduzima."),
            new Polje("Kriteriji bodovanja", "udaljenostBodovi", "Udaljenost (km)", "< manje od, <= do i uključivo, * sve ostalo.   npr. <50:0; <80:4; <120:8; <160:12; *:15"),
            new Polje("Kriteriji bodovanja", "primanjaBodovi", "Primanja po članu (KM)", "npr. <110:20; <=150:15; <=200:8; <=300:4; <=400:2; *:0")
    );

    private final Map<String, Control> kontrole = new HashMap<>();
    private final Label lblStatus = new Label();
    private final Label lblInfo = new Label();
    private final Button btnSacuvaj = new Button("Sačuvaj postavke");
    private final TabPane kartice = new TabPane();

    public PostavkePanel() {
        super(12);
        setPadding(new Insets(24));

        Label naslov = new Label("Postavke");
        naslov.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        Label opis = new Label("Podaci o konkursu, rang listi, uplatama i Studentskom centru. "
                + "Koriste se na web stranicama, u rang listi, prijavnom obrascu i ugovorima.");
        opis.setWrapText(true);
        opis.setStyle("-fx-text-fill: #6b7a90;");
        lblStatus.setStyle("-fx-font-weight: bold;");

        kartice.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(kartice, Priority.ALWAYS);
        napraviKartice();

        btnSacuvaj.getStyleClass().add("btn-primary");
        btnSacuvaj.setDisable(true);
        btnSacuvaj.setOnAction(e -> sacuvaj());
        lblInfo.setStyle("-fx-text-fill: #6b7a90;");
        Region razmak = new Region();
        HBox.setHgrow(razmak, Priority.ALWAYS);
        HBox dno = new HBox(12, lblInfo, razmak, btnSacuvaj);
        dno.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(naslov, opis, lblStatus, kartice, dno);
        ucitaj();
    }

    // ---------- izgled ----------

    private void napraviKartice() {
        Map<String, Class<?>> tipovi = new HashMap<>();
        for (RecordComponent rc : KOMPONENTE) tipovi.put(rc.getName(), rc.getType());

        Map<String, GridPane> mreze = new LinkedHashMap<>();
        for (Polje p : POLJA) {
            GridPane g = mreze.computeIfAbsent(p.kartica(), k -> novaMreza());
            int red = g.getRowCount();

            Label n = new Label(p.naziv());
            n.setStyle("-fx-font-weight: bold;");
            n.setWrapText(true);

            Control c;
            if (tipovi.get(p.kljuc()) == LocalDate.class) {
                DatePicker dp = new DatePicker();
                dp.setPrefWidth(200);
                c = dp;
            } else {
                TextField tf = new TextField();
                tf.setPrefWidth(360);
                c = tf;
            }
            kontrole.put(p.kljuc(), c);

            g.add(n, 0, red);
            g.add(c, 1, red);
            if (p.pomoc() != null) {
                Label h = new Label(p.pomoc());
                h.setWrapText(true);
                h.setStyle("-fx-text-fill: #6b7a90; -fx-font-size: 12px;");
                g.add(h, 1, red + 1);
            }
        }

        GridPane pravila = mreze.remove("Kriteriji bodovanja");   // ide na vrh kartice s kriterijima
        mreze.forEach((ime, g) -> {
            ScrollPane sp = new ScrollPane(g);
            sp.setFitToWidth(true);
            kartice.getTabs().add(new Tab(ime, sp));
        });
        kartice.getTabs().add(new KriterijiTab(pravila));
    }

    private GridPane novaMreza() {
        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(8);
        g.setPadding(new Insets(18));
        ColumnConstraints c0 = new ColumnConstraints(220, 260, 320);
        ColumnConstraints c1 = new ColumnConstraints(200, 420, Double.MAX_VALUE);
        c1.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c0, c1);
        return g;
    }

    // ---------- učitavanje / prikaz ----------

    private void ucitaj() {
        pozadina(new Task<>() {
            @Override
            protected PostavkeDTO call() {
                return PostavkeApi.ucitaj();
            }
        }, "Postavke nisu učitane.");
    }

    private void popuni(PostavkeDTO p) {
        for (RecordComponent rc : KOMPONENTE) {
            Control c = kontrole.get(rc.getName());
            if (c == null) continue;
            Object v = vrijednost(rc, p);
            if (c instanceof DatePicker dp) dp.setValue((LocalDate) v);
            else if (c instanceof TextField tf) tf.setText(v == null ? "" : v.toString());
        }
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

        if (prije) {
            lblStatus.setText("● Konkurs " + godina + ": prijave počinju " + p.prijaveOd().format(D));
            lblStatus.setStyle("-fx-font-weight: bold; -fx-text-fill: #b86e00;");
        } else if (poslije) {
            lblStatus.setText("● Konkurs " + godina + ": prijave su zatvorene (rok je istekao " + p.prijaveDo().format(D) + ")");
            lblStatus.setStyle("-fx-font-weight: bold; -fx-text-fill: #b3261e;");
        } else {
            lblStatus.setText("● Konkurs " + godina + ": prijave su otvorene"
                    + (p.prijaveDo() != null ? " do " + p.prijaveDo().format(D) : " (krajnji rok nije postavljen)"));
            lblStatus.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e7a4f;");
        }
    }

    // ---------- snimanje ----------

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

    // ---------- pomoćne ----------

    private void pozadina(Task<PostavkeDTO> task, String greska) {
        pozadina(task, greska, null);
    }

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
            new Alert(Alert.AlertType.ERROR, ex instanceof ApiException ? ex.getMessage() : greska).showAndWait();
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

    private static void upozori(String poruka) {
        Alert a = new Alert(Alert.AlertType.WARNING, poruka);
        a.setHeaderText(null);
        a.showAndWait();
    }
}