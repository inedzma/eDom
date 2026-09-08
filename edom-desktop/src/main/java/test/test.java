package test;

import dao.*;
import model.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class test {

    public static void main(String[] args) {

        UlogaDAO uDao = new UlogaDAO();

        Uloga u = new Uloga();

        KorisnikDAO dao = new KorisnikDAO();

        Korisnik k = new Korisnik();

        u.setNaziv("ADMIN");
        u.setIdUloga(2);
       // k.setIdKorisnik(1);
        k.setIme("Hamo2");
        k.setPrezime("Hamić2");
        k.setUsername("hamo.hamic");
        k.setPasswordHash("12345");
        k.setUloga(u);

        //uDao.unesiUlogu(u);

        //dao.unesiKorisnika(k);

       // uDao.obrisiUlogu(1);

        SocijalniStatusDAO sDao = new SocijalniStatusDAO();

        SocijalniStatus s = new SocijalniStatus();

        //s.setNaziv("zaposlen3");
        //sDao.unesiStatus(s);


        StudentDAO studentDAO  = new StudentDAO();
        Student student = new Student();

        student.setIme("hkjh");
        student.setPrezime("jhhj");
        student.setBrojIndeksa("543");
        student.setGodinaStudija(3);
        student.setFakultet("Politehnički");
        student.setSocijalniStatus(s);
        student.setProsjek(8.5);
        student.setEmail("ujgjgjgjg.23@size.ba");

        //studentDAO.unesiStudent(student);

        List<Student> studenti = new ArrayList<>();

        studenti = studentDAO.dohvatiSveStudente();

       // studenti.forEach(System.out::println);



        VrstaDokumentaDAO vdDao = new VrstaDokumentaDAO();
        VrstaDokumenta vd = new VrstaDokumenta();

        vd.setNaziv("uplatnica");
        vd.setIdVrsta(1);

        //vdDao.unesiVrstu(vd);

        DokumentDAO  dDao = new DokumentDAO();
        Dokument d = new Dokument();


        PrijavaDAO pDao = new PrijavaDAO();

        Prijava prijava = new Prijava();

        StatusPrijaveDAO spDao = new StatusPrijaveDAO();

        StatusPrijave sp = new StatusPrijave();

        sp.setNaziv("na pregledu");
        sp.setIdStatus(1);

        //spDao.unesiStatus(sp);

        prijava.setIdStudent(4);
        prijava.setDatumPrijava(LocalDate.parse("2025-01-06"));
        prijava.setNapomena("djngjdfgk");
        prijava.setAkademskaGodina(2025);
        prijava.setStatusPrijave(sp);
        prijava.setUkupniBodovi(20);

       // pDao.unesiPrijavu(prijava);

       // d.setIdDokument(3);
        d.setNaziv("uplatnica za ");
        d.setBrojBodova(1);
        d.setDostavljen(true);
        d.setVrstaDokumenta(vd);
        d.setDatumUpload(LocalDate.parse("2025-01-06"));




       // dDao.unesiDokument(d, 3);

        List<Dokument> docs = new ArrayList<>();

        docs = dDao.dohvatiSveDokumente();
        //docs.forEach(System.out::println);

        //dDao.azurirajDokument(d, 3);

       // dDao.obrisiDokument(3);

        List<Korisnik> kor;

        kor = dao.dohvatiSveKorisnike();
        //kor.forEach(System.out::println);

        //dao.azurirajKorisnika(k);
        //dao.obrisiKorisnika(1);

        Korisnik k2 = new Korisnik();
       k2 = dao.nadjiPoUsernameIPassword("hamo.hamic", "12345");
        System.out.println(k2.getUsername());
    }
}
