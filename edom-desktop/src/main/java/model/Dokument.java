package model;

import java.time.LocalDate;
import java.util.Date;

public class Dokument {
    private int IdDokument;
    private String Naziv;
    private LocalDate DatumUpload;
    private double BrojBodova;
    private String DokumentB64;
    private boolean IsDostavljen;
    private VrstaDokumenta VrstaDokumenta;

    public Dokument() {
    }

    public Dokument(int idDokument, String naziv, LocalDate datumUpload, double brojBodova, String dokumentB64, boolean isDostavljen, VrstaDokumenta vrstaDokumenta) {
        IdDokument = idDokument;
        Naziv = naziv;
        DatumUpload = datumUpload;
        BrojBodova = brojBodova;
        DokumentB64 = dokumentB64;
        IsDostavljen = isDostavljen;
        VrstaDokumenta = vrstaDokumenta;
    }

    public int getIdDokument() {
        return IdDokument;
    }

    public void setIdDokument(int idDokument) {
        IdDokument = idDokument;
    }

    public String getNaziv() {
        return Naziv;
    }

    public void setNaziv(String naziv) {
        Naziv = naziv;
    }

    public LocalDate getDatumUpload() {
        return DatumUpload;
    }

    public void setDatumUpload(LocalDate datumUpload) {
        DatumUpload = datumUpload;
    }

    public double getBrojBodova() {
        return BrojBodova;
    }

    public void setBrojBodova(double brojBodova) {
        BrojBodova = brojBodova;
    }

    public String getDokumentB64() {
        return DokumentB64;
    }

    public void setDokumentB64(String dokumentB64) {
        DokumentB64 = dokumentB64;
    }

    public boolean isDostavljen() {
        return IsDostavljen;
    }

    public void setDostavljen(boolean dostavljen) {
        IsDostavljen = dostavljen;
    }

    public VrstaDokumenta getVrstaDokumenta() {
        return VrstaDokumenta;
    }

    public void setVrstaDokumenta(VrstaDokumenta vrstaDokumenta) {
        VrstaDokumenta = vrstaDokumenta;
    }

    @Override
    public String toString() {
        return "Dokument{" +
                "IdDokument=" + IdDokument +
                ", Naziv='" + Naziv + '\'' +
                ", DatumUpload=" + DatumUpload +
                ", BrojBodova=" + BrojBodova +
                ", DokumentB64='" + DokumentB64 + '\'' +
                ", IsDostavljen=" + IsDostavljen +
                ", VrstaDokumenta=" + VrstaDokumenta +
                '}';
    }
}
