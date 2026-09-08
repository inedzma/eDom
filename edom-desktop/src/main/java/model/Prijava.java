package model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class Prijava {
    private int IdPrijava;
    private LocalDate DatumPrijava;
    private StatusPrijave StatusPrijave;
    private double UkupniBodovi;
    private String Napomena;
    private int AkademskaGodina;
    private int IdStudent;
    private Map<String, Double> bodoviMap;

    private List<Dokument> Dokumenti;

    private String imeStudenta;
    private String prezimeStudenta;

    private String imeRoditelja;


    public Prijava() {
    }

    public Prijava(int idPrijava, LocalDate datumPrijava, StatusPrijave statusPrijave, double ukupniBodovi, String napomena, int akademskaGodina, int StudentId) {
        IdPrijava = idPrijava;
        DatumPrijava = datumPrijava;
        StatusPrijave = statusPrijave;
        UkupniBodovi = ukupniBodovi;
        Napomena = napomena;
        AkademskaGodina = akademskaGodina;
        IdStudent = StudentId;
    }

    public int getIdPrijava() {
        return IdPrijava;
    }

    public void setIdPrijava(int idPrijava) {
        IdPrijava = idPrijava;
    }

    public LocalDate getDatumPrijava() {
        return DatumPrijava;
    }

    public void setDatumPrijava(LocalDate datumPrijava) {
        DatumPrijava = datumPrijava;
    }

    public StatusPrijave getStatusPrijave() {
        return StatusPrijave;
    }

    public void setStatusPrijave(StatusPrijave statusPrijave) {
        StatusPrijave = statusPrijave;
    }

    public double getUkupniBodovi() {
        return UkupniBodovi;
    }

    public void setUkupniBodovi(double ukupniBodovi) {
        UkupniBodovi = ukupniBodovi;
    }

    public String getNapomena() {
        return Napomena;
    }

    public void setNapomena(String napomena) {
        Napomena = napomena;
    }

    public int getAkademskaGodina() {
        return AkademskaGodina;
    }

    public void setAkademskaGodina(int akademskaGodina) {
        AkademskaGodina = akademskaGodina;
    }

    public List<Dokument> getDokumenti() {
        return Dokumenti;
    }

    public void setDokumenti(List<Dokument> dokumenti) {
        Dokumenti = dokumenti;
    }

    public int getIdStudent() {
        return IdStudent;
    }

    public void setIdStudent(int idStudent) {
        IdStudent = idStudent;
    }

    public String getImeStudenta() {
        return imeStudenta;
    }

    public void setImeStudenta(String imeStudenta) {
        this.imeStudenta = imeStudenta;
    }

    public String getImeRoditelja() {
        return imeRoditelja;
    }

    public void setImeRoditelja(String imeRoditelja) {
        this.imeRoditelja = imeRoditelja;
    }

    public String getPrezimeStudenta() {
        return prezimeStudenta;
    }

    public void setPrezimeStudenta(String prezimeStudenta) {
        this.prezimeStudenta = prezimeStudenta;
    }

    public Map<String, Double> getBodoviMap() { return bodoviMap; }
    public void setBodoviMap(Map<String, Double> bodoviMap) { this.bodoviMap = bodoviMap; }

    @Override
    public String toString() {
        return "Prijava{" +
                "IdPrijava=" + IdPrijava +
                ", DatumPrijava=" + DatumPrijava +
                ", StatusPrijave=" + StatusPrijave +
                ", UkupniBodovi=" + UkupniBodovi +
                ", Napomena='" + Napomena + '\'' +
                ", AkademskaGodina=" + AkademskaGodina +
                '}';
    }

    public int IzracunajUkupneBodove(){
        int suma = 0;
        for(Dokument d : Dokumenti){
            suma += d.getBrojBodova();
        }
        return suma;
    }



}
