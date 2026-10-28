package controllers;

import api.ApiException;
import api.PostavkeApi;
import api.RucniUnosApi;
import ba.unze.edom.common.dto.*;
import ba.unze.edom.common.util.Fakulteti;
import ba.unze.edom.common.util.Imena;
import ba.unze.edom.common.util.Jmbg;
import ba.unze.edom.common.util.Opcine;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Korak 1: student, lični podaci i studij. */
public class NoviStudentController {

    private static final List<String> FAKULTETI = List.of(
            "Politehnički fakultet UNZE",
            "Pravni fakultet UNZE",
            "Ekonomski fakultet UNZE",
            "Filozofski fakultet UNZE",
            "Medicinski fakultet UNZE",
            "Mašinski fakultet UNZE",
            "Islamsko pedagoški fakultet UNZE",
            "Fakultet inženjerstva i prirodnih nauka UNZE"
    );
    private static final List<String> GODINE =
            List.of("1", "2", "3", "4", "5", "6", "Apsolvent", "Postdiplomac");

    /** Redni broj u listi + 1 = upisGodinePut (zadnja stavka znači 4 ili više). */
    private static final List<String> UPIS = List.of(
            "1. put (prvi upis)", "2. put (ponavlja godinu)", "3. put", "4. put ili više");

    @FXML private HBox stepper;
    @FXML private ProgressBar traka;
    @FXML private Label lblPostojeci;

    // osnovno
    @FXML private TextField txtIme;
    @FXML private TextField txtPrezime;
    @FXML private TextField txtRoditelj;
    @FXML private TextField txtJMBG;
    @FXML private Label lblSpol;
    @FXML private TextField txtIndeks;
    @FXML private ComboBox<String> cmbFakultet;
    @FXML private ComboBox<String> cmbGodina;
    @FXML private TextField txtProsjek;
    @FXML private TextField txtEmail;
    @FXML private TextField txtTelefon;
    @FXML private TextField txtAdresa;
    @FXML private ComboBox<SocijalniStatusDTO> cmbSocijalniStatus;

    // lični podaci za prijavu
    @FXML private TextField txtLicnaKarta;
    @FXML private TextField txtTelefonRoditelja;
    @FXML private ComboBox<String> cmbKanton;
    @FXML private ComboBox<String> cmbOpcina;

    // studij
    @FXML private TextField txtAkGod;
    @FXML private ComboBox<String> cmbUpisPut;
    @FXML private VBox boxIspiti;
    @FXML private TextField txtIspiti;

    private NacrtPrijave nacrt = new NacrtPrijave();

    @FXML
    public void initialize() {
        KoraciUnosa.popuni(stepper, traka, 1);

        cmbFakultet.setItems(FXCollections.observableArrayList(Fakulteti.SVI));
        cmbGodina.setItems(FXCollections.observableArrayList(GODINE));
        cmbUpisPut.setItems(FXCollections.observableArrayList(UPIS));
        cmbUpisPut.getSelectionModel().selectFirst();
        cmbSocijalniStatus.setCellFactory(c -> statusCell());
        cmbSocijalniStatus.setButtonCell(statusCell());

        // kanton -> općine tog kantona
        cmbKanton.setItems(FXCollections.observableArrayList(Opcine.kantoni()));
        cmbOpcina.setDisable(true);
        cmbKanton.valueProperty().addListener((obs, o, n) -> {
            cmbOpcina.setItems(FXCollections.observableArrayList(Opcine.opcine(n)));
            cmbOpcina.setValue(null);
            cmbOpcina.setDisable(n == null);
        });

        txtAkGod.setText(String.valueOf(LocalDate.now().getYear()));

        txtTelefon.setTextFormatter(new TextFormatter<>(ch ->
                ch.getText().matches("[0-9+()\\-\\s]*") ? ch : null));
        txtTelefonRoditelja.setTextFormatter(new TextFormatter<>(ch ->
                ch.getText().matches("[0-9+()\\-\\s]*") ? ch : null));

        // broj položenih ispita ne pita se za 1. godinu
        cmbGodina.valueProperty().addListener((obs, o, n) -> prikaziIspite(n));
        prikaziIspite(null);

        // čim je JMBG kompletan: spol + provjera postoji li student
        txtJMBG.textProperty().addListener((obs, o, n) -> {
            prikaziSpol(n);
            if (n != null && n.matches("\\d{13}")) potraziPoJmbg(n);
            else sakrijPostojeceg();
        });
        prikaziSpol(null);

        // setNacrt (ako se vraćamo s koraka 2) poziva se prije ovoga
        Platform.runLater(() -> {
            if (nacrt.sifarnici == null) ucitajSifarnike();
            else popuniStatuse();
        });

        for (TextField t : List.of(txtIme, txtPrezime, txtRoditelj)) {
            t.focusedProperty().addListener((obs, bio, jeste) -> {
                if (!jeste && t.getText() != null) t.setText(Imena.formatiraj(t.getText()));
            });
        }
        ucitajKonkurs();
    }

    /** Povratak s koraka 2: popuni polja iz nacrta. */
    public void setNacrt(NacrtPrijave n) {
        this.nacrt = n;
        if (n.student != null) popuniPolja(n.student);
        if (n.prosjek != null) txtProsjek.setText(n.prosjek.toPlainString());
        if (n.akademskaGodina != null) txtAkGod.setText(String.valueOf(n.akademskaGodina));
        if (n.polozeniIspiti != null) txtIspiti.setText(String.valueOf(n.polozeniIspiti));
        if (n.upisGodinePut != null) {
            cmbUpisPut.getSelectionModel().select(Math.min(Math.max(n.upisGodinePut, 1), UPIS.size()) - 1);
        }
    }

    // ---------- server ----------

    private void ucitajSifarnike() {
        Task<SifarniciDTO> t = new Task<>() {
            @Override
            protected SifarniciDTO call() {
                return RucniUnosApi.sifarnici();
            }
        };
        t.setOnSucceeded(e -> {
            nacrt.sifarnici = t.getValue();
            popuniStatuse();
        });
        t.setOnFailed(e -> greska(t.getException(), "Ne mogu učitati šifarnike."));
        pokreni(t);
    }

    private void potraziPoJmbg(String jmbg) {
        Task<Optional<StudentPronadjenDTO>> t = new Task<>() {
            @Override
            protected Optional<StudentPronadjenDTO> call() {
                return RucniUnosApi.nadjiPoJmbg(jmbg);
            }
        };
        t.setOnSucceeded(e -> t.getValue().ifPresentOrElse(s -> {
            nacrt.idPostojecegStudenta = s.idStudent();
            nacrt.imaNalog = s.imaNalog();
            popuniPolja(s.podaci());
            lblPostojeci.setText("Student s ovim JMBG-om već postoji u evidenciji"
                    + (s.imaNalog() ? " i ima nalog na webu." : ".")
                    + " Podaci su učitani — provjerite ih i po potrebi ispravite.");
            lblPostojeci.setVisible(true);
            lblPostojeci.setManaged(true);
        }, this::sakrijPostojeceg));
        t.setOnFailed(e -> greska(t.getException(), "Greška pri provjeri JMBG-a."));
        pokreni(t);
    }

    // ---------- navigacija ----------

    @FXML
    public void onNextClicked() {
        if (nacrt.sifarnici == null) {
            alert("Šifarnici se još učitavaju. Pokušajte za trenutak.");
            return;
        }
        if (prazno(txtIme) || prazno(txtPrezime) || prazno(txtIndeks) || prazno(txtJMBG)
                || prazno(txtAdresa) || prazno(txtRoditelj) || prazno(txtEmail) || prazno(txtProsjek)
                || prazno(txtAkGod) || cmbFakultet.getValue() == null || cmbGodina.getValue() == null) {
            alert("Sva obavezna polja moraju biti popunjena.");
            return;
        }

        if (!txtJMBG.getText().trim().matches("\\d{13}")) {
            alert("JMBG mora imati tačno 13 cifara.");
            return;
        }

        if (prazno(txtLicnaKarta) || prazno(txtTelefonRoditelja)) {
            alert("Broj lične karte i telefon roditelja su obavezni.");
            return;
        }
        if (!txtLicnaKarta.getText().trim().matches("[A-Za-z0-9]{1,20}")) {
            alert("Broj lične karte smije imati samo slova i brojeve (do 20 znakova).");
            return;
        }
        if (!Opcine.postoji(cmbKanton.getValue(), cmbOpcina.getValue())) {
            alert("Odaberite kanton i općinu prebivališta.");
            return;
        }

        BigDecimal prosjek;
        try {
            prosjek = new BigDecimal(txtProsjek.getText().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            alert("Prosjek mora biti broj, npr. 8.6");
            return;
        }

        Integer akGod = cijeli(txtAkGod);
        if (akGod == null) {
            alert("Akademska godina mora biti broj, npr. " + LocalDate.now().getYear() + ".");
            return;
        }
        if (akGod < LocalDate.now().getYear()) {
            alert("Akademska godina ne može biti manja od tekuće.");
            return;
        }

        int godina = godinaUBroj(cmbGodina.getValue());
        Integer ispiti = null;
        if (godina != 1) {
            ispiti = cijeli(txtIspiti);
            if (ispiti == null || ispiti < 0) {
                alert("Unesite broj položenih ispita (0 ako nema).");
                return;
            }
        }

        SocijalniStatusDTO status = cmbSocijalniStatus.getValue();

        nacrt.student = new StudentUnosDTO(
                txtIme.getText().trim(),
                txtPrezime.getText().trim(),
                txtJMBG.getText().trim(),
                txtIndeks.getText().trim(),
                txtEmail.getText().trim(),
                txtTelefon.getText().trim(),
                cmbFakultet.getValue(),
                godina,
                txtAdresa.getText().trim(),
                txtRoditelj.getText().trim(),
                status != null ? status.idStatus() : null,
                txtLicnaKarta.getText().trim().toUpperCase(),
                txtTelefonRoditelja.getText().trim(),
                cmbKanton.getValue(),
                cmbOpcina.getValue());
        nacrt.prosjek = prosjek;
        nacrt.akademskaGodina = akGod;
        nacrt.polozeniIspiti = ispiti;
        nacrt.upisGodinePut = cmbUpisPut.getSelectionModel().getSelectedIndex() + 1;

        Ekrani.<NovaPrijavaController>zamijeni(txtIme, "/views/nova-prijava.fxml",
                c -> c.setNacrt(nacrt));
    }

    @FXML
    private void onBackClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin-main-view.fxml"));
            Parent root = loader.load();
            AdminController admin = loader.getController();
            admin.loadViewPublic("/views/prijave.fxml");

            Stage stage = (Stage) txtIme.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
            alert("Ne mogu se vratiti nazad.");
        }
    }

    // ---------- pomoćne ----------

    private void prikaziIspite(String godina) {
        boolean vidi = godina != null && !"1".equals(godina);
        boxIspiti.setVisible(vidi);
        boxIspiti.setManaged(vidi);
    }

    private void prikaziSpol(String jmbg) {
        String spol = Jmbg.spol(jmbg != null ? jmbg.trim() : null);
        lblSpol.setText(spol == null ? "Spol se računa iz JMBG-a."
                : "Spol: " + ("M".equals(spol) ? "muški" : "ženski") + " (iz JMBG-a)");
    }

    private void popuniPolja(StudentUnosDTO s) {
        txtIme.setText(s.ime());
        txtPrezime.setText(s.prezime());
        txtJMBG.setText(s.jmbg());
        txtIndeks.setText(s.brojIndeksa());
        txtEmail.setText(s.email());
        txtTelefon.setText(s.telefon());
        cmbFakultet.setValue(s.fakultet());
        cmbGodina.setValue(godinaUTekst(s.godinaStudija()));
        txtAdresa.setText(s.adresa());
        txtRoditelj.setText(s.imeRoditelja());
        odaberiStatus(s.idSocijalnogStatusa());

        txtLicnaKarta.setText(s.brojLicneKarte());
        txtTelefonRoditelja.setText(s.telefonRoditelja());
        cmbKanton.setValue(s.kanton());      // ovo napuni listu općina
        cmbOpcina.setValue(s.opcina());      // pa tek onda općina
    }

    private void popuniStatuse() {
        cmbSocijalniStatus.setItems(FXCollections.observableArrayList(nacrt.sifarnici.socijalniStatusi()));
        if (nacrt.student != null) odaberiStatus(nacrt.student.idSocijalnogStatusa());
    }

    private void odaberiStatus(Integer id) {
        if (id == null) return;
        cmbSocijalniStatus.getItems().stream()
                .filter(st -> Objects.equals(st.idStatus(), id))
                .findFirst()
                .ifPresent(cmbSocijalniStatus::setValue);
    }

    private void sakrijPostojeceg() {
        nacrt.idPostojecegStudenta = null;
        nacrt.imaNalog = false;
        lblPostojeci.setVisible(false);
        lblPostojeci.setManaged(false);
    }

    private int godinaUBroj(String g) {
        return switch (g) {
            case "Apsolvent" -> 7;
            case "Postdiplomac" -> 8;
            default -> Integer.parseInt(g);
        };
    }

    private String godinaUTekst(Integer g) {
        if (g == null) return null;
        return switch (g) {
            case 7 -> "Apsolvent";
            case 8 -> "Postdiplomac";
            default -> String.valueOf(g);
        };
    }

    private ListCell<SocijalniStatusDTO> statusCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(SocijalniStatusDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.naziv());
            }
        };
    }

    private Integer cijeli(TextField t) {
        try {
            return Integer.parseInt(t.getText().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean prazno(TextField t) {
        return t.getText() == null || t.getText().isBlank();
    }

    private void pokreni(Task<?> t) {
        Thread th = new Thread(t);
        th.setDaemon(true);
        th.start();
    }

    private void greska(Throwable ex, String opsta) {
        if (!(ex instanceof ApiException)) ex.printStackTrace();
        alert(ex instanceof ApiException ? ex.getMessage() : opsta);
    }

    private void alert(String poruka) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, poruka);
        a.setHeaderText(null);
        a.showAndWait();
    }

    /** Godina konkursa iz Postavki + upozorenje ako je unos van roka. */
    private void ucitajKonkurs() {
        Task<PostavkeDTO> t = new Task<>() {
            @Override
            protected PostavkeDTO call() {
                return PostavkeApi.ucitaj();
            }
        };
        t.setOnSucceeded(e -> {
            PostavkeDTO p = t.getValue();
            if (nacrt.akademskaGodina == null && p.akademskaGodina() != null) {
                txtAkGod.setText(String.valueOf(p.akademskaGodina()));
            }
            LocalDate danas = LocalDate.now();
            boolean prije = p.prijaveOd() != null && danas.isBefore(p.prijaveOd());
            boolean poslije = p.prijaveDo() != null && danas.isAfter(p.prijaveDo());
            if (nacrt.student == null && (prije || poslije)) {
                java.time.format.DateTimeFormatter df = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy.");
                alert("Konkurs trenutno nije otvoren"
                        + (poslije ? " (rok za prijave istekao je " + p.prijaveDo().format(df) + ")"
                        : " (prijave počinju " + p.prijaveOd().format(df) + ")")
                        + ".\nPrijavu možete unijeti, ali provjerite da li je Studentski centar prima.");
            }
        });
        t.setOnFailed(e -> System.err.println("Postavke nisu učitane: " + t.getException().getMessage()));
        pokreni(t);
    }
}