package controllers;

import api.ApiException;
import api.PrijavaApi;
import ba.unze.edom.common.dto.*;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;

/** Tabela bodovanja sa svim kriterijima; admin mijenja podatke, kriterije i provjeru. */
public class BodovanjePanel extends VBox {

    private static final String P_BRUCOS = "brucoš: prosjek × 9 + 1";
    private static final String P_USPJEH = "prosjek × 3,5 + ispiti × 1,8 + godina (2.=3, 3.=5, 4.=7, 5.=9, 6.=10)";
    private static final String P_UPIS = "2. put i više: −3";
    private static final String P_UDALJENOST = "<50 km 0 · <80 4 · <120 8 · <160 12 · više 15";
    private static final String P_PRIMANJA = "po članu: <110 KM 20 · ≤150 15 · ≤200 8 · ≤300 4 · ≤400 2 · više 0";

    private final BodovanjeDetaljnoDTO d;
    private final Runnable nakonSnimanja;

    private final TextField tfGodina, tfUpis, tfProsjek, tfIspiti, tfKm, tfPrimanja, tfClanova;
    private final CheckBox chkUspjeh, chkUdaljenost, chkPrimanja;

    private record KRed(BodovanjeKriterijDTO k, CheckBox izabran, TextField param,
                        DatePicker od, DatePicker doDatuma, CheckBox ver) {}

    private final List<KRed> redovi = new ArrayList<>();

    public BodovanjePanel(BodovanjeDetaljnoDTO d, Runnable nakonSnimanja) {
        super(12);
        this.d = d;
        this.nakonSnimanja = nakonSnimanja;

        tfGodina = polje(d.godinaStudija(), "1–6", 50);
        tfUpis = polje(d.upisGodinePut(), "1", 45);
        tfProsjek = polje(d.prosjek(), "prosjek", 70);
        tfIspiti = polje(d.polozeniIspiti(), "ispita", 60);
        tfKm = polje(d.udaljenostKm(), "km", 70);
        tfPrimanja = polje(d.ukupnaPrimanja(), "KM ukupno", 90);
        tfClanova = polje(d.brojClanova(), "članova", 60);
        chkUspjeh = kvacica(d.uspjehVerifikovan());
        chkUdaljenost = kvacica(d.udaljenostVerifikovana());
        chkPrimanja = kvacica(d.primanjaVerifikovana());

        setPadding(new Insets(16));
        setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #e3e8ef; -fx-border-radius: 14;");

        getChildren().add(naslov("Bodovanje – svi kriteriji"));
        getChildren().add(sitno("Bodovi se računaju iz unesenih podataka. Promijenite podatak, označite ili skinite kriterij "
                + "i kliknite „Sačuvaj bodovanje“ — bodovi se preračunaju. ✓ = provjereno (ulazi u potvrđene bodove)."));

        if (d.greska() != null) getChildren().add(upozorenje("⚠ " + d.greska(), "#b3261e"));
        for (String u : d.upozorenja()) getChildren().add(upozorenje("⚠ " + u, "#b86e00"));

        getChildren().add(osnovni());
        Node branioci = kriteriji("BRANIOCI", "Branioci i socijalni kriteriji — boduje se samo najveća stavka (◄)");
        if (branioci != null) getChildren().add(branioci);
        Node dodatni = kriteriji("DODATNI", "Dodatni kriteriji — sabiraju se");
        if (dodatni != null) getChildren().add(dodatni);
        Node ostali = kriteriji(null, "Ostali kriteriji");
        if (ostali != null) getChildren().add(ostali);

        Label ukupno = new Label(String.format("Potvrđeno: %.2f   ·   moguće: %.2f", d.potvrdjeno(), d.potencijalno()));
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
                new HBox(6, sitno("god."), tfGodina, sitno("prosjek"), tfProsjek, sitno("ispiti"), tfIspiti),
                brucos ? P_BRUCOS : P_USPJEH, bodovi(uspjeh), chkUspjeh);
        red(g, 2, new Label("Ponovni upis godine"),
                new HBox(6, sitno("upisuje"), tfUpis, sitno(". put")),
                P_UPIS, bodovi(upis), sitno("uz uspjeh"));
        red(g, 3, new Label("Udaljenost"),
                new HBox(6, tfKm, sitno("km")),
                P_UDALJENOST, bodovi(udalj), chkUdaljenost);
        red(g, 4, new Label("Primanja domaćinstva"),
                new HBox(6, tfPrimanja, sitno("KM,"), tfClanova, sitno("članova")),
                P_PRIMANJA, bodovi(prim), chkPrimanja);
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
            DatePicker od = null, doDatuma = null;
            HBox unos = new HBox(6);
            unos.setAlignment(Pos.CENTER_LEFT);
            if (k.traziParametar()) {
                param = polje(k.parametar(), k.nazivParametra() != null ? k.nazivParametra() : "", 90);
                param.disableProperty().bind(izabran.selectedProperty().not());
                if (k.nazivParametra() != null) unos.getChildren().add(sitno(k.nazivParametra()));
                unos.getChildren().add(param);
            }
            if (k.traziPeriod()) {
                od = new DatePicker(k.datumOd());
                doDatuma = new DatePicker(k.datumDo());
                od.setPrefWidth(120);
                doDatuma.setPrefWidth(120);
                od.disableProperty().bind(izabran.selectedProperty().not());
                doDatuma.disableProperty().bind(izabran.selectedProperty().not());
                unos.getChildren().addAll(sitno("od"), od, sitno("do"), doDatuma);
            }

            Label b = new Label(k.izabran()
                    ? String.format("%.2f", k.bodovi()) + (k.boduje() ? "  ◄" : "")
                    : "—");
            if (k.boduje()) b.setStyle("-fx-font-weight: bold;");
            if (!k.izabran()) b.setStyle("-fx-text-fill: #9aa5b4;");

            CheckBox ver = kvacica(k.verifikovan());
            ver.disableProperty().bind(izabran.selectedProperty().not());

            red(g, red++, izabran, unos, k.pravilo(), b, ver);
            redovi.add(new KRed(k, izabran, param, od, doDatuma, ver));
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
                kr.add(new BodovanjeKriterijIzmjenaDTO(
                        r.k().idVrste(),
                        r.param() != null ? dec(r.param()) : null,
                        r.od() != null ? r.od().getValue() : null,
                        r.doDatuma() != null ? r.doDatuma().getValue() : null,
                        r.ver().isSelected()));
            }
            z = new BodovanjeIzmjenaDTO(
                    cijeli(tfGodina), cijeli(tfUpis), dec(tfProsjek), cijeli(tfIspiti),
                    dec(tfKm), cijeli(tfClanova), dec(tfPrimanja),
                    chkUspjeh.isSelected(), chkUdaljenost.isSelected(), chkPrimanja.isSelected(),
                    kr);
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
            BodovanjeDetaljnoDTO n = task.getValue();
            Alert a = new Alert(Alert.AlertType.INFORMATION,
                    String.format("Bodovanje je sačuvano. Potvrđeno: %.2f (moguće %.2f).", n.potvrdjeno(), n.potencijalno()));
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
        ColumnConstraints c1 = new ColumnConstraints(180, 320, Double.MAX_VALUE);
        c1.setHgrow(Priority.ALWAYS);
        ColumnConstraints c2 = new ColumnConstraints(140, 240, 320);
        ColumnConstraints c3 = new ColumnConstraints(70, 80, 100);
        ColumnConstraints c4 = new ColumnConstraints(40, 60, 80);
        g.getColumnConstraints().addAll(c0, c1, c2, c3, c4);

        String[] kol = {"Stavka", "Uneseno", "Pravilo", "Bodovi", "✓"};
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

    private void red(GridPane g, int red, Node stavka, Node unos, String pravilo, Node bodovi, Node ver) {
        Label p = sitno(pravilo);
        p.setWrapText(true);
        g.add(stavka, 0, red);
        g.add(unos, 1, red);
        g.add(p, 2, red);
        g.add(bodovi, 3, red);
        g.add(ver, 4, red);
    }

    private Label bodovi(StavkaBodovanja s) {
        Label l = new Label(s == null ? "0.00" : String.format("%.2f", s.bodovi()));
        if (s != null && s.obrazlozenje() != null) l.setTooltip(new Tooltip(s.obrazlozenje()));
        return l;
    }

    private static CheckBox kvacica(boolean v) {
        CheckBox c = new CheckBox();
        c.setSelected(v);
        return c;
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

    private static Label naslov(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        return l;
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