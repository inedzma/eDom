package ba.unze.edom.server.dto;

import java.util.List;

/**
 * Svi dokumenti za jednu prijavu.
 *
 * @param obavezni          za sve kandidate
 * @param dodatni           samo za kriterije koje je student označio
 * @param kriterijiBezDokaza označeni kriteriji za koje konkurs ne propisuje poseban dokument
 * @param ostali            priloženi dokumenti koji ne odgovaraju nijednom zahtjevu (stare prijave)
 */
public record SpisakDokumenata(
        List<ZahtjevDokumenta> obavezni,
        List<ZahtjevDokumenta> dodatni,
        List<String> kriterijiBezDokaza,
        List<PrilozeniDokument> ostali
) {
    public long ispunjenoObaveznih() {
        return obavezni.stream().filter(ZahtjevDokumenta::ispunjen).count();
    }

    public int ukupnoObaveznih() {
        return obavezni.size();
    }

    public boolean sviObavezni() {
        return ispunjenoObaveznih() == ukupnoObaveznih();
    }

    public int procenat() {
        return obavezni.isEmpty() ? 100 : (int) (ispunjenoObaveznih() * 100 / obavezni.size());
    }
}