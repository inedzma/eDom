package dto;

public class PrijavaExportDTo {

    private final int idPrijave;
    private final String ime;
    private final String prezime;
    private final String fakultet;
    private final String datumPrijave;
    private final double ukupniBodovi;
    private final String status;

    public PrijavaExportDTo(
            int idPrijave,
            String ime,
            String prezime,
            String fakultet,
            String datumPrijave,
            double ukupniBodovi,
            String status
    ) {
        this.idPrijave = idPrijave;
        this.ime = ime;
        this.prezime = prezime;
        this.fakultet = fakultet;
        this.datumPrijave = datumPrijave;
        this.ukupniBodovi = ukupniBodovi;
        this.status = status;
    }

    public int getIdPrijave() { return idPrijave; }
    public String getIme() { return ime; }
    public String getPrezime() { return prezime; }
    public String getFakultet() { return fakultet; }
    public String getDatumPrijave() { return datumPrijave; }
    public double getUkupniBodovi() { return ukupniBodovi; }
    public String getStatus() { return status; }
}
