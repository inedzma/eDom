package unos;

import ba.unze.edom.common.dto.SifarniciDTO;
import ba.unze.edom.common.dto.StudentUnosDTO;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** Podaci koje admin unosi kroz tri ekrana; serveru se šalju tek na kraju. */
public class NacrtPrijave {

    public SifarniciDTO sifarnici;

    // korak 1
    public StudentUnosDTO student;
    public BigDecimal prosjek;
    public Integer idPostojecegStudenta;
    public boolean imaNalog;

    // korak 2
    public Integer akademskaGodina;
    public Integer brojClanova;
    public BigDecimal udaljenostKm;
    public String napomena;
    /** šifra kriterija -> parametar (null kad kriterij ne traži parametar) */
    public final Map<String, BigDecimal> kriteriji = new LinkedHashMap<>();
}