package service;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

/**
 * Otvara priloženi dokument bez obzira na format:
 * PDF → PdfService, slika (JPG/PNG/GIF/BMP) → pregled slike, ostalo → program iz Windowsa.
 * Tip se prepoznaje iz sadržaja, pa radi i za dokumente koji su već u bazi.
 */
public final class DokumentPregled {

    private static final double MIN_ZUM = 0.1;
    private static final double MAX_ZUM = 5.0;
    private static final double KORAK = 1.25;

    private DokumentPregled() {}

    public static void prikazi(String b64, String naziv) {
        if (b64 == null || b64.isBlank()) {
            poruka(Alert.AlertType.WARNING, "Dokument nema sadržaja.");
            return;
        }
        byte[] bajtovi;
        try {
            bajtovi = Base64.getMimeDecoder().decode(bezPrefiksa(b64));
        } catch (IllegalArgumentException e) {
            poruka(Alert.AlertType.ERROR, "Dokument nije ispravno sačuvan i ne može se otvoriti.");
            return;
        }

        String vrsta = vrsta(bajtovi);
        switch (vrsta) {
            case "pdf" -> PdfService.prikaziPdf(b64, naziv);
            case "png", "jpg", "gif", "bmp" -> prikaziSliku(bajtovi, naziv, vrsta);
            default -> otvoriVani(bajtovi, naziv, "bin");
        }
    }

    // =====================================================================
    //  PREGLED SLIKE
    // =====================================================================

    private static void prikaziSliku(byte[] bajtovi, String naziv, String ekstenzija) {
        Image slika = new Image(new ByteArrayInputStream(bajtovi));
        if (slika.isError()) {
            otvoriVani(bajtovi, naziv, ekstenzija);
            return;
        }
        String naslov = naziv != null && !naziv.isBlank() ? naziv : "Dokument";
        Stage stage = new Stage();

        // ---------- slika na "papiru" ----------
        ImageView iv = new ImageView(slika);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);

        StackPane papir = new StackPane(new Group(iv));
        papir.getStyleClass().add("pr-pregled-papir");
        papir.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane platno = new StackPane(papir);
        platno.getStyleClass().add("pr-pregled-platno");
        platno.setPadding(new Insets(28, 28, 90, 28));   // dolje mjesto za plutajuću traku

        ScrollPane sp = new ScrollPane(platno);
        sp.getStyleClass().add("pr-pregled-scroll");
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.setPannable(true);

        // ---------- zum ----------
        Label lblZum = new Label("100 %");
        lblZum.getStyleClass().add("pr-pregled-zoom");
        double[] zum = {1};
        boolean[] prilagodi = {true};

        Runnable primijeni = () -> {
            boolean polozena = Math.abs(iv.getRotate() % 180) == 90;
            double w = polozena ? slika.getHeight() : slika.getWidth();
            double h = polozena ? slika.getWidth() : slika.getHeight();
            if (prilagodi[0]) {
                double sirina = Math.max(100, sp.getViewportBounds().getWidth() - 80);
                double visina = Math.max(100, sp.getViewportBounds().getHeight() - 140);
                zum[0] = Math.min(Math.min(sirina / w, visina / h), 1.5);
            }
            iv.setFitWidth(slika.getWidth() * zum[0]);
            lblZum.setText(Math.round(zum[0] * 100) + " %");
        };
        Runnable uvecaj = () -> { prilagodi[0] = false; zum[0] = Math.min(MAX_ZUM, zum[0] * KORAK); primijeni.run(); };
        Runnable umanji = () -> { prilagodi[0] = false; zum[0] = Math.max(MIN_ZUM, zum[0] / KORAK); primijeni.run(); };
        Runnable naPrilagodi = () -> { prilagodi[0] = true; primijeni.run(); };
        Runnable rotiraj = () -> { iv.setRotate((iv.getRotate() + 90) % 360); primijeni.run(); };

        sp.viewportBoundsProperty().addListener((o, a, n) -> { if (prilagodi[0]) primijeni.run(); });
        sp.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (!e.isControlDown()) return;
            if (e.getDeltaY() > 0) uvecaj.run(); else umanji.run();
            e.consume();
        });
        papir.setOnMouseClicked(e -> {
            if (e.getClickCount() != 2) return;
            if (prilagodi[0]) { prilagodi[0] = false; zum[0] = 1; primijeni.run(); }
            else naPrilagodi.run();
        });

        // ---------- plutajuća traka dolje ----------
        HBox alati = new HBox(2,
                dugme("−", "Umanji (−)", umanji),
                lblZum,
                dugme("+", "Uvećaj (+)", uvecaj),
                new Separator(javafx.geometry.Orientation.VERTICAL),
                dugme("Prilagodi", "Prilagodi prozoru (0)", naPrilagodi),
                dugme("Rotiraj ↻", "Rotiraj za 90° (R)", rotiraj));
        alati.getStyleClass().add("pr-pregled-alati");
        alati.setAlignment(Pos.CENTER);
        alati.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane sredina = new StackPane(sp, alati);
        StackPane.setAlignment(alati, Pos.BOTTOM_CENTER);
        StackPane.setMargin(alati, new Insets(0, 0, 20, 0));

        // ---------- gornja traka ----------
        SVGPath ikona = new SVGPath();
        ikona.setContent("M21 19V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2zM8.5 13.5l2.5 3 3.5-4.5 4.5 6H5l3.5-4.5z");
        ikona.getStyleClass().add("pr-pregled-ikona-svg");
        StackPane ikonaBox = new StackPane(ikona);
        ikonaBox.getStyleClass().add("pr-pregled-ikona");

        Label lblNaslov = new Label(naslov);
        lblNaslov.getStyleClass().add("pr-pregled-naslov");
        Label lblOpis = new Label(ekstenzija.toUpperCase() + "   ·   " + (int) slika.getWidth() + " × "
                + (int) slika.getHeight() + " px   ·   dvoklik na sliku = stvarna veličina");
        lblOpis.getStyleClass().add("pr-hero-podnaslov");

        Button btnSacuvaj = new Button("Sačuvaj kao…");
        btnSacuvaj.getStyleClass().add("pr-btn-glass");
        btnSacuvaj.setOnAction(e -> sacuvajKao(stage, bajtovi, naslov, ekstenzija));

        Button btnVani = new Button("Otvori u programu");
        btnVani.getStyleClass().add("pr-btn-glass");
        btnVani.setOnAction(e -> otvoriVani(bajtovi, naslov, ekstenzija));

        Button btnZatvori = new Button("Zatvori");
        btnZatvori.getStyleClass().add("pr-btn-bijelo");
        btnZatvori.setOnAction(e -> stage.close());

        Region razmak = new Region();
        HBox.setHgrow(razmak, Priority.ALWAYS);
        HBox traka = new HBox(12, ikonaBox, new VBox(2, lblNaslov, lblOpis), razmak, btnSacuvaj, btnVani, btnZatvori);
        traka.setAlignment(Pos.CENTER_LEFT);
        traka.getStyleClass().addAll("pr-hero", "pr-pregled-traka");

        // ---------- prozor ----------
        BorderPane root = new BorderPane(sredina);
        root.setTop(traka);
        root.getStyleClass().add("pr-root");

        Scene scene = new Scene(root, 1050, 800);
        var css = DokumentPregled.class.getResource("/styles/prijave.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        scene.setOnKeyPressed(e -> {
            KeyCode k = e.getCode();
            if (k == KeyCode.ESCAPE) stage.close();
            else if (k == KeyCode.R) rotiraj.run();
            else if (k == KeyCode.DIGIT0 || k == KeyCode.NUMPAD0) naPrilagodi.run();
            else if (k == KeyCode.PLUS || k == KeyCode.ADD || k == KeyCode.EQUALS) uvecaj.run();
            else if (k == KeyCode.MINUS || k == KeyCode.SUBTRACT) umanji.run();
        });

        stage.setTitle(naslov);
        stage.setScene(scene);
        stage.show();
        Platform.runLater(primijeni);
    }

    private static Button dugme(String tekst, String opis, Runnable akcija) {
        Button b = new Button(tekst);
        b.setTooltip(new Tooltip(opis));
        b.setFocusTraversable(false);   // tastatura ostaje na prozoru (R, +, −, 0, Esc)
        b.setOnAction(e -> akcija.run());
        return b;
    }

    private static void sacuvajKao(Stage vlasnik, byte[] bajtovi, String naziv, String ekstenzija) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Sačuvaj dokument");
        fc.setInitialFileName(imeFajla(naziv) + "." + ekstenzija);
        File f = fc.showSaveDialog(vlasnik);
        if (f == null) return;
        try {
            Files.write(f.toPath(), bajtovi);
        } catch (Exception ex) {
            poruka(Alert.AlertType.ERROR, "Fajl nije sačuvan: " + ex.getMessage());
        }
    }

    // =====================================================================
    //  PREPOZNAVANJE I PROGRAM IZ WINDOWSA
    // =====================================================================

    /** "data:image/png;base64,AAAA…" → "AAAA…" */
    private static String bezPrefiksa(String b64) {
        String s = b64.trim();
        if (s.startsWith("data:")) {
            int zarez = s.indexOf(',');
            if (zarez > 0) s = s.substring(zarez + 1);
        }
        return s;
    }

    private static String vrsta(byte[] b) {
        if (b.length >= 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F') return "pdf";
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (b.length >= 4 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') return "gif";
        if (b.length >= 2 && b[0] == 'B' && b[1] == 'M') return "bmp";
        return "nepoznato";
    }

    private static void otvoriVani(byte[] bajtovi, String naziv, String ekstenzija) {
        try {
            File tmp = File.createTempFile(imeFajla(naziv) + "_", "." + ekstenzija);
            tmp.deleteOnExit();
            Files.write(tmp.toPath(), bajtovi);
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                poruka(Alert.AlertType.INFORMATION, "Dokument je sačuvan u:\n" + tmp.getAbsolutePath());
                return;
            }
            Thread t = new Thread(() -> {          // AWT ne smije blokirati JavaFX nit
                try {
                    Desktop.getDesktop().open(tmp);
                } catch (Exception ex) {
                    Platform.runLater(() ->
                            poruka(Alert.AlertType.INFORMATION, "Dokument je sačuvan u:\n" + tmp.getAbsolutePath()));
                }
            });
            t.setDaemon(true);
            t.start();
        } catch (Exception ex) {
            poruka(Alert.AlertType.ERROR, "Dokument se ne može otvoriti: " + ex.getMessage());
        }
    }

    private static String imeFajla(String naziv) {
        String s = naziv == null || naziv.isBlank() ? "dokument" : naziv;
        s = s.replaceAll("[\\\\/:*?\"<>|]+", "_").trim();
        return s.length() < 3 ? "dokument" : s;
    }

    private static void poruka(Alert.AlertType tip, String tekst) {
        Alert a = new Alert(tip, tekst);
        a.setHeaderText(null);
        a.showAndWait();
    }
}