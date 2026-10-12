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

    private static String token;
    private static String username;

    public static void prijavi(String u, String t) {
        username = u;
        token = t;
    }

    public static String getToken()     { return token; }
    public static String getUsername()  { return username; }
    public static boolean jePrijavljen() { return token != null; }

    public static void odjavi() {
        token = null;
        username = null;
    }
}
