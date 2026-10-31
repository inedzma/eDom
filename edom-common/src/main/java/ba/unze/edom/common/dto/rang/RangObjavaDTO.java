package ba.unze.edom.common.dto.rang;

/** Podaci o zadnjoj objavi; idObjava == null znači da lista još nije objavljena. */
public record RangObjavaDTO(
        Integer idObjava,
        Integer akademskaGodina,
        String vrsta,          // PRELIMINARNA / KONACNA
        String objavljeno,     // "22.10.2026 17:45"
        String objavio,
        int brojStudenata
) {
    public static RangObjavaDTO nema() {
        return new RangObjavaDTO(null, null, null, null, null, 0);
    }
}