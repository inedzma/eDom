package model;

public class Student {
    private int idStudent;
    private String Ime;
    private String Prezime;
    private String BrojIndeksa;
    private String Fakultet;
    private int GodinaStudija;
    private double Prosjek;
    private String Email;
    private String Telefon;
    private SocijalniStatus SocijalniStatus;
    private String ImeRoditelja;
    private String Adresa;
    private String JMBG;

    public Student() {
    }

    public Student(int idStudent, String ime, String prezime, String brojIndeksa, String fakultet, int godinaStudija, double prosjek, String email, String telefon, SocijalniStatus socijalniStatus, String imeRoditelja, String adresa, String JMBG) {
        this.idStudent = idStudent;
        Ime = ime;
        Prezime = prezime;
        BrojIndeksa = brojIndeksa;
        Fakultet = fakultet;
        GodinaStudija = godinaStudija;
        Prosjek = prosjek;
        Email = email;
        Telefon = telefon;
        SocijalniStatus = socijalniStatus;
        ImeRoditelja = imeRoditelja;
        Adresa = adresa;
        this.JMBG = JMBG;
    }

    public int getIdStudent() {
        return idStudent;
    }

    public void setIdStudent(int idStudent) {
        this.idStudent = idStudent;
    }

    public String getIme() {
        return Ime;
    }

    public void setIme(String ime) {
        Ime = ime;
    }

    public String getPrezime() {
        return Prezime;
    }

    public void setPrezime(String prezime) {
        Prezime = prezime;
    }

    public String getBrojIndeksa() {
        return BrojIndeksa;
    }

    public void setBrojIndeksa(String brojIndeksa) {
        BrojIndeksa = brojIndeksa;
    }

    public String getFakultet() {
        return Fakultet;
    }

    public void setFakultet(String fakultet) {
        Fakultet = fakultet;
    }

    public int getGodinaStudija() {
        return GodinaStudija;
    }

    public void setGodinaStudija(int godinaStudija) {
        GodinaStudija = godinaStudija;
    }

    public double getProsjek() {
        return Prosjek;
    }

    public void setProsjek(double prosjek) {
        Prosjek = prosjek;
    }

    public String getEmail() {
        return Email;
    }

    public void setEmail(String email) {
        Email = email;
    }

    public String getTelefon() {
        return Telefon;
    }

    public void setTelefon(String telefon) {
        Telefon = telefon;
    }

    public SocijalniStatus getSocijalniStatus() {
        return SocijalniStatus;
    }

    public void setSocijalniStatus(SocijalniStatus socijalniStatus) {
        SocijalniStatus = socijalniStatus;
    }

    public String getImeRoditelja() {
        return ImeRoditelja;
    }

    public void setImeRoditelja(String imeRoditelja) {
        ImeRoditelja = imeRoditelja;
    }

    public String getAdresa() {
        return Adresa;
    }

    public void setAdresa(String adresa) {
        Adresa = adresa;
    }

    public String getJMBG() {
        return JMBG;
    }

    public void setJMBG(String JMBG) {
        this.JMBG = JMBG;
    }

    @Override
    public String toString() {
        return "Student{" +
                "idStudent=" + idStudent +
                ", Ime='" + Ime + '\'' +
                ", Prezime='" + Prezime + '\'' +
                ", BrojIndeksa='" + BrojIndeksa + '\'' +
                ", Fakultet='" + Fakultet + '\'' +
                ", GodinaStudija='" + GodinaStudija + '\'' +
                ", Prosjek=" + Prosjek +
                ", Email='" + Email + '\'' +
                ", Telefon='" + Telefon + '\'' +
                ", SocijalniStatus=" + SocijalniStatus +
                '}';
    }
}
