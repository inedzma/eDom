package controllers;

import api.ApiException;
import api.ProfilApi;
import ba.unze.edom.common.dto.KorisnikProfilDTO;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class MojProfilController {

    @FXML private TextField txtIme;
    @FXML private TextField txtPrezime;
    @FXML private TextField txtUsername;
    @FXML private TextField txtUloga;
    @FXML private TextField txtEmail;
    @FXML private Label lblBadgeUloga;
    @FXML private Label lblZadnjaPrijava;

    @FXML private PasswordField txtStaraLozinka;
    @FXML private PasswordField txtNovaLozinka;
    @FXML private PasswordField txtPotvrdaLozinke;

    @FXML private Label lblPoruka;

    private KorisnikProfilDTO profil;

    @FXML
    public void initialize() {
        txtUsername.setDisable(true);
        txtUloga.setDisable(true);

        Task<KorisnikProfilDTO> task = new Task<>() {
            @Override
            protected KorisnikProfilDTO call() {
                return ProfilApi.dohvati();
            }
        };
        task.setOnSucceeded(e -> prikazi(task.getValue()));
        task.setOnFailed(e -> greska(task.getException(), "Greška pri učitavanju profila."));
        pokreni(task);
    }

    private void prikazi(KorisnikProfilDTO p) {
        this.profil = p;
        txtIme.setText(p.ime());
        txtPrezime.setText(p.prezime());
        txtUsername.setText(p.username());
        txtEmail.setText(p.email() != null ? p.email() : "nije upisan");
        txtUloga.setText(p.uloga());
        lblBadgeUloga.setText(p.uloga() != null ? p.uloga().toUpperCase() : "");
        lblZadnjaPrijava.setText(formatZadnjaPrijava(p.zadnjaPrijava()));
    }

    @FXML
    private void sacuvajPromjene() {
        if (profil == null) return;

        String ime = txtIme.getText().trim();
        String prezime = txtPrezime.getText().trim();
        String stara = txtStaraLozinka.getText();
        String nova = txtNovaLozinka.getText();
        String potvrda = txtPotvrdaLozinke.getText();

        boolean mijenjaImena = !ime.equals(profil.ime()) || !prezime.equals(profil.prezime());
        boolean mijenjaLozinku = !stara.isEmpty() || !nova.isEmpty() || !potvrda.isEmpty();

        if (!mijenjaImena && !mijenjaLozinku) {
            poruka("Nema promjena za sačuvati.", true);
            return;
        }

        // provjere koje pripadaju ekranu; sve ostalo provjerava server
        if (mijenjaLozinku) {
            if (stara.isEmpty() || nova.isEmpty() || potvrda.isEmpty()) {
                poruka("Za promjenu lozinke morate ispuniti sva tri polja.", false);
                return;
            }
            if (!nova.equals(potvrda)) {
                poruka("Nova lozinka i potvrda se ne podudaraju.", false);
                return;
            }
        }

        Task<KorisnikProfilDTO> task = new Task<>() {
            @Override
            protected KorisnikProfilDTO call() {
                KorisnikProfilDTO rezultat = profil;
                if (mijenjaImena) {
                    rezultat = ProfilApi.izmijeni(ime, prezime);
                }
                if (mijenjaLozinku) {
                    ProfilApi.promijeniLozinku(stara, nova);
                }
                return rezultat;
            }
        };

        task.setOnSucceeded(e -> {
            prikazi(task.getValue());
            txtStaraLozinka.clear();
            txtNovaLozinka.clear();
            txtPotvrdaLozinke.clear();
            poruka(mijenjaLozinku ? "Profil i lozinka uspješno promijenjeni."
                    : "Podaci profila uspješno ažurirani.", true);
        });
        task.setOnFailed(e -> greska(task.getException(), "Greška pri spremanju promjena."));
        pokreni(task);
    }

    // ---------- pomoćne ----------

    private void poruka(String tekst, boolean uspjeh) {
        lblPoruka.setText(tekst);
        lblPoruka.setStyle(uspjeh ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        lblPoruka.setVisible(true);
        lblPoruka.setManaged(true);
    }

    private void greska(Throwable ex, String opsta) {
        if (!(ex instanceof ApiException)) ex.printStackTrace();
        poruka(ex instanceof ApiException ? ex.getMessage() : opsta, false);
    }

    private void pokreni(Task<?> task) {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private String formatZadnjaPrijava(Instant trenutak) {
        if (trenutak == null) return "Prva prijava";

        LocalDateTime last = LocalDateTime.ofInstant(trenutak, ZoneId.systemDefault());
        LocalDateTime now = LocalDateTime.now();

        long minutes = Duration.between(last, now).toMinutes();
        long hours   = Duration.between(last, now).toHours();
        long days    = Duration.between(last, now).toDays();

        if (minutes < 1)  return "Upravo sada";
        if (minutes < 60) return "Prije " + minutes + " min";
        if (hours < 24)   return "Prije " + hours + " h";
        if (days == 1)    return "Jučer u " + last.format(DateTimeFormatter.ofPattern("HH:mm"));
        if (days < 7)     return "Prije " + days + " dana";
        return last.format(DateTimeFormatter.ofPattern("dd.MM.yyyy 'u' HH:mm"));
    }
}