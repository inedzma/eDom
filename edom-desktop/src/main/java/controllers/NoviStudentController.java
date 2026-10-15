package controllers;

import api.ApiException;
import api.RucniUnosApi;
import ba.unze.edom.common.dto.SocijalniStatusDTO;
import ba.unze.edom.common.dto.StudentPronadjenDTO;
import ba.unze.edom.common.dto.StudentUnosDTO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import unos.NacrtPrijave;
import util.Ekrani;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    @FXML private TextField txtIme;
    @FXML private TextField txtPrezime;
    @FXML private TextField txtIndeks;
    @FXML private ComboBox<String> cmbFakultet;
    @FXML private ComboBox<String> cmbGodina;
    @FXML private TextField txtProsjek;
    @FXML private TextField txtEmail;
    @FXML private TextField txtTelefon;
    @FXML private ComboBox<SocijalniStatusDTO> cmbSocijalniStatus;
    @FXML private TextField txtJMBG;
    @FXML private TextField txtAdresa;
    @FXML private TextField txtRoditelj;
    @FXML private Label lblPostojeci;

    private NacrtPrijave nacrt = new NacrtPrijave();

    @FXML
    public void initialize() {
        cmbFakultet.setItems(FXCollections.observableArrayList(FAKULTETI));
        cmbGodina.setItems(FXCollections.observableArrayList(GODINE));
        cmbSocijalniStatus.setCellFactory(c -> statusCell());
        cmbSocijalniStatus.setButtonCell(statusCell());

        txtTelefon.setTextFormatter(new TextFormatter<>(ch ->
                ch.getText().matches("[0-9+()\\-\\s]*") ? ch : null));

        // čim je JMBG kompletan, provjeri postoji li student
        txtJMBG.textProperty().addListener((obs, o, n) -> {
            if (n != null && n.matches("\\d{13}")) potraziPoJmbg(n);
            else sakrijPostojeceg();
        });

        // setNacrt (ako se vraćamo s koraka 2) poziva se prije ovoga
        Platform.runLater(() -> {
            if (nacrt.sifarnici == null) ucitajSifarnike();
            else popuniStatuse();
        });
    }

    /** Povratak s koraka 2 — popuni polja iz nacrta. */
    public void setNacrt(NacrtPrijave n) {
        this.nacrt = n;
        if (n.student != null) popuniPolja(n.student);
        if (n.prosjek != null) txtProsjek.setText(n.prosjek.toPlainString());
    }

    // ---------- server ----------

    private void ucitajSifarnike() {
        Task<ba.unze.edom.common.dto.SifarniciDTO> t = new Task<>() {
            @Override
            protected ba.unze.edom.common.dto.SifarniciDTO call() {
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
                || cmbFakultet.getValue() == null || cmbGodina.getValue() == null) {
            alert("Sva obavezna polja moraju biti popunjena.");
            return;
        }

        BigDecimal prosjek;
        try {
            prosjek = new BigDecimal(txtProsjek.getText().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            alert("Prosjek mora biti broj, npr. 8.6");
            return;
        }

        if (!txtJMBG.getText().trim().matches("\\d{13}")) {
            alert("JMBG mora imati tačno 13 cifara.");
            return;
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
                godinaUBroj(cmbGodina.getValue()),
                txtAdresa.getText().trim(),
                txtRoditelj.getText().trim(),
                status != null ? status.idStatus() : null);
        nacrt.prosjek = prosjek;

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
}