package service;

import model.Korisnik;

public class Session {

    private static Korisnik prijavljeniKorisnik;

    public static void setKorisnik(Korisnik korisnik) {
        prijavljeniKorisnik = korisnik;
    }

    public static Korisnik getKorisnik() {
        return prijavljeniKorisnik;
    }

    public static void logout() {
        prijavljeniKorisnik = null;
    }
}
