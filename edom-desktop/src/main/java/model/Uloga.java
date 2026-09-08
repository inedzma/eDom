package model;

public class Uloga {
    private int IdUloga;
    private String Naziv;

    public Uloga(int idUloga, String naziv) {
        IdUloga = idUloga;
        Naziv = naziv;
    }

    public Uloga() {
    }

    public int getIdUloga() {
        return IdUloga;
    }

    public void setIdUloga(int idUloga) {
        IdUloga = idUloga;
    }

    public String getNaziv() {
        return Naziv;
    }

    public void setNaziv(String naziv) {
        Naziv = naziv;
    }

    @Override
    public String toString() {
        return "Uloga{" +
                "IdUloga=" + IdUloga +
                ", Naziv='" + Naziv + '\'' +
                '}';
    }
}
