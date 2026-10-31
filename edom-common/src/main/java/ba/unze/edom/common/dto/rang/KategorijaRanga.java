package ba.unze.edom.common.dto.rang;

public enum KategorijaRanga {

    BRUCOS("Brucoši", "BRUCOŠI"),
    SA_USLOVOM("S uslovom", "STUDENTI VIŠIH GODINA"),
    LISTA_CEKANJA("Lista čekanja", "LISTA ČEKANJA – STUDENTI BEZ DOKAZA O ISPUNJAVANJU USLOVA ZA UPIS");

    private final String naziv;
    private final String naslovPdf;

    KategorijaRanga(String naziv, String naslovPdf) {
        this.naziv = naziv;
        this.naslovPdf = naslovPdf;
    }

    public String naziv() { return naziv; }
    public String naslovPdf() { return naslovPdf; }
}