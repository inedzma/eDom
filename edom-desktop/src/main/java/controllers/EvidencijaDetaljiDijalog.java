package controllers;

import api.ApiException;
import api.EvidencijaApi;
import api.PrijavaApi;
import ba.unze.edom.common.dto.student.EvidencijaOznakeDTO;
import ba.unze.edom.common.dto.student.EvidencijaRedDTO;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import ba.unze.edom.common.dto.dokumenti.GenerisaniDokumentDTO;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Objects;

/** Detalji studenta iz centralne evidencije + oznake za useljenje. */
public class EvidencijaDetaljiDijalog {

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

    private final EvidencijaRedDTO r;
    private final Runnable poslije;
    private int red;

    public EvidencijaDetaljiDijalog(EvidencijaRedDTO r, Runnable poslije) {
        this.r = r;
        this.poslije = poslije;
    }

    public void prikazi() {
        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(6);
        g.setPadding(new Insets(10));

        sekcija(g, "Lični podaci");
        polje(g, "Ime roditelja", r.imeRoditelja());
        polje(g, "JMBG", r.jmbg());
        polje(g, "Lična karta", r.brojLicneKarte());
        polje(g, "Spol", r.spol());
        polje(g, "Adresa", r.adresa());
        polje(g, "Kanton / općina", spoji(r.kanton(), r.opcina()));
        polje(g, "E-mail", r.email());
        polje(g, "Telefon", r.telefon());
        polje(g, "Telefon roditelja", r.telefonRoditelja());

        sekcija(g, "Studij i prijava");
        polje(g, "Fakultet", r.fakultet());
        polje(g, "Godina studija", r.godinaStudija() != null ? r.godinaStudija() + ". godina" : null);
        polje(g, "Broj indeksa", r.brojIndeksa());
        polje(g, "Status prijave", r.status());
        polje(g, "Kategorija", r.kategorija());
        polje(g, "Rang lista", r.primljen() ? "Primljen" : "Nije primljen");
        polje(g, "Bodovi", r.bodovi() != null ? String.format("%.2f", r.bodovi()) : null);
        polje(g, "Datum prijave", dat(r.datumPrijave()));

        sekcija(g, "Smještaj");
        polje(g, "Soba", r.soba() != null ? r.soba() + " (" + r.statusSmjestaja() + ")" : "nije dodijeljena");
        polje(g, "Useljen", dat(r.datumUseljenja()));
        polje(g, "Iseljen", dat(r.datumIseljenja()));
        polje(g, "Ključ", kljuc(r));

        sekcija(g, "Useljenje – predano / preuzeto");
        boolean moze = r.statusSmjestaja() != null;

        CheckBox chkKartica = new CheckBox("Kartica za ishranu preuzeta" + kada(r.karticaPreuzeta()));
        chkKartica.setSelected(r.karticaPreuzeta() != null);

        CheckBox chkKarticaRaz = new CheckBox("Kartica razdužena (vraćena)" + kada(r.karticaRazduzena()));
        chkKarticaRaz.setSelected(r.karticaRazduzena() != null);
        chkKarticaRaz.disableProperty().bind(chkKartica.selectedProperty().not());
        chkKartica.selectedProperty().addListener((o, a, n) -> { if (!n) chkKarticaRaz.setSelected(false); });

        CheckBox chkLjekarsko = new CheckBox("Ljekarsko uvjerenje predano" + kada(r.ljekarskoPredano()));
        chkLjekarsko.setSelected(r.ljekarskoPredano() != null);

        CheckBox chkUvjerenje = new CheckBox(r.uvjerenjeUzPrijavu()
                ? "Uvjerenje s fakulteta – priloženo uz prijavu"
                : "Uvjerenje s fakulteta predano" + kada(r.uvjerenjeFakultetPredano()));
        chkUvjerenje.setSelected(r.uvjerenjeUzPrijavu() || r.uvjerenjeFakultetPredano() != null);
        chkUvjerenje.setDisable(r.uvjerenjeUzPrijavu());

        for (CheckBox c : new CheckBox[]{chkKartica, chkKarticaRaz, chkLjekarsko, chkUvjerenje}) {
            if (!moze && !c.disableProperty().isBound()) c.setDisable(true);
            g.add(c, 0, red++, 2, 1);
        }
        if (!moze) {
            chkKarticaRaz.disableProperty().unbind();
            chkKarticaRaz.setDisable(true);
            Label info = new Label("Oznake se upisuju kad student dobije sobu.");
            info.setStyle("-fx-text-fill: #6b7a90;");
            g.add(info, 0, red++, 2, 1);
        }

        // napomena uz prijavu: piše je student pri predaji, admin je može mijenjati
        sekcija(g, "Napomena uz prijavu");
        TextArea taNapomena = new TextArea(r.napomenaPrijave() != null ? r.napomenaPrijave() : "");
        taNapomena.setPromptText("Student nije ostavio napomenu.");
        taNapomena.setWrapText(true);
        taNapomena.setPrefRowCount(3);
        taNapomena.setPrefWidth(460);
        g.add(taNapomena, 0, red++, 2, 1);
        Label napInfo = new Label("Piše je student pri predaji prijave. Administrator je može ispraviti ili dopuniti (najviše 500 znakova).");
        napInfo.setStyle("-fx-text-fill: #6b7a90;");
        napInfo.setWrapText(true);
        napInfo.setMaxWidth(460);
        g.add(napInfo, 0, red++, 2, 1);

        // napomena koju je admin poslao studentu mailom (student.napomena) – samo za čitanje
        if (r.napomena() != null && !r.napomena().isBlank()) {
            sekcija(g, "Napomena poslana studentu");
            Label n = new Label(r.napomena());
            n.setWrapText(true);
            n.setMaxWidth(460);
            g.add(n, 0, red++, 2, 1);
        }

        ScrollPane sp = new ScrollPane(g);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(540);
        sp.setPrefViewportWidth(500);

        ButtonType btPrijava = new ButtonType("Detalji prijave", ButtonBar.ButtonData.LEFT);
        ButtonType btUgovor = new ButtonType("Ugovor (.docx)", ButtonBar.ButtonData.LEFT);
        ButtonType btRevers = new ButtonType("Revers (.docx)", ButtonBar.ButtonData.LEFT);
        ButtonType btSacuvaj = new ButtonType("Sačuvaj", ButtonBar.ButtonData.OK_DONE);

        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Detalji studenta");
        d.setHeaderText(r.ime() + " " + r.prezime() + "   ·   " + r.sifra());
        d.getDialogPane().setContent(sp);
        d.getDialogPane().getButtonTypes().add(btPrijava);
        d.getDialogPane().getButtonTypes().add(btSacuvaj);
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // ova dugmad ne zatvaraju dijalog
        d.getDialogPane().lookupButton(btPrijava).addEventFilter(ActionEvent.ACTION, e -> {
            e.consume();
            otvoriPrijavu(r.idPrijava());
        });
        if (moze) {
            d.getDialogPane().lookupButton(btUgovor).addEventFilter(ActionEvent.ACTION, e -> {
                e.consume();
                preuzmi(true, d);
            });
            d.getDialogPane().lookupButton(btRevers).addEventFilter(ActionEvent.ACTION, e -> {
                e.consume();
                preuzmi(false, d);
            });
        }

        if (d.showAndWait().orElse(ButtonType.CLOSE) == btSacuvaj) {
            EvidencijaOznakeDTO z = moze
                    ? new EvidencijaOznakeDTO(
                    chkKartica.isSelected(), chkKarticaRaz.isSelected(),
                    chkLjekarsko.isSelected(), chkUvjerenje.isSelected() && !r.uvjerenjeUzPrijavu())
                    : null;

            String nova = taNapomena.getText();
            if (nova != null && nova.trim().length() > 500) {
                new Alert(Alert.AlertType.WARNING, "Napomena može imati najviše 500 znakova.").showAndWait();
                return;
            }
            boolean napomenaPromijenjena = !Objects.equals(ocisti(nova), ocisti(r.napomenaPrijave()));
            sacuvaj(z, nova, napomenaPromijenjena);
        }
    }

    private void sacuvaj(EvidencijaOznakeDTO z, String napomena, boolean napomenaPromijenjena) {
        if (z == null && !napomenaPromijenjena) return;   // nema šta snimiti

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                if (z != null) EvidencijaApi.sacuvajOznake(r.idPrijava(), z);
                if (napomenaPromijenjena) PrijavaApi.sacuvajNapomenu(r.idPrijava(), napomena);
                return null;
            }
        };
        task.setOnSucceeded(e -> poslije.run());
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (!(ex instanceof ApiException)) ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    ex instanceof ApiException ? ex.getMessage() : "Promjene nisu sačuvane.").showAndWait();
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private static String ocisti(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private void otvoriPrijavu(Integer idPrijave) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/detalji-prijave.fxml"));
            Scene scene = new Scene(loader.load());
            DetaljiPrijaveController c = loader.getController();
            c.ucitaj(idPrijave);
            Stage stage = new Stage();
            stage.setTitle("Detalji prijave");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------- pomoćne ----------

    private void sekcija(GridPane g, String naslov) {
        Label l = new Label(naslov);
        l.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 8 0 2 0;");
        g.add(l, 0, red++, 2, 1);
    }

    private void polje(GridPane g, String naziv, String vrijednost) {
        Label n = new Label(naziv + ":");
        n.setStyle("-fx-text-fill: #6b7a90;");
        Label v = new Label(vrijednost == null || vrijednost.isBlank() ? "—" : vrijednost);
        v.setWrapText(true);
        g.add(n, 0, red);
        g.add(v, 1, red++);
    }

    static String kljuc(EvidencijaRedDTO r) {
        if (r.kljuceviPreuzeti() == 0) return r.soba() != null ? "Nije zadužen" : null;
        int kod = r.kljuceviPreuzeti() - r.kljuceviVraceni();
        return kod > 0 ? "Zadužen (" + kod + ")" : "Vraćen";
    }

    private static String dat(LocalDate d) {
        return d == null ? null : d.format(D);
    }

    private static String kada(LocalDate d) {
        return d == null ? "" : "  (" + d.format(D) + ")";
    }

    private static String spoji(String a, String b) {
        if (a == null) return b;
        if (b == null) return a;
        return a + " / " + b;
    }

    /** Generiše ugovor ili revers na serveru, sačuva ga i ponudi da se otvori u Wordu. */
    private void preuzmi(boolean ugovor, Dialog<?> d) {
        Task<GenerisaniDokumentDTO> task = new Task<>() {
            @Override
            protected GenerisaniDokumentDTO call() {
                return ugovor ? EvidencijaApi.ugovor(r.idPrijava()) : EvidencijaApi.revers(r.idPrijava());
            }
        };
        task.setOnSucceeded(e -> {
            GenerisaniDokumentDTO g = task.getValue();

            FileChooser fc = new FileChooser();
            fc.setTitle(ugovor ? "Sačuvaj ugovor" : "Sačuvaj revers");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word dokument", "*.docx"));
            fc.setInitialFileName(g.naziv());
            File f = fc.showSaveDialog(d.getDialogPane().getScene().getWindow());
            if (f == null) return;

            try {
                Files.write(f.toPath(), Base64.getDecoder().decode(g.b64()));
            } catch (IOException ex) {
                new Alert(Alert.AlertType.ERROR, "Fajl nije sačuvan: " + ex.getMessage()).showAndWait();
                return;
            }

            Alert pitanje = new Alert(Alert.AlertType.CONFIRMATION,
                    "Dokument je sačuvan. Otvoriti ga sada?", ButtonType.YES, ButtonType.NO);
            pitanje.setHeaderText(null);
            if (pitanje.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                try {
                    java.awt.Desktop.getDesktop().open(f);
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.WARNING, "Otvorite ga ručno: " + f.getAbsolutePath()).showAndWait();
                }
            }
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (!(ex instanceof ApiException)) ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    ex instanceof ApiException ? ex.getMessage() : "Dokument nije generisan.").showAndWait();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}