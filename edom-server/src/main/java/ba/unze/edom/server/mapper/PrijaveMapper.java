package ba.unze.edom.server.mapper;

import ba.unze.edom.common.dto.dokumenti.DokumentDTO;
import ba.unze.edom.common.dto.prijava.PrijavaDetaljiDTO;
import ba.unze.edom.common.dto.prijava.PrijavaListaDTO;
import ba.unze.edom.common.dto.bodovanje.RezultatBodovanja;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.Student;
import ba.unze.edom.common.dto.prijava.KriterijStanjeDTO;

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

    public static PrijavaDetaljiDTO uDetalje(Prijava p, RezultatBodovanja r, String greska,
                                             List<DokumentDTO> dokumenti, List<String> dozvoljeniStatusi) {
        Student s = p.getStudent();

        List<KriterijStanjeDTO> kriteriji = p.getKriteriji().stream()
                .map(k -> new KriterijStanjeDTO(
                        k.getIdPrijavaKriterij(),
                        k.getVrsta().getNaziv(),
                        k.getParametar(),
                        k.getVrsta().getNazivParametra(),
                        k.isVerifikovan()))
                .toList();

        return new PrijavaDetaljiDTO(
                p.getIdPrijava(), p.getAkademskaGodina(), p.getDatumPrijave(),
                status(p), p.getNapomena(),
                s.getIdStudent(), s.getIme(), s.getPrezime(), s.getBrojIndeksa(),
                s.getFakultet(), s.getEmail(), s.getTelefon(),
                p.getGodinaStudija(), p.getProsjek(), p.getPolozeniIspiti(),
                p.getBrojClanovaDomacinstva(), p.getUkupnaPrimanja(), p.getUdaljenostKm(),
                r, greska,
                dokumenti,
                p.isUspjehVerifikovan(), p.isUdaljenostVerifikovana(), p.isPrimanjaVerifikovana(),
                kriteriji,
                p.getNapomenaAdmina(),
                "na pregledu".equals(status(p)),
                dozvoljeniStatusi
        );
    }

    private static String status(Prijava p) {
        return p.getStatus() != null ? p.getStatus().getNaziv() : null;
    }
}