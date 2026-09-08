package controllers;

import controllers.DodajDokumenteControllers.*;
import dao.DokumentDAO;
import dao.PrijavaDAO;
import dao.VrstaDokumentaDAO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import service.BraniociRezultat;
import model.Dokument;
import model.VrstaDokumenta;
import service.KriterijPoOsnovuUdaljenosti;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DodajDokumenteController {

    @FXML private int prijavaId;
    @FXML private Accordion accordionDokumenti;

    private int clanovi;
    private double udaljenost;
    private double prosjek;     // Bitno: postavljeno preko settera
    private int godinaStudija;  // Bitno: postavljeno preko settera

    private BraniociRezultat braniociRezultat;
    private double bodoviUdaljenost;
    private boolean isIzbjeglica;
    private boolean isBratSestra;

    // Reference na sub-kontrolere za validaciju prije kraja
    private DodajKucnuListuController kucnaDokumentControllerRef;
    private KucnaListaController kucnaControllerRef;

    private final VrstaDokumentaDAO vdDao = new VrstaDokumentaDAO();
    private final List<VrstaDokumenta> vrsteDokumenata = vdDao.dohvatiSveVrste();

    @FXML
    public void initialize() {
        accordionDokumenti.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Stage stage = (Stage) newScene.getWindow();
                Platform.runLater(() -> {
                    stage.setMaximized(true);
                });
            }
        });
    }

    // Poziva se iz Admin/Main kontrolera da postavi podatke
    public void setClanovi(int clanovi) {
        this.clanovi = clanovi;
        initAccordion(); // Tek kad imamo podatke, crtamo interfejs
    }

    public void setProsjek(double prosjek) {
        this.prosjek = prosjek;
    }

    public void setGodinaStudija(int godinaStudija) {
        this.godinaStudija = godinaStudija;
    }

    public void setPrijavaId(int prijavaId) {
        this.prijavaId = prijavaId;
    }

    public void setUdaljenost(double udaljenost) {
        this.udaljenost = udaljenost;
        // Odmah računamo i spašavamo bodove za udaljenost jer tu nema dodatnih inputa
        KriterijPoOsnovuUdaljenosti kriterij = new KriterijPoOsnovuUdaljenosti();
        bodoviUdaljenost = kriterij.izracunajBodove(udaljenost);
        new PrijavaDAO().dodajBodoveNaPrijavu(prijavaId, bodoviUdaljenost);
    }

    public void setBraniociRezultat(BraniociRezultat rezultat) {
        this.braniociRezultat = rezultat;
        if (rezultat != null && rezultat.getBodovi() > 0) {
            new PrijavaDAO().dodajBodoveNaPrijavu(prijavaId, rezultat.getBodovi());
        }
    }

    public void setIzbjeglica(boolean izbjeglica){ isIzbjeglica = izbjeglica; }
    public void setBratSestra(boolean bratSestra){ isBratSestra = bratSestra; }

    // --- KREIRANJE UI SEKCIJA ---

    private void initAccordion() {
        accordionDokumenti.getPanes().clear();

        if (clanovi <= 0) return;

        // 1. Osnovni dokumenti
        TitledPane paneOsnovni = new TitledPane("Osnovni dokumenti", new VBox(10));
        VBox vboxOsnovni = (VBox) paneOsnovni.getContent();
        dodajPrijavniObrazac(vboxOsnovni);
        dodajCipsSekciju(vboxOsnovni);
        dodajUplatnicuSekciju(vboxOsnovni);
        dodajGarancijuSekciju(vboxOsnovni);
        dodajKucnuListuDokument(vboxOsnovni);

        // 2. Domaćinstvo
        TitledPane paneDomacinstvo = new TitledPane("Domaćinstvo", null); // Sadržaj se puni metodom
        dodajKucnuListuSekciju(paneDomacinstvo);

        // 3. Fakultet / Škola (OVDJE JE TVOJ PROSJEK)
        TitledPane paneFaks = new TitledPane("Dokumenti sa fakulteta/škole", new VBox(10));
        VBox vboxFaks = (VBox) paneFaks.getContent();
        dodajSekcijuSvjedodzbe(vboxFaks); // <--- KLJUČNO
        dodajNagradeSekciju(vboxFaks);

        accordionDokumenti.getPanes().addAll(paneOsnovni, paneDomacinstvo, paneFaks);

        // 4. Dodatni bodovi (Opcionalno)
        if ((braniociRezultat != null && braniociRezultat.getBodovi() > 0) || isIzbjeglica) {
            TitledPane paneDodatni = new TitledPane("Dodatni bodovi", new VBox(10));
            VBox vboxDodatni = (VBox) paneDodatni.getContent();
            if(isIzbjeglica) dodajIzbjegliceDokument(vboxDodatni);
            if(braniociRezultat != null && braniociRezultat.getBodovi() > 0) dodajSekcijuDodatniBodovi(vboxDodatni);
            accordionDokumenti.getPanes().add(paneDodatni);
        }
    }

    // --- POJEDINAČNE METODE ZA UCITAVANJE FXML-ova ---

    private void dodajSekcijuSvjedodzbe(VBox parent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/DodajDokumenteSections/prosjek.fxml"));
            VBox box = loader.load();
            ProsjekDokumentController controller = loader.getController();

            // Šaljemo SAMO sirove podatke. Računanje radi sam kontroler kad korisnik klikne "Dodaj"
            // jer glavni kontroler ne zna koliko ispita je student položio (to se unosi u tom View-u).
            controller.init(
                    prijavaId,
                    godinaStudija,
                    prosjek,
                    vdDao.dohvatiVrstuPoId(7), // Svjedodzba
                    vdDao.dohvatiVrstuPoId(8), // Uvjerenje
                    vdDao.dohvatiVrstuPoId(9)  // Indeks
            );
            parent.getChildren().add(box);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ... (Ostale metode ostaju iste: dodajCipsSekciju, dodajUplatnicuSekciju, itd.)
    // Samo ih kopiraj iz starog koda ako nisu mijenjane, ali logika za Prosjek je iznad.

    private void dodajPrijavniObrazac(VBox parent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/DodajDokumenteSections/obrazac-prijava.fxml"));
            parent.getChildren().add(loader.load());
            ObrazacPrijaveController c = loader.getController();
            c.init(prijavaId, vdDao.dohvatiVrstuPoId(17));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void dodajCipsSekciju(VBox parent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/DodajDokumenteSections/cips.fxml"));
            parent.getChildren().add(loader.load());
            CipsDokumentController c = loader.getController();
            c.init(prijavaId, vdDao.dohvatiVrstuPoId(6), bodoviUdaljenost);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void dodajUplatnicuSekciju(VBox parent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/DodajDokumenteSections/uplatnica.fxml"));
            parent.getChildren().add(loader.load());
            UplatnicaDokumentConroller c = loader.getController();
            c.init(prijavaId, vdDao.dohvatiVrstuPoId(1));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void dodajGarancijuSekciju(VBox parent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/DodajDokumenteSections/garancija.fxml"));
            parent.getChildren().add(loader.load());
            GarancijaDokumentController c = loader.getController();
            c.init(prijavaId, Arrays.asList(vdDao.dohvatiVrstuPoId(5), vdDao.dohvatiVrstuPoId(16), vdDao.dohvatiVrstuPoId(24)));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void dodajKucnuListuDokument(VBox parent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/DodajDokumenteSections/kucna-dokument.fxml"));
            parent.getChildren().add(loader.load());
            kucnaDokumentControllerRef = loader.getController();
            kucnaDokumentControllerRef.init(prijavaId, vdDao.dohvatiVrstuPoId(18));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void dodajKucnuListuSekciju(TitledPane pane) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/DodajDokumenteSections/kucna-lista.fxml"));
            VBox root = loader.load();
            kucnaControllerRef = loader.getController();
            kucnaControllerRef.init(prijavaId, clanovi, vrsteDokumenata);
            pane.setContent(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void dodajNagradeSekciju(VBox parent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/DodajDokumenteSections/nagrade.fxml"));
            parent.getChildren().add(loader.load());
            NagradeDokumentController c = loader.getController();
            c.init(prijavaId, vdDao.dohvatiVrstuPoId(10));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void dodajIzbjegliceDokument(VBox parent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/DodajDokumenteSections/izbjeglica-dokument.fxml"));
            parent.getChildren().add(loader.load());
            IzbjeglicaDokumentController c = loader.getController();
            c.init(prijavaId);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void dodajSekcijuDodatniBodovi(VBox parent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/DodajDokumenteSections/dodatni-bodovi.fxml"));
            parent.getChildren().add(loader.load());
            DodatniBodoviController c = loader.getController();
            String naziv = (braniociRezultat != null) ? braniociRezultat.getNaziv() : "";
            c.init(prijavaId, naziv, List.of(vdDao.dohvatiVrstuPoId(19), vdDao.dohvatiVrstuPoId(20), vdDao.dohvatiVrstuPoId(21), vdDao.dohvatiVrstuPoId(22), vdDao.dohvatiVrstuPoId(23)), braniociRezultat.getBodovi());
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    private void onPodnesiPrijavu(javafx.event.ActionEvent event) {
        if (kucnaControllerRef == null || kucnaDokumentControllerRef == null) {
            showAlert("Greška", "Kućna lista nije inicijalizirana."); return;
        }

        // Završi unos kućne liste
        double bodoviKucna = kucnaControllerRef.zavrsiUnos();
        int dokumentId = kucnaDokumentControllerRef.getKucnaListaDokumentId();

        if (isBratSestra) bodoviKucna += 2;

        if (dokumentId <= 0) {
            showAlert("Greška", "Morate prvo priložiti/sačuvati dokument Kućne liste (kliknite 'Dodaj' kod dokumenta).");
            return;
        }

        // Dodaj bodove na sam dokument kućne liste
        new DokumentDAO().dodajBodove(dokumentId, bodoviKucna);
        // Dodaj te bodove i na ukupnu prijavu
        new PrijavaDAO().dodajBodoveNaPrijavu(prijavaId, bodoviKucna);

        showAlert("Uspješno", "Prijava je uspješno podnesena.");
        otvoriAdminMain();
    }

    private void otvoriAdminMain() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/admin-main-view.fxml"));
            Parent root = loader.load();
            AdminController adminController = loader.getController();
            Stage stage = (Stage) accordionDokumenti.getScene().getWindow();
            stage.setScene(new Scene(root));
            Platform.runLater(() -> { stage.setMaximized(true); });
            adminController.loadViewPublic("/views/prijave.fxml");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}