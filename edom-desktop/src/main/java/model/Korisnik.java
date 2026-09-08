package model;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.Timestamp;

public class Korisnik {
    private int IdKorisnik;
    private String Ime;
    private String Prezime;
    private String Username;
    private String PasswordHash;
    private Uloga Uloga;
    private String Email;
    private Timestamp zadnjaPrijava;

    public Korisnik(int idKorisnik, String ime, String prezime, String username, String passwordHash, Uloga uloga, String email) {
        IdKorisnik = idKorisnik;
        Ime = ime;
        Prezime = prezime;
        Username = username;
        PasswordHash = passwordHash;
        Uloga = uloga;
        Email = email;
    }

    public Korisnik() {
    }

    public int getIdKorisnik() {
        return IdKorisnik;
    }

    public void setIdKorisnik(int idKorisnik) {
        IdKorisnik = idKorisnik;
    }

    public String getIme() {
        return Ime;
    }

    public void setIme(String ime) {
        Ime = ime;
    }

    public String getPrezime() {
        return Prezime;
    }

    public void setPrezime(String prezime) {
        Prezime = prezime;
    }

    public String getUsername() {
        return Username;
    }

    public void setUsername(String username) {
        Username = username;
    }

    public String getPasswordHash() {
        return PasswordHash;
    }

    public void setPasswordHash(String passwordHash) {
        PasswordHash = passwordHash;
    }

    public Uloga getUloga() {
        return Uloga;
    }

    public void setUloga(Uloga uloga) {
        Uloga = uloga;
    }

    public String getEmail() {
        return Email;
    }

    public void setEmail(String email) {
        Email = email;
    }

    @Override
    public String toString() {
        return "Korisnik{" +
                "IdKorisnik=" + IdKorisnik +
                ", Ime='" + Ime + '\'' +
                ", Prezime='" + Prezime + '\'' +
                ", Username='" + Username + '\'' +
                ", PasswordHash='" + PasswordHash + '\'' +
                ", Uloga=" + Uloga +
                '}';
    }

    public boolean ProvjeriPassword(String password) {
        return BCrypt.checkpw(password, this.PasswordHash);
    }
    public Timestamp getZadnjaPrijava() {
        return zadnjaPrijava;
    }

    public void setZadnjaPrijava(Timestamp zadnjaPrijava) {
        this.zadnjaPrijava = zadnjaPrijava;
    }
}


