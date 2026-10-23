package service.export;

import ba.unze.edom.common.dto.KategorijaRanga;
import ba.unze.edom.common.dto.RangRedDTO;

public enum RangKolona {

    REDNI_BROJ("R.br", true),
    SIFRA("Šifra", true),
    IME("Prezime (ime oca) ime", false),
    GODINA("Godina", true),
    KATEGORIJA("Kategorija", false),
    USPJEH("Uspjeh", true),
    BRANIOCI("Branioci", true),
    SOCIJALNI("Socijalni status", true),
    UDALJENOST("Udaljenost", true),
    UKUPNO("Ukupno", true),
    STATUS("Status", false);

    private final String naslov;
    private final boolean centrirano;

    RangKolona(String naslov, boolean centrirano) {
        this.naslov = naslov;
        this.centrirano = centrirano;
    }

    public String naslov() { return naslov; }
    public boolean centrirano() { return centrirano; }

    /** @param rb redni broj u tabeli koja se prikazuje (ekran ili PDF) */
    public String vrijednost(RangRedDTO r, int rb) {
        return switch (this) {
            case REDNI_BROJ -> String.valueOf(rb);
            case SIFRA -> r.sifra() == null ? "" : r.sifra();
            case IME -> punoIme(r);
            case GODINA -> r.godinaStudija() == null ? "" : String.valueOf(r.godinaStudija());
            case KATEGORIJA -> r.kategorija() == null ? "" : r.kategorija().naziv();
            case USPJEH -> broj(r.uspjeh());
            case BRANIOCI -> broj(r.branioci());
            case SOCIJALNI -> broj(r.socijalni());
            case UDALJENOST -> broj(r.udaljenost());
            case UKUPNO -> r.greska() != null ? "⚠" : broj(r.ukupno());
            case STATUS -> status(r);
        };
    }

    /** Zvanični format: Prezime (ime oca) Ime. */
    public static String punoIme(RangRedDTO r) {
        String prezime = r.prezime() != null ? r.prezime() : "";
        String ime = r.ime() != null ? r.ime() : "";
        String otac = r.imeOca() != null && !r.imeOca().isBlank() ? " (" + r.imeOca() + ") " : " ";
        return prezime + otac + ime;
    }

    public static String status(RangRedDTO r) {
        if (r.greska() != null) return "Neispravni podaci";
        if (r.kategorija() == KategorijaRanga.LISTA_CEKANJA) return "Čeka dokaz o uslovu";
        String dokaz = r.dokazNaknadno() ? " · dokaz " + r.dokazUslova() : "";
        return (r.primljen() ? "Primljen" : "Ispod crte") + dokaz;
    }

    private static String broj(double v) {
        return String.format("%.2f", v);
    }
    /** Brojčana vrijednost za Excel; null = kolona je tekst. */
    public Number broj(RangRedDTO r, int rb) {
        return switch (this) {
            case REDNI_BROJ -> rb;
            case GODINA -> r.godinaStudija();
            case USPJEH -> r.uspjeh();
            case BRANIOCI -> r.branioci();
            case SOCIJALNI -> r.socijalni();
            case UDALJENOST -> r.udaljenost();
            case UKUPNO -> r.greska() != null ? null : r.ukupno();
            default -> null;
        };
    }
}