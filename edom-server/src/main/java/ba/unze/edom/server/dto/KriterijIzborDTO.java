package ba.unze.edom.server.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public class KriterijIzborDTO {

    private Integer idVrsteKriterija;
    private boolean izabran;
    private BigDecimal parametar;

    // za prikaz
    private String naziv;
    private String grupa;
    private boolean traziParametar;
    private String nazivParametra;

    // Lombok ne radi dobro sa th:field na listama - getteri i setteri rucno
    public Integer getIdVrsteKriterija() { return idVrsteKriterija; }
    public void setIdVrsteKriterija(Integer v) { idVrsteKriterija = v; }

    public boolean isIzabran() { return izabran; }
    public void setIzabran(boolean v) { izabran = v; }

    public BigDecimal getParametar() { return parametar; }
    public void setParametar(BigDecimal v) { parametar = v; }

    public String getNaziv() { return naziv; }
    public void setNaziv(String v) { naziv = v; }

    public String getGrupa() { return grupa; }
    public void setGrupa(String v) { grupa = v; }

    public boolean isTraziParametar() { return traziParametar; }
    public void setTraziParametar(boolean v) { traziParametar = v; }

    public String getNazivParametra() { return nazivParametra; }
    public void setNazivParametra(String v) { nazivParametra = v; }

    private boolean traziPeriod;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate datumOd;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate datumDo;

    public boolean isTraziPeriod() { return traziPeriod; }
    public void setTraziPeriod(boolean v) { traziPeriod = v; }

    public LocalDate getDatumOd() { return datumOd; }
    public void setDatumOd(LocalDate v) { datumOd = v; }

    public LocalDate getDatumDo() { return datumDo; }
    public void setDatumDo(LocalDate v) { datumDo = v; }
}