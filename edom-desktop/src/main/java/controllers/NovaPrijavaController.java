package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import unos.NacrtPrijave;
import util.Ekrani;
import util.KoraciUnosa;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Korak 2: domaćinstvo (broj članova, udaljenost, članovi s primanjima). */
public class NovaPrijavaController {

    private static final int NAJVISE_CLANOVA = 20;

    @FXML private HBox stepper;
    @FXML private ProgressBar traka;
    @FXML private TextField txtClanovi;
    @FXML private TextField txtUdaljenost;
    @FXML private VBox vboxClanovi;
    @FXML private Label lblUkupno;

    private NacrtPrijave nacrt;

    private record ClanPolja(TextField ime, TextField primanja) {}

    private final List<ClanPolja> polja = new ArrayList<>();

    @FXML
    public void initialize() {
        KoraciUnosa.popuni(stepper, traka, 2);
        txtClanovi.textProperty().addListener((obs, o, n) -> napraviClanove());
    }

    public void setNacrt(NacrtPrijave n) {
        this.nacrt = n;
        if (n.udaljenostKm != null) txtUdaljenost.setText(n.udaljenostKm.toPlainString());
        if (n.brojClanova != null) txtClanovi.setText(String.valueOf(n.brojClanova));
        napraviClanove();
    }

    // ---------- članovi ----------

    private void napraviClanove() {
        if (nacrt == null) return;
        sacuvajClanove();
        vboxClanovi.getChildren().clear();
        polja.clear();

        Integer broj = cijeli(txtClanovi);
        if (broj == null || broj < 1) {
            vboxClanovi.getChildren().add(opis("Unesite broj članova domaćinstva, zajedno sa studentom."));
            osvjeziUkupno();
            return;
        }

        int ostalih = Math.min(broj, NAJVISE_CLANOVA) - 1;
        vboxClanovi.getChildren().add(opis(ostalih == 0
                ? "Student živi sam: nema drugih članova."
                : "Ostali članovi domaćinstva, bez studenta (" + ostalih + "):"));

        for (int i = 0; i < ostalih; i++) {
            NacrtPrijave.Clan stari = i < nacrt.clanovi.size() ? nacrt.clanovi.get(i) : null;

            TextField ime = new TextField(stari != null && stari.ime() != null ? stari.ime() : "");
            ime.setPromptText("Ime / uloga (npr. majka)");
            ime.getStyleClass().add("field");
            HBox.setHgrow(ime, Priority.ALWAYS);

            TextField primanja = new TextField(stari != null && stari.primanja() != null
                    ? stari.primanja().toPlainString() : "");
            primanja.setPromptText("Mjesečna primanja (KM)");
            primanja.getStyleClass().add("field");
            primanja.setPrefWidth(220);
            primanja.textProperty().addListener((obs, o, n) -> osvjeziUkupno());

            Label naslov = new Label("Član " + (i + 1));
            naslov.getStyleClass().add("member-title");

            VBox kartica = new VBox(10, naslov, new HBox(12, ime, primanja));
            kartica.getStyleClass().add("household-member-card");
            vboxClanovi.getChildren().add(kartica);

            polja.add(new ClanPolja(ime, primanja));
        }
        osvjeziUkupno();
    }

    /** Ono što je upisano ne smije nestati kad se promijeni broj članova. */
    private void sacuvajClanove() {
        if (polja.isEmpty()) return;
        nacrt.clanovi.clear();
        for (ClanPolja p : polja) {
            nacrt.clanovi.add(new NacrtPrijave.Clan(p.ime().getText(), decimalni(p.primanja())));
        }
    }

    private void osvjeziUkupno() {
        BigDecimal ukupno = BigDecimal.ZERO;
        for (ClanPolja p : polja) {
            BigDecimal b = decimalni(p.primanja());
            if (b != null) ukupno = ukupno.add(b);
        }
        lblUkupno.setText("Ukupna mjesečna primanja domaćinstva: " + ukupno.toPlainString() + " KM");
    }

    // ---------- navigacija ----------

    @FXML
    private void onDalje() {
        Integer clanovi = cijeli(txtClanovi);
        BigDecimal udaljenost = decimalni(txtUdaljenost);

        if (clanovi == null || clanovi < 1) {
            alert("Broj članova domaćinstva je obavezan (najmanje 1, sa studentom).");
            return;
        }
        if (clanovi > NAJVISE_CLANOVA) {
            alert("Najviše " + NAJVISE_CLANOVA + " članova domaćinstva.");
            return;
        }
        if (udaljenost == null || udaljenost.signum() < 0) {
            alert("Udaljenost je obavezna i mora biti broj, npr. 43.");
            return;
        }

        List<NacrtPrijave.Clan> novi = new ArrayList<>();
        for (int i = 0; i < polja.size(); i++) {
            ClanPolja p = polja.get(i);
            BigDecimal primanja = null;
            if (!p.primanja().getText().isBlank()) {
                primanja = decimalni(p.primanja());
                if (primanja == null || primanja.signum() < 0) {
                    alert("Primanja za člana " + (i + 1) + " moraju biti broj (0 ako nema primanja).");
                    return;
                }
            }
            novi.add(new NacrtPrijave.Clan(p.ime().getText().trim(), primanja));
        }

        nacrt.brojClanova = clanovi;
        nacrt.udaljenostKm = udaljenost;
        nacrt.clanovi.clear();
        nacrt.clanovi.addAll(novi);

        Ekrani.<KriterijiUnosController>zamijeni(txtClanovi, "/views/kriteriji-unos.fxml",
                c -> c.setNacrt(nacrt));
    }

    @FXML
    private void onNazad() {
        sacuvajClanove();
        Integer clanovi = cijeli(txtClanovi);
        if (clanovi != null) nacrt.brojClanova = clanovi;
        BigDecimal udaljenost = decimalni(txtUdaljenost);
        if (udaljenost != null) nacrt.udaljenostKm = udaljenost;

        Ekrani.<NoviStudentController>zamijeni(txtClanovi, "/views/novi-student.fxml",
                c -> c.setNacrt(nacrt));
    }

    // ---------- pomoćne ----------

    private Label opis(String tekst) {
        Label l = new Label(tekst);
        l.getStyleClass().add("doc-subtitle");
        l.setWrapText(true);
        return l;
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