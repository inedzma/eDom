package service;

public class BraniociRezultat {
    private double bodovi;
    private String naziv;

    public BraniociRezultat(double bodovi, String naziv) {
        this.bodovi = bodovi;
        this.naziv = naziv;
    }

    public double getBodovi() {
        return bodovi;
    }

    public String getNaziv() {
        return naziv;
    }
}