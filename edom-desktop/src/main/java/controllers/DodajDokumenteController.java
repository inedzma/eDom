package controllers;

import api.ApiException;
import api.RucniUnosApi;
import ba.unze.edom.common.dto.SpisakUnosDTO;
import ba.unze.edom.common.dto.SpisakUnosZahtjevDTO;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import unos.NacrtPrijave;
import util.Ekrani;
import util.KoraciUnosa;

import java.util.ArrayList;

/** Korak 4: dokumenti, po istom spisku kao na webu. */
public class DodajDokumenteController {

    @FXML private HBox stepper;
    @FXML private ProgressBar traka;
    @FXML private Label lblNapredak;
    @FXML private Label lblUcitavanje;
    @FXML private VBox vboxSpisak;
    @FXML private Button btnDalje;

    private NacrtPrijave nacrt;

    @FXML
    public void initialize() {
        KoraciUnosa.popuni(stepper, traka, 4);
    }

    public void setNacrt(NacrtPrijave n) {
        this.nacrt = n;
        String kljuc = n.kljucSpiska();
        if (n.spisak != null && kljuc.equals(n.spisakZa)) prikazi();
        else ucitaj(kljuc);
    }

    // ---------- server ----------

    private void ucitaj(String kljuc) {
        btnDalje.setDisable(true);
        lblUcitavanje.setText("Učitavam spisak dokumenata...");
        lblUcitavanje.setVisible(true);
        lblUcitavanje.setManaged(true);

        SpisakUnosZahtjevDTO z = new SpisakUnosZahtjevDTO(
                nacrt.student.godinaStudija(), nacrt.brojClanova, new ArrayList<>(nacrt.kriteriji.keySet()));

        Task<SpisakUnosDTO> t = new Task<>() {
            @Override
            protected SpisakUnosDTO call() {
                return RucniUnosApi.spisakDokumenata(z);
            }
        };
        t.setOnSucceeded(e -> {
            nacrt.spisak = t.getValue();
            nacrt.spisakZa = kljuc;
            prikazi();
        });
        t.setOnFailed(e -> {
            Throwable ex = t.getException();
            if (!(ex instanceof ApiException)) ex.printStackTrace();
            lblUcitavanje.setText(ex instanceof ApiException
                    ? "Spisak nije učitan: " + ex.getMessage()
                    : "Spisak dokumenata nije učitan. Provjerite vezu sa serverom i vratite se korak nazad.");
        });

        Thread th = new Thread(t);
        th.setDaemon(true);
        th.start();
    }

    // ---------- prikaz ----------

    private void prikazi() {
        lblUcitavanje.setVisible(false);
        lblUcitavanje.setManaged(false);
        btnDalje.setDisable(false);
        vboxSpisak.getChildren().clear();

        SpisakUnosDTO s = nacrt.spisak;

        vboxSpisak.getChildren().add(naslov("Obavezni dokumenti"));
        for (SpisakUnosDTO.Zahtjev z : s.obavezni()) {
            vboxSpisak.getChildren().add(z.poClanu() ? redPoClanu(z) : red(z, true));
        }

        vboxSpisak.getChildren().add(naslov("Dokumenti za dodatne kriterije"));
        if (s.dodatni().isEmpty() && s.kriterijiBezDokaza().isEmpty()) {
            vboxSpisak.getChildren().add(opis("Nije označen nijedan dodatni kriterij."));
        }
        for (SpisakUnosDTO.Zahtjev z : s.dodatni()) {
            vboxSpisak.getChildren().add(red(z, false));
        }

        if (!s.kriterijiBezDokaza().isEmpty()) {
            VBox kartica = kartica("Kriteriji bez posebnog dokumenta",
                    "Provjerite iz ostale dokumentacije i označite šta je potvrđeno.", false);
            for (SpisakUnosDTO.Kriterij k : s.kriterijiBezDokaza()) {
                CheckBox chk = kvacica("Provjereno: " + k.naziv());
                chk.setSelected(nacrt.provjereniKriteriji.contains(k.sifra()));
                chk.selectedProperty().addListener((obs, o, n) -> {
                    if (n) nacrt.provjereniKriteriji.add(k.sifra());
                    else nacrt.provjereniKriteriji.remove(k.sifra());
                });
                kartica.getChildren().add(chk);
            }
            vboxSpisak.getChildren().add(kartica);
        }

        osvjeziNapredak();
    }

    /** Red s jednom ili više vrsta dokumenta: kvačica za svaku vrstu, dovoljna je jedna. */
    private VBox red(SpisakUnosDTO.Zahtjev z, boolean obavezan) {
        VBox kartica = kartica(z.naziv(), z.opis(), obavezan);

        if (z.vrste().isEmpty()) {
            kartica.getChildren().add(opis("Ova vrsta dokumenta nije podešena u bazi (nema šifru)."));
        }
        for (SpisakUnosDTO.Vrsta v : z.vrste()) {
            String k = NacrtPrijave.kljucVrste(z, v.id());
            CheckBox chk = kvacica(z.vrste().size() == 1 ? "Dostavljeno" : "Dostavljeno: " + v.naziv());
            chk.setSelected(nacrt.dostavljeno.contains(k));
            chk.selectedProperty().addListener((obs, o, n) -> {
                if (n) nacrt.dostavljeno.add(k);
                else nacrt.dostavljeno.remove(k);
                osvjeziNapredak();
            });
            kartica.getChildren().add(chk);
        }
        return kartica;
    }

    /** Dokumenti članova: za svakog člana bira se vrsta dokumenta koji je donio. */
    private VBox redPoClanu(SpisakUnosDTO.Zahtjev z) {
        VBox kartica = kartica(z.naziv(), z.opis(), true);

        for (int i = 0; i < z.potrebno(); i++) {
            final int clan = i;

            Label ime = new Label(nacrt.imeClana(i));
            ime.getStyleClass().add("member-title");
            ime.setMinWidth(160);

            ComboBox<SpisakUnosDTO.Vrsta> cmb = new ComboBox<>(FXCollections.observableArrayList(z.vrste()));
            cmb.setPromptText("Dokument nije dostavljen");
            cmb.getStyleClass().add("combo");
            cmb.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(cmb, Priority.ALWAYS);
            cmb.setCellFactory(c -> vrstaCell());
            cmb.setButtonCell(vrstaCell());

            Integer vec = nacrt.dokumentClana.get(i);
            if (vec != null) {
                z.vrste().stream().filter(v -> v.id().equals(vec)).findFirst().ifPresent(cmb::setValue);
            }
            cmb.valueProperty().addListener((obs, o, n) -> {
                if (n == null) nacrt.dokumentClana.remove(clan);
                else nacrt.dokumentClana.put(clan, n.id());
                osvjeziNapredak();
            });

            Button ukloni = new Button("Ukloni");
            ukloni.setOnAction(e -> cmb.setValue(null));

            HBox red = new HBox(12, ime, cmb, ukloni);
            red.setAlignment(Pos.CENTER_LEFT);
            kartica.getChildren().add(red);
        }
        return kartica;
    }

    private void osvjeziNapredak() {
        int ukupno = nacrt.spisak.obavezni().size();
        int fali = nacrt.falaObaveznih();
        lblNapredak.setText("Obavezni dokumenti: " + (ukupno - fali) + " od " + ukupno
                + (fali == 0 ? " (sve dostavljeno)" : ""));
    }

    // ---------- navigacija ----------

    @FXML
    private void onDalje() {
        if (nacrt.spisak == null) return;

        int fali = nacrt.falaObaveznih();
        if (fali > 0) {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                    "Nije označeno " + fali + " obaveznih dokumenata.\n\n"
                            + "Prijavu možete podnijeti, ali ti podaci ostaju neverifikovani. Nastaviti?");
            a.setHeaderText(null);
            if (a.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        }

        Ekrani.<PregledUnosaController>zamijeni(vboxSpisak, "/views/pregled-unosa.fxml",
                c -> c.setNacrt(nacrt));
    }

    @FXML
    private void onNazad() {
        Ekrani.<KriterijiUnosController>zamijeni(vboxSpisak, "/views/kriteriji-unos.fxml",
                c -> c.setNacrt(nacrt));
    }

    // ---------- pomoćne ----------

    private VBox kartica(String naslov, String opis, boolean obavezan) {
        Label lbl = new Label(naslov);
        lbl.getStyleClass().add("doc-title");
        lbl.setWrapText(true);

        Region razmak = new Region();
        HBox.setHgrow(razmak, Priority.ALWAYS);

        Label oznaka = new Label(obavezan ? "obavezno" : "dodatno");
        oznaka.getStyleClass().add("doc-subtitle");

        HBox zaglavlje = new HBox(10, lbl, razmak, oznaka);
        zaglavlje.setAlignment(Pos.CENTER_LEFT);

        VBox k = new VBox(8, zaglavlje);
        if (opis != null && !opis.isBlank()) k.getChildren().add(opis(opis));
        k.getStyleClass().add("doc-card");
        return k;
    }

    private Label naslov(String tekst) {
        Label l = new Label(tekst);
        l.getStyleClass().add("section-pill");
        return l;
    }

    private Label opis(String tekst) {
        Label l = new Label(tekst);
        l.getStyleClass().add("doc-subtitle");
        l.setWrapText(true);
        return l;
    }

    private CheckBox kvacica(String tekst) {
        CheckBox c = new CheckBox(tekst);
        c.getStyleClass().add("checkbox");
        c.setWrapText(true);
        return c;
    }

    private ListCell<SpisakUnosDTO.Vrsta> vrstaCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(SpisakUnosDTO.Vrsta v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : v.naziv());
            }
        };
    }
}