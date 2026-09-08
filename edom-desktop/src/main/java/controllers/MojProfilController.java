package controllers;

import dao.KorisnikDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Korisnik;
import service.Session;
import util.TextUtil;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.Timestamp;
import org.mindrot.jbcrypt.BCrypt;

public class MojProfilController {

    @FXML private TextField txtIme;
    @FXML private TextField txtPrezime;
    @FXML private TextField txtUsername;
    @FXML private TextField txtUloga;

    @FXML private PasswordField txtStaraLozinka;
    @FXML private PasswordField txtNovaLozinka;
    @FXML private PasswordField txtPotvrdaLozinke;

    @FXML private Label lblPoruka;
    @FXML private Label lblZadnjaPrijava;

    private final KorisnikDAO korisnikDAO = new KorisnikDAO();
    private Korisnik korisnik;

    @FXML
    public void initialize() {
        korisnik = Session.getKorisnik();

        if (korisnik == null) {
            lblPoruka.setText("Greška: korisnik nije prijavljen.");
            return;
        }

        txtIme.setText(korisnik.getIme());
        txtPrezime.setText(korisnik.getPrezime());
        txtUsername.setText(korisnik.getUsername());
        txtUloga.setText(korisnik.getUloga().getNaziv());

        txtUsername.setDisable(true);
        txtUloga.setDisable(true);
        lblZadnjaPrijava.setText(
                formatZadnjaPrijava(korisnik.getZadnjaPrijava())
        );
    }

    @FXML
    private void sacuvajPromjene() {
        // 1. Ažuriranje osnovnih podataka
        korisnik.setIme(TextUtil.formatirajIme(txtIme.getText().trim()));
        korisnik.setPrezime(TextUtil.formatirajIme(txtPrezime.getText().trim()));

        // Provjeri da li korisnik uopće želi mijenjati lozinku
        boolean zeliMijenjatiLozinku = !txtStaraLozinka.getText().isEmpty()
                || !txtNovaLozinka.getText().isEmpty()
                || !txtPotvrdaLozinke.getText().isEmpty();

        // Ako su sva polja za lozinku prazna, samo spremi ime/prezime i izađi
        if (!zeliMijenjatiLozinku) {
            korisnikDAO.azurirajKorisnika(korisnik); // Metoda koja update-uje samo ime/prezime
            lblPoruka.setText("Podaci profila uspješno ažurirani.");
            lblPoruka.setStyle("-fx-text-fill: green;"); // Da budemo sigurni da se vidi
            return;
        }

        // --- OVDJE POČINJE LOGIKA ZA LOZINKU ---

        // 2. Provjera: Jesu li sva polja popunjena?
        if (txtStaraLozinka.getText().isEmpty() || txtNovaLozinka.getText().isEmpty() || txtPotvrdaLozinke.getText().isEmpty()) {
            lblPoruka.setText("Za promjenu lozinke morate ispuniti sva tri polja.");
            lblPoruka.setStyle("-fx-text-fill: red;");
            return;
        }

        // 3. Provjera STARE lozinke (ključni ispravak!)
        // Ne zovi bazu, usporedi unos sa trenutnim hashom u objektu
        if (!BCrypt.checkpw(txtStaraLozinka.getText(), korisnik.getPasswordHash())) {
            lblPoruka.setText("Pogrešna trenutna lozinka.");
            lblPoruka.setStyle("-fx-text-fill: red;");
            return;
        }

        // 4. Provjera podudaranja nove i potvrde
        if (!txtNovaLozinka.getText().equals(txtPotvrdaLozinke.getText())) {
            lblPoruka.setText("Nova lozinka i potvrda se ne podudaraju.");
            lblPoruka.setStyle("-fx-text-fill: red;");
            return;
        }

        // 5. Provjera da nova nije ista kao stara
        if (txtStaraLozinka.getText().equals(txtNovaLozinka.getText())) {
            lblPoruka.setText("Nova lozinka mora biti različita od stare.");
            lblPoruka.setStyle("-fx-text-fill: red;");
            return;
        }

        // 6. Validacija kompleksnosti (tvoj regex)
        if (!txtNovaLozinka.getText().matches("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{4,}$")) {
            lblPoruka.setText("Lozinka preslaba (Min 4 znaka, 1 velika, 1 broj, 1 znak).");
            lblPoruka.setStyle("-fx-text-fill: red;");
            return;
        }

        // 7. Hashiranje i spremanje (ključni ispravak!)
        String hashedNewPass = BCrypt.hashpw(txtNovaLozinka.getText(), BCrypt.gensalt());
        korisnik.setPasswordHash(hashedNewPass);

        // Spremi sve u bazu
        korisnikDAO.azurirajProfil(korisnik); // Očekujemo da ova metoda radi UPDATE passworda

        // Očisti polja
        txtStaraLozinka.clear();
        txtNovaLozinka.clear();
        txtPotvrdaLozinke.clear();

        lblPoruka.setText("Profil i lozinka uspješno promijenjeni.");
        lblPoruka.setStyle("-fx-text-fill: green;");
    }

    private String formatZadnjaPrijava(Timestamp ts) {
        if (ts == null) {
            return "Prva prijava";
        }

        LocalDateTime last = ts.toLocalDateTime();
        LocalDateTime now = LocalDateTime.now();

        long minutes = java.time.Duration.between(last, now).toMinutes();
        long hours   = java.time.Duration.between(last, now).toHours();
        long days    = java.time.Duration.between(last, now).toDays();

        if (minutes < 1) {
            return "Upravo sada";
        } else if (minutes < 60) {
            return "Prije " + minutes + " min";
        } else if (hours < 24) {
            return "Prije " + hours + " h";
        } else if (days == 1) {
            return "Jučer u " + last.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else if (days < 7) {
            return "Prije " + days + " dana";
        } else {
            return last.format(DateTimeFormatter.ofPattern("dd.MM.yyyy 'u' HH:mm"));
        }
    }
}
