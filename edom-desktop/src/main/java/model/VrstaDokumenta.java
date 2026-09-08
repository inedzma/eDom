package model;

public class VrstaDokumenta {
    private int IdVrsta;
    private String Naziv;

    public VrstaDokumenta() {
    }

    public VrstaDokumenta(int idVrsta, String naziv) {
        IdVrsta = idVrsta;
        Naziv = naziv;
    }

    public String getNaziv() {
        return Naziv;
    }

    public void setNaziv(String naziv) {
        Naziv = naziv;
    }

    public int getIdVrsta() {
        return IdVrsta;
    }

    public void setIdVrsta(int idVrsta) {
        IdVrsta = idVrsta;
    }

    @Override
    public String toString() {
        return "VrstaDokumenta{" +
                "IdVrsta=" + IdVrsta +
                ", Naziv='" + Naziv + '\'' +
                '}';
    }
}
