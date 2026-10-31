package controllers;

import api.ApiException;
import api.RucniUnosApi;
import ba.unze.edom.common.dto.bodovanje.RezultatBodovanja;
import ba.unze.edom.common.dto.prijava.RucniUnosDTO;
import ba.unze.edom.common.dto.prijava.RucniUnosRezultatDTO;
import ba.unze.edom.common.dto.student.StudentUnosDTO;
import ba.unze.edom.common.dto.bodovanje.VrstaKriterijaDTO;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import unos.NacrtPrijave;
import util.Ekrani;
import util.KoraciUnosa;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Korak 5: pregled svega, bodovi sa servera i podnošenje. */
public class PregledUnosaController {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

    @FXML private HBox stepper;
    @FXML private ProgressBar traka;
    @FXML private VBox vboxSazetak;
    @FXML private Label lblBodovi;
    @FXML private Label lblBodoviOpis;
    @FXML private VBox vboxUpozorenja;
    @FXML private TextArea txtNapomena;
    @FXML private Button btnPodnesi;

    private NacrtPrijave nacrt;

    @FXML
    public void initialize() {
        KoraciUnosa.popuni(stepper, traka, 5);
    }

    public void setNacrt(NacrtPrijave n) {
        this.nacrt = n;
        if (n.napomena != null) txtNapomena.setText(n.napomena);
        txtNapomena.textProperty().addListener((obs, o, v) -> nacrt.napomena = v);
        popuniSazetak();
        izracunaj();
    }

    // ---------- sažetak ----------

    private void popuniSazetak() {
        vboxSazetak.getChildren().clear();
        StudentUnosDTO s = nacrt.student;

        red("Student", s.ime() + " " + s.prezime() + " (JMBG " + s.jmbg() + ")");
        red("Lična karta", s.brojLicneKarte());
        red("Telefon roditelja", s.telefonRoditelja());
        red("Prebivalište", s.opcina() + ", " + s.kanton());
        red("Fakultet", s.fakultet() + ", " + godinaTekst(s.godinaStudija()));
        red("Akademska godina", nacrt.akademskaGodina + "/" + (nacrt.akademskaGodina + 1));
        red("Prosjek", nacrt.prosjek.toPlainString());
        if (!nacrt.brucos()) red("Položeni ispiti", String.valueOf(nacrt.polozeniIspiti));
        red("Upis godine", nacrt.upisGodinePut + ". put" + (nacrt.upisGodinePut >= 4 ? " ili više" : ""));
        red("Članova domaćinstva", String.valueOf(nacrt.brojClanova));
        red("Ukupna mjesečna primanja", nacrt.ukupnaPrimanja().toPlainString() + " KM");
        red("Udaljenost", nacrt.udaljenostKm.toPlainString() + " km");
        red("Kriteriji", kriterijiTekst());

        if (nacrt.spisak != null) {
            int ukupno = nacrt.spisak.obavezni().size();
            red("Obavezni dokumenti", (ukupno - nacrt.falaObaveznih()) + " od " + ukupno + " dostavljeno");
        }
    }

    private String kriterijiTekst() {
        if (nacrt.kriteriji.isEmpty()) return "nijedan";

        List<String> redovi = new ArrayList<>();
        nacrt.kriteriji.forEach((sifra, param) -> {
            VrstaKriterijaDTO v = nacrt.sifarnici.kriteriji().stream()
                    .filter(x -> x.sifra().equals(sifra)).findFirst().orElse(null);

            StringBuilder sb = new StringBuilder(v != null ? v.naziv() : sifra);
            if (param != null) {
                sb.append(" (").append(v != null && v.nazivParametra() != null ? v.nazivParametra() + ": " : "")
                        .append(param.toPlainString()).append(")");
            }
            NacrtPrijave.Period p = nacrt.periodi.get(sifra);
            if (p != null) {
                sb.append(", od ").append(p.od().format(DATUM)).append(" do ").append(p.doDatuma().format(DATUM));
            }
            redovi.add(sb.toString());
        });
        return String.join("\n", redovi);
    }

    private void red(String naziv, String vrijednost) {
        Label k = new Label(naziv);
        k.getStyleClass().add("field-label");
        k.setMinWidth(220);
        k.setPrefWidth(220);

        Label v = new Label(vrijednost);
        v.getStyleClass().add("doc-subtitle");
        v.setWrapText(true);

        vboxSazetak.getChildren().add(new HBox(16, k, v));
    }

    // ---------- bodovi ----------

    private void izracunaj() {
        btnPodnesi.setDisable(true);
        lblBodovi.setText("…");
        lblBodoviOpis.setText("Računam bodove...");
        vboxUpozorenja.getChildren().clear();

        RucniUnosDTO zahtjev = nacrt.zahtjev();

        Task<RezultatBodovanja> t = new Task<>() {
            @Override
            protected RezultatBodovanja call() {
                return RucniUnosApi.pregledBodova(zahtjev);
            }
        };

        t.setOnSucceeded(e -> {
            RezultatBodovanja r = t.getValue();
            lblBodovi.setText(String.format("%.2f bodova", r.potvrdjeno()));
            lblBodoviOpis.setText(r.cekaVerifikaciju()
                    ? String.format("Potvrđeno odmah. Nakon verifikacije preostalih dokumenata moguće je %.2f.",
                    r.potencijalno())
                    : "Svi bodovi su potvrđeni.");

            if (r.imaUpozorenja()) {
                for (String u : r.upozorenja()) {
                    Label l = new Label("⚠ " + u);
                    l.getStyleClass().add("field-label");
                    l.setWrapText(true);
                    vboxUpozorenja.getChildren().add(l);
                }
            }
            btnPodnesi.setDisable(false);
        });

        t.setOnFailed(e -> {
            Throwable ex = t.getException();
            lblBodovi.setText("—");
            if (ex instanceof ApiException) {
                // npr. student već ima prijavu za tu godinu: podnošenje ne bi prošlo
                lblBodoviOpis.setText("Prijava se ne može podnijeti: " + ex.getMessage()
                        + "\nVratite se nazad i ispravite podatke.");
            } else {
                ex.printStackTrace();
                lblBodoviOpis.setText("Bodovi nisu izračunati (veza sa serverom). Prijavu možete ipak podnijeti.");
                btnPodnesi.setDisable(false);
            }
        });

        pokreni(t);
    }

    // ---------- podnošenje ----------

    @FXML
    private void onPodnesi() {
        nacrt.napomena = txtNapomena.getText();
        RucniUnosDTO zahtjev = nacrt.zahtjev();

        Alert potvrda = new Alert(Alert.AlertType.CONFIRMATION,
                "Podnijeti prijavu za " + nacrt.student.ime() + " " + nacrt.student.prezime()
                        + " (" + nacrt.akademskaGodina + "/" + (nacrt.akademskaGodina + 1) + ")?");
        potvrda.setHeaderText(null);
        if (potvrda.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        btnPodnesi.setDisable(true);

        Task<RucniUnosRezultatDTO> t = new Task<>() {
            @Override
            protected RucniUnosRezultatDTO call() {
                return RucniUnosApi.posalji(zahtjev);
            }
        };

        t.setOnSucceeded(e -> {
            RucniUnosRezultatDTO r = t.getValue();
            alert("Prijava #" + r.idPrijava() + " je podnesena.\n\n"
                    + String.format("Bodovi: %.2f potvrđeno / %.2f moguće", r.potvrdjeniBodovi(), r.potencijalniBodovi())
                    + (r.noviNalog()
                    ? "\n\nZa studenta je kreiran nalog. Na " + zahtjev.student().email()
                    + " je poslana obavijest s uputom za postavljanje lozinke."
                    : "\n\nStudentu je poslana obavijest na " + zahtjev.student().email() + "."));
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

        pokreni(t);
    }

    // ---------- navigacija ----------

    @FXML
    private void onNazad() {
        nacrt.napomena = txtNapomena.getText();
        Ekrani.<DodajDokumenteController>zamijeni(vboxSazetak, "/views/dodaj-dokumente.fxml",
                c -> c.setNacrt(nacrt));
    }

    private void otvoriPrijave() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin-main-view.fxml"));
            Parent root = loader.load();
            AdminController admin = loader.getController();
            admin.loadViewPublic("/views/prijave.fxml");

            Stage stage = (Stage) vboxSazetak.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------- pomoćne ----------

    private String godinaTekst(Integer g) {
        if (g == null) return "";
        return switch (g) {
            case 7 -> "apsolvent";
            case 8 -> "postdiplomac";
            default -> g + ". godina";
        };
    }

    private void pokreni(Task<?> t) {
        Thread th = new Thread(t);
        th.setDaemon(true);
        th.start();
    }

    private void alert(String poruka) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, poruka);
        a.setHeaderText(null);
        a.showAndWait();
    }
}