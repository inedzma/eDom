package ba.unze.edom.server.mapper;

import ba.unze.edom.common.dto.DokumentDTO;
import ba.unze.edom.common.dto.PrijavaDetaljiDTO;
import ba.unze.edom.common.dto.PrijavaListaDTO;
import ba.unze.edom.common.dto.RezultatBodovanja;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.Student;

import java.util.List;

public final class PrijaveMapper {

    private PrijaveMapper() {}

    public static PrijavaListaDTO uListu(Prijava p, RezultatBodovanja r,
                                         String greska, int brojDokumenata) {
        Student s = p.getStudent();
        return new PrijavaListaDTO(
                p.getIdPrijava(), p.getAkademskaGodina(), p.getDatumPrijave(),
                status(p), p.getNapomena(),
                s.getIdStudent(), s.getIme(), s.getPrezime(), s.getBrojIndeksa(), s.getFakultet(),
                p.getGodinaStudija(), p.getProsjek(),
                r != null ? r.potvrdjeno() : null,
                r != null ? r.potencijalno() : null,
                r != null && r.cekaVerifikaciju(),
                brojDokumenata,
                greska
        );
    }

    public static PrijavaDetaljiDTO uDetalje(Prijava p, RezultatBodovanja r,
                                             String greska, List<DokumentDTO> dokumenti) {
        Student s = p.getStudent();
        return new PrijavaDetaljiDTO(
                p.getIdPrijava(), p.getAkademskaGodina(), p.getDatumPrijave(),
                status(p), p.getNapomena(),
                s.getIdStudent(), s.getIme(), s.getPrezime(), s.getBrojIndeksa(),
                s.getFakultet(), s.getEmail(), s.getTelefon(),
                p.getGodinaStudija(), p.getProsjek(), p.getPolozeniIspiti(),
                p.getBrojClanovaDomacinstva(), p.getUkupnaPrimanja(), p.getUdaljenostKm(),
                r, greska,
                dokumenti
        );
    }

    private static String status(Prijava p) {
        return p.getStatus() != null ? p.getStatus().getNaziv() : null;
    }
}