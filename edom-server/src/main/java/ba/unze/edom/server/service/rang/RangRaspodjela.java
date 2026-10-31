package ba.unze.edom.server.service.rang; // ⚠ tvoj paket

import ba.unze.edom.common.dto.rang.KategorijaRanga;
import ba.unze.edom.common.dto.rang.RangListaAdminDTO;
import ba.unze.edom.common.dto.rang.RangRedDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Dijeli prijave na 3 liste, rangira ih i povlači crtu po kvotama.
 * Ne čita bazu — dobija gotove redove, pa se lako testira.
 */
@Component
public class RangRaspodjela {

    /** Neispravni na dno; pa bodovi; privremeno pravilo za jednake bodove: ranija prijava (manji ID). */
    static final Comparator<RangRedDTO> PO_BODOVIMA =
            Comparator.comparing((RangRedDTO r) -> r.greska() != null)
                    .thenComparing(RangRedDTO::ukupno, Comparator.reverseOrder())
                    .thenComparing(RangRedDTO::idPrijava);

    /** S uslovom: naknadni dokaz ide iza onih koji su dokaz priložili uz prijavu — ne istiskuje ih. */
    static final Comparator<RangRedDTO> SA_USLOVOM_REDOSLIJED =
            Comparator.comparing((RangRedDTO r) -> r.greska() != null)
                    .thenComparing(RangRedDTO::dokazNaknadno)
                    .thenComparing(PO_BODOVIMA);

    public RangListaAdminDTO rasporedi(String akademskaGodina, Integer godina, List<RangRedDTO> redovi,
                                       int kapacitet, double udioBrucosa, boolean popuniBrucosima) {

        List<RangRedDTO> brucosi = filtriraj(redovi, KategorijaRanga.BRUCOS, PO_BODOVIMA);
        List<RangRedDTO> saUslovom = filtriraj(redovi, KategorijaRanga.SA_USLOVOM, SA_USLOVOM_REDOSLIJED);
        List<RangRedDTO> cekanje = filtriraj(redovi, KategorijaRanga.LISTA_CEKANJA, PO_BODOVIMA);

        int kvotaB = (int) Math.round(kapacitet * udioBrucosa);
        int kvotaV = kapacitet - kvotaB;
        int ispravnihB = brojIspravnih(brucosi);
        int ispravnihV = brojIspravnih(saUslovom);

        int primB = Math.min(ispravnihB, kvotaB);
        // neiskorištena brucoška mjesta dobijaju viši s uslovom
        int primV = Math.min(ispravnihV, kvotaV + (kvotaB - primB));
        int slobodno = kapacitet - primB - primV;

        // slobodna mjesta popunjavaju se brucošima tek kad admin to uključi
        if (popuniBrucosima) {
            int dodatno = Math.min(ispravnihB - primB, slobodno);
            primB += dodatno;
            slobodno -= dodatno;
        }

        return new RangListaAdminDTO(akademskaGodina, godina, kapacitet, kvotaB, kvotaV,
                primB, primV, slobodno, popuniBrucosima,
                numerisi(brucosi, primB), numerisi(saUslovom, primV), numerisi(cekanje, 0));
    }

    private List<RangRedDTO> filtriraj(List<RangRedDTO> redovi, KategorijaRanga k, Comparator<RangRedDTO> redoslijed) {
        return redovi.stream().filter(r -> r.kategorija() == k).sorted(redoslijed).toList();
    }

    private int brojIspravnih(List<RangRedDTO> lista) {
        return (int) lista.stream().filter(r -> r.greska() == null).count();
    }

    /** Neispravni su sortirani na dno, pa prvih `primljenih` redova uvijek su ispravni. */
    private List<RangRedDTO> numerisi(List<RangRedDTO> lista, int primljenih) {
        List<RangRedDTO> out = new ArrayList<>(lista.size());
        for (int i = 0; i < lista.size(); i++) {
            RangRedDTO r = lista.get(i);
            out.add(r.saRangom(i + 1, i < primljenih && r.greska() == null));
        }
        return out;
    }
}