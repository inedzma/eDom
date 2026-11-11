package controllers;

import api.ApiException;
import api.PrijavaApi;
import ba.unze.edom.common.dto.bodovanje.*;
import ba.unze.edom.common.dto.opste.PostavkeDTO;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** Tabela bodovanja sa svim kriterijima; admin ispravlja podatke i uklanja nepotkrijepljene kriterije. */
public class BodovanjePanel extends VBox {

    private final String P_BRUCOS, P_USPJEH, P_UPIS, P_UDALJENOST, P_PRIMANJA;
    private final BodovanjeDetaljnoDTO d;
    private final Runnable nakonSnimanja;

    private final TextField tfGodina, tfUpis, tfProsjek, tfIspiti, tfKm, tfPrimanja, tfClanova;

    private record KRed(BodovanjeKriterijDTO k, CheckBox izabran, TextField param) {}

    private final List<KRed> redovi = new ArrayList<>();

    public BodovanjePanel(BodovanjeDetaljnoDTO d, PostavkeDTO ps, Runnable nakonSnimanja) {
        super(12);
        P_BRUCOS = "brucoš: prosjek × " + ps.brucosMnozilac() + " + " + ps.brucosDodatak();
        P_USPJEH = "prosjek × " + ps.prosjekMnozilac() + " + ispiti × " + ps.ispitMnozilac()
                + " + bonus za godinu (" + ps.bonusGodine() + ")";
        P_UPIS = "2. put i više: " + ps.kaznaPonovniUpis();
        P_UDALJENOST = ps.udaljenostBodovi();
        P_PRIMANJA = "po članu (KM): " + ps.primanjaBodovi();
        this.d = d;
        this.nakonSnimanja = nakonSnimanja;

        tfGodina = polje(d.godinaStudija(), "1–6", 50);
        tfUpis = polje(d.upisGodinePut(), "1", 45);
        tfProsjek = polje(d.prosjek(), "prosjek", 70);
        tfIspiti = polje(d.polozeniIspiti(), "ispita", 60);
        tfKm = polje(d.udaljenostKm(), "km", 70);
        tfPrimanja = polje(d.ukupnaPrimanja(), "KM ukupno", 90);
        tfClanova = polje(d.brojClanova(), "članova", 60);

        setPadding(new Insets(16));
        getStyleClass().add("pr-kartica");

        Label nsl = new Label("Bodovanje – svi kriteriji");
        nsl.getStyleClass().add("pr-kartica-naslov");
        Label opis = sitno("Podatak koji nije potkrijepljen dokumentom ispravite, a kriterij bez dokumenta odznačite.");
        getChildren().addAll(nsl, opis);
        if (d.greska() != null) getChildren().add(upozorenje("⚠ " + d.greska(), "#b3261e"));
        for (String u : d.upozorenja()) getChildren().add(upozorenje("⚠ " + u, "#b86e00"));

        getChildren().add(osnovni());
        Node branioci = kriteriji("BRANIOCI", "Branioci i socijalni kriteriji — boduje se samo najveća stavka (◄)");
        if (branioci != null) getChildren().add(branioci);
        Node dodatni = kriteriji("DODATNI", "Dodatni kriteriji — sabiraju se");
        if (dodatni != null) getChildren().add(dodatni);
        Node ostali = kriteriji(null, "Ostali kriteriji");
        if (ostali != null) getChildren().add(ostali);

        Label ukupno = new Label(String.format("Ukupno: %.2f bodova", d.bodovi()));
        ukupno.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Button sacuvaj = new Button("Sačuvaj bodovanje");
        sacuvaj.getStyleClass().add("btn-primary");
        sacuvaj.setOnAction(e -> sacuvaj());
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        HBox dno = new HBox(12, ukupno, r, sacuvaj);
        dno.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(dno);
    }

    // ---------- osnovni ----------

    private Node osnovni() {
        GridPane g = tabela("Uspjeh, udaljenost, primanja");

        boolean brucos = Objects.equals(d.godinaStudija(), 1);
        StavkaBodovanja uspjeh = osnovna(s -> s.naziv().startsWith("Uspjeh"));
        StavkaBodovanja upis = osnovna(s -> s.naziv().startsWith("Ponovni upis"));
        StavkaBodovanja udalj = osnovna(s -> KategorijaBodovanja.UDALJENOST.equals(s.kategorija()));
        StavkaBodovanja prim = osnovna(s -> KategorijaBodovanja.PRIMANJA.equals(s.kategorija()));

        red(g, 1, new Label("Uspjeh"),
                unos(kratko("godina"), tfGodina, kratko("prosjek"), tfProsjek, kratko("ispiti"), tfIspiti),
                brucos ? P_BRUCOS : P_USPJEH, bodovi(uspjeh));
        red(g, 2, new Label("Ponovni upis godine"),
                unos(kratko("upisuje"), tfUpis, kratko(". put")),
                P_UPIS, bodovi(upis));
        red(g, 3, new Label("Udaljenost"),
                unos(tfKm, kratko("km")),
                P_UDALJENOST, bodovi(udalj));
        red(g, 4, new Label("Primanja domaćinstva"),
                unos(tfPrimanja, kratko("KM ukupno,"), tfClanova, kratko("članova")),
                P_PRIMANJA, bodovi(prim));
        return g.getParent();
    }

    private StavkaBodovanja osnovna(Predicate<StavkaBodovanja> uslov) {
        return d.osnovne().stream().filter(uslov).findFirst().orElse(null);
    }

    // ---------- kriteriji ----------

    private Node kriteriji(String grupa, String naslov) {
        List<BodovanjeKriterijDTO> lista = d.kriteriji().stream().filter(k -> grupa == null
                ? !"BRANIOCI".equals(k.grupa()) && !"DODATNI".equals(k.grupa())
                : grupa.equals(k.grupa())).toList();
        if (lista.isEmpty()) return null;

        GridPane g = tabela(naslov);
        int red = 1;
        for (BodovanjeKriterijDTO k : lista) {
            CheckBox izabran = new CheckBox(k.naziv());
            izabran.setSelected(k.izabran());
            izabran.setWrapText(true);
            izabran.setMaxWidth(230);

            TextField param = null;
            HBox unos = new HBox(6);
            unos.setAlignment(Pos.CENTER_LEFT);
            if (k.traziParametar()) {
                param = polje(k.parametar(), k.nazivParametra() != null ? k.nazivParametra() : "", 90);
                param.disableProperty().bind(izabran.selectedProperty().not());
                if (k.nazivParametra() != null) unos.getChildren().add(sitno(k.nazivParametra()));
                unos.getChildren().add(param);
            }

            Label b = new Label(k.izabran()
                    ? String.format("%.2f", k.bodovi()) + (k.boduje() ? "  ◄" : "")
                    : "—");
            if (k.boduje()) b.setStyle("-fx-font-weight: bold;");
            if (!k.izabran()) b.setStyle("-fx-text-fill: #9aa5b4;");

            red(g, red++, izabran, unos, k.pravilo(), b);
            redovi.add(new KRed(k, izabran, param));
        }
        return g.getParent();
    }

    // ---------- snimanje ----------

    private void sacuvaj() {
        BodovanjeIzmjenaDTO z;
        try {
            List<BodovanjeKriterijIzmjenaDTO> kr = new ArrayList<>();
            for (KRed r : redovi) {
                if (!r.izabran().isSelected()) continue;
                kr.add(new BodovanjeKriterijIzmjenaDTO(r.k().idVrste(), r.param() != null ? dec(r.param()) : null));
            }
            z = new BodovanjeIzmjenaDTO(
                    cijeli(tfGodina), cijeli(tfUpis), dec(tfProsjek), cijeli(tfIspiti),
                    dec(tfKm), cijeli(tfClanova), dec(tfPrimanja), kr);
        } catch (IllegalArgumentException ex) {
            new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
            return;
        }

        Task<BodovanjeDetaljnoDTO> task = new Task<>() {
            @Override
            protected BodovanjeDetaljnoDTO call() {
                return PrijavaApi.sacuvajBodovanje(d.idPrijava(), z);
            }
        };
        task.setOnSucceeded(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION,
                    String.format("Bodovanje je sačuvano. Ukupno: %.2f bodova.", task.getValue().bodovi()));
            a.setHeaderText(null);
            a.showAndWait();
            nakonSnimanja.run();
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (!(ex instanceof ApiException)) ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    ex instanceof ApiException ? ex.getMessage() : "Bodovanje nije sačuvano.").showAndWait();
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // ---------- pomoćne ----------

    /** Tabela s naslovom i zaglavljem; vraća GridPane, a njegov roditelj (VBox) ide u panel. */
    private GridPane tabela(String naslov) {
        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(8);
        ColumnConstraints c0 = new ColumnConstraints(200, 230, 260);
        ColumnConstraints c1 = new ColumnConstraints(330, 360, Double.MAX_VALUE);
        c1.setHgrow(Priority.ALWAYS);
        ColumnConstraints c2 = new ColumnConstraints(140, 240, 320);
        ColumnConstraints c3 = new ColumnConstraints(70, 80, 100);
        g.getColumnConstraints().addAll(c0, c1, c2, c3);

        String[] kol = {"Stavka", "Uneseno", "Pravilo", "Bodovi"};
        for (int i = 0; i < kol.length; i++) {
            Label l = new Label(kol[i]);
            l.setStyle("-fx-text-fill: #6b7a90; -fx-font-weight: bold;");
            g.add(l, i, 0);
        }

        Label n = new Label(naslov);
        n.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0b6b5c;");
        new VBox(6, n, g);   // g dobija roditelja
        return g;
    }

    /** Kratka oznaka uz polje (god., prosjek, km…) – ne lomi se u dva reda. */
    private static Label kratko(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-text-fill: #6b7a90;");
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    private static HBox unos(Node... dijelovi) {
        HBox h = new HBox(6, dijelovi);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private void red(GridPane g, int red, Node stavka, Node unos, String pravilo, Node bodovi) {
        Label p = sitno(pravilo);
        p.setWrapText(true);
        g.add(stavka, 0, red);
        g.add(unos, 1, red);
        g.add(p, 2, red);
        g.add(bodovi, 3, red);
    }

    private Label bodovi(StavkaBodovanja s) {
        Label l = new Label(s == null ? "0.00" : String.format("%.2f", s.bodovi()));
        if (s != null && s.obrazlozenje() != null) l.setTooltip(new Tooltip(s.obrazlozenje()));
        return l;
    }

    private static TextField polje(Object v, String prompt, double sirina) {
        String t = v == null ? "" : v instanceof BigDecimal b ? b.stripTrailingZeros().toPlainString() : v.toString();
        TextField f = new TextField(t);
        f.setPromptText(prompt);
        f.setPrefWidth(sirina);
        return f;
    }

    private static BigDecimal dec(TextField t) {
        String s = t.getText() == null ? "" : t.getText().trim().replace(',', '.');
        if (s.isEmpty()) return null;
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Neispravan broj: " + t.getText());
        }
    }

    private static Integer cijeli(TextField t) {
        String s = t.getText() == null ? "" : t.getText().trim();
        if (s.isEmpty()) return null;
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Neispravan cijeli broj: " + t.getText());
        }
    }

    private static Label sitno(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-text-fill: #6b7a90;");
        l.setWrapText(true);
        return l;
    }

    private static Label upozorenje(String t, String boja) {
        Label l = new Label(t);
        l.setWrapText(true);
        l.setStyle("-fx-text-fill: " + boja + "; -fx-font-weight: bold;");
        return l;
    }
}