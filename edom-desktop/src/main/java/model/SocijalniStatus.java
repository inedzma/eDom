package model;

public class SocijalniStatus {
    private int IdStatus;
    private String Naziv;

    public SocijalniStatus() {
    }

    public SocijalniStatus(int idStatus, String naziv) {
        IdStatus = idStatus;
        Naziv = naziv;
    }

    public int getIdStatus() {
        return IdStatus;
    }

    public void setIdStatus(int idStatus) {
        IdStatus = idStatus;
    }

    public String getNaziv() {
        return Naziv;
    }

    public void setNaziv(String naziv) {
        Naziv = naziv;
    }

    @Override
    public String toString() {
        return "SocijalniStatus{" +
                "IdStatus=" + IdStatus +
                ", Naziv='" + Naziv + '\'' +
                '}';
    }
}
