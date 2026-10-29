package util;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.URL;
import java.util.Map;

/**
 * Jedan dizajn za sve dijaloge (Alert, potvrde, unos, izbor, vlastiti Dialog):
 * stil iz /styles/dijalozi.css, ikona u krugu, dugmad i naslovi na bosanskom.
 * Uključuje se jednom u HelloApplication.start(); postojeći kod se ne mijenja.
 */
public final class Dijalozi {

    private static final Map<String, String> DUGMAD = Map.of(
            "Yes", "Da", "No", "Ne", "Cancel", "Odustani", "Close", "Zatvori",
            "Apply", "Primijeni", "Finish", "Završi", "Next", "Dalje", "Previous", "Nazad");

    private static final Map<String, String> NASLOVI = Map.of(
            "Information", "Obavijest", "Message", "Obavijest", "Confirmation", "Potvrda",
            "Error", "Greška", "Warning", "Upozorenje");

    private static boolean ukljuceno;

    private Dijalozi() {}

    public static void ukljuci(Image ikona) {
        if (ukljuceno) return;
        ukljuceno = true;

        URL url = Dijalozi.class.getResource("/styles/dijalozi.css");
        if (url == null) {
            System.err.println("dijalozi.css nije pronađen u /styles – dijalozi ostaju u starom izgledu.");
            return;
        }
        String css = url.toExternalForm();

        Window.getWindows().addListener((ListChangeListener<Window>) c -> {
            while (c.next()) {
                for (Window w : c.getAddedSubList()) pripremi(w, css, ikona);
            }
        });
    }

    private static void pripremi(Window w, String css, Image ikona) {
        if (w.getScene() != null) uredi(w, w.getScene(), css, ikona);
        else w.sceneProperty().addListener((o, a, s) -> { if (s != null) uredi(w, s, css, ikona); });
    }

    private static void uredi(Window w, Scene scene, String css, Image ikona) {
        if (!(scene.getRoot() instanceof DialogPane dp)) return;
        if (dp.getStyleClass().contains("edom-dijalog")) return;

        if (!scene.getStylesheets().contains(css)) scene.getStylesheets().add(css);
        dp.getStyleClass().add("edom-dijalog");

        if (w instanceof Stage st) {
            if (ikona != null && st.getIcons().isEmpty()) st.getIcons().add(ikona);

            // Alert bez zadanog naslova ima engleski naslov i isti takav podnaslov
            String naslov = st.getTitle();
            if (naslov != null && naslov.equals(dp.getHeaderText())) dp.setHeaderText(null);
            if (naslov != null && NASLOVI.containsKey(naslov)) st.setTitle(NASLOVI.get(naslov));
        }

        if (dp.getStyleClass().contains("alert")) dp.setGraphic(znak(dp));

        for (ButtonType bt : dp.getButtonTypes()) {
            Node n = dp.lookupButton(bt);
            if (n instanceof Button b && DUGMAD.containsKey(b.getText())) b.setText(DUGMAD.get(b.getText()));
        }

        // nakon promjena (ikona, bez podnaslova) prozor zauzme tačnu veličinu
        if (w instanceof Stage st) Platform.runLater(st::sizeToScene);
    }

    /** Ikona u krugu prema vrsti poruke. */
    private static Node znak(DialogPane dp) {
        String tip = dp.getStyleClass().contains("error") ? "greska"
                : dp.getStyleClass().contains("warning") ? "upozorenje"
                : dp.getStyleClass().contains("confirmation") ? "potvrda"
                : "info";
        String simbol = switch (tip) {
            case "greska" -> "✕";
            case "upozorenje" -> "!";
            case "potvrda" -> "?";
            default -> "i";
        };
        Label l = new Label(simbol);
        l.getStyleClass().addAll("edom-znak", "edom-znak--" + tip);
        return l;
    }
}