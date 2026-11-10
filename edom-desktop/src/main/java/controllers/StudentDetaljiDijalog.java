package controllers;

import api.SobeApi;
import ba.unze.edom.common.dto.student.StudentDetaljiDTO;
import ba.unze.edom.common.dto.student.StudentIzmjenaDTO;
import ba.unze.edom.common.util.Fakulteti;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Detalji studenta: kontakt, studij, prijava, soba, napomena (šalje se mailom) i izmjena podataka. */
final class StudentDetaljiDijalog {

    private StudentDetaljiDijalog() {}

    /** Učita detalje sa servera i otvori dijalog. */
    static void otvori(int idPrijava) {
        SobeController.pozovi(() -> SobeApi.studentDetalji(idPrijava), StudentDetaljiDijalog::prikazi);
    }

    private static void prikazi(StudentDetaljiDTO s) {
        Dialog<Void> d = new Dialog<>();
        d.setTitle(s.ime() + " " + s.prezime());
        d.setHeaderText(s.ime() + " " + s.prezime()
                + (s.brojIndeksa() != null ? "  ·  indeks " + s.brojIndeksa() : ""));
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        d.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(StudentDetaljiDijalog.class.getResource("/styles/sobe.css")).toExternalForm());

        VBox sadrzaj = new VBox(14);
        sadrzaj.setPadding(new Insets(6, 4, 4, 4));
        sadrzaj.setPrefWidth(620);

        // ---------- uredi ----------
        Button uredi = new Button("Uredi podatke studenta");
        uredi.getStyleClass().add("sobe-btn");
        uredi.setMinWidth(Region.USE_PREF_SIZE);
        uredi.setOnAction(e -> uredi(s, d));
        HBox vrh = new HBox(uredi);
        vrh.setAlignment(Pos.CENTER_RIGHT);
        sadrzaj.getChildren().add(vrh);

        // ---------- kontakt ----------
        sadrzaj.getChildren().add(naslov("Kontakt"));
        GridPane kontakt = mreza();
        red(kontakt, "Telefon", s.telefon(), true);
        red(kontakt, "Email", s.email(), true);
        red(kontakt, "Adresa", s.adresa(), false);
        red(kontakt, "Roditelj", s.imeRoditelja(), false);
        red(kontakt, "Telefon roditelja", s.telefonRoditelja(), true);
        red(kontakt, "Prebivalište", s.opcina() != null ? s.opcina() + ", " + s.kanton() : null, false);
        red(kontakt, "Lična karta", s.brojLicneKarte(), true);
        sadrzaj.getChildren().add(kontakt);

        // ---------- studij i prijava ----------
        sadrzaj.getChildren().add(naslov("Studij i prijava"));
        GridPane studij = mreza();
        red(studij, "Fakultet", s.fakultet(), false);
        red(studij, "Godina studija", SobeController.godinaTekst(s.godinaStudija()), false);
        red(studij, "JMBG", s.jmbg(), true);
        red(studij, "Akademska godina", s.akademskaGodina() + "/" + (s.akademskaGodina() + 1), false);
        red(studij, "Status prijave", s.statusPrijave(), false);
        red(studij, "Bodovi", String.format("%.2f", s.bodovi())
                + (s.rang() != null ? "  ·  " + s.rang() + ". na rang listi" : ""), false);
        red(studij, "Soba", s.brojSobe() != null
                ? s.brojSobe() + " (" + statusTekst(s.statusSmjestaja()) + ")" : "nema sobe", false);
        sadrzaj.getChildren().add(studij);

        // ---------- napomena ----------
        sadrzaj.getChildren().add(naslov("Napomena studentu"));

        TextArea nova = new TextArea(s.napomena() != null ? s.napomena() : "");
        nova.setPromptText("Npr. Molimo da do petka donesete original uvjerenja o položenim ispitima.");
        nova.setWrapText(true);
        nova.setPrefRowCount(4);

        Button posalji = new Button("Sačuvaj i pošalji studentu");
        posalji.getStyleClass().add("sobe-btn-glavno");
        Label info = new Label(s.email() != null && !s.email().isBlank()
                ? "Napomena se šalje na " + s.email() + ". Nova napomena zamijeni staru."
                : "Student nema email: napomena će se samo sačuvati.");
        info.getStyleClass().add("sobe-podnaslov");
        info.setWrapText(true);
        Region razmak = new Region();
        HBox.setHgrow(razmak, Priority.ALWAYS);
        HBox akcije = new HBox(10, info, razmak, posalji);
        akcije.setAlignment(Pos.CENTER_LEFT);

        posalji.setOnAction(e -> {
            String tekst = nova.getText();
            if (tekst == null || tekst.isBlank()) return;
            posalji.setDisable(true);
            SobeController.pozovi(() -> SobeApi.posaljiNapomenu(s.idPrijava(), tekst), poslano -> {
                posalji.setDisable(false);
                SobeController.info(poslano
                        ? "Napomena je sačuvana i poslana studentu mailom."
                        : "Napomena je sačuvana, ali mail NIJE poslan. Provjerite email studenta i postavke maila.");
            }, () -> posalji.setDisable(false));
        });

        sadrzaj.getChildren().addAll(nova, akcije);

        ScrollPane sp = new ScrollPane(sadrzaj);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(560);
        d.getDialogPane().setContent(sp);
        d.setResizable(true);
        d.showAndWait();
    }

    // ---------- izmjena podataka ----------

    /** Forma za izmjenu podataka studenta. Ako server odbije, forma ostaje otvorena. */
    private static void uredi(StudentDetaljiDTO s, Dialog<Void> detalji) {
        Dialog<Void> d = new Dialog<>();
        d.setTitle("Uredi podatke studenta");
        d.setHeaderText(s.ime() + " " + s.prezime());
        ButtonType sacuvajTip = new ButtonType("Sačuvaj", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(sacuvajTip, ButtonType.CANCEL);
        d.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(StudentDetaljiDijalog.class.getResource("/styles/sobe.css")).toExternalForm());

        TextField ime = polje(s.ime());
        TextField prezime = polje(s.prezime());
        TextField imeRoditelja = polje(s.imeRoditelja());
        TextField jmbg = polje(s.jmbg());
        TextField indeks = polje(s.brojIndeksa());
        indeks.setPromptText("prazno ako još nema indeks");

        List<String> fakulteti = new ArrayList<>(Fakulteti.SVI);   // ⚠ ista lista kao na registraciji
        fakulteti.add("Drugo");
        if (s.fakultet() != null && !fakulteti.contains(s.fakultet())) fakulteti.add(s.fakultet());
        ComboBox<String> fakultet = new ComboBox<>(javafx.collections.FXCollections.observableArrayList(fakulteti));
        fakultet.setValue(s.fakultet());
        fakultet.setMaxWidth(Double.MAX_VALUE);

        TextField email = polje(s.email());
        TextField telefon = polje(s.telefon());
        TextField adresa = polje(s.adresa());
        TextField licna = polje(s.brojLicneKarte());
        TextField telRoditelja = polje(s.telefonRoditelja());
        TextField kanton = polje(s.kanton());
        TextField opcina = polje(s.opcina());

        VBox sadrzaj = new VBox(12);
        sadrzaj.setPadding(new Insets(6, 4, 4, 4));
        sadrzaj.setPrefWidth(560);

        sadrzaj.getChildren().add(naslov("Lični podaci"));
        GridPane licni = mreza();
        unos(licni, "Ime", ime);
        unos(licni, "Prezime", prezime);
        unos(licni, "Ime roditelja", imeRoditelja);
        unos(licni, "JMBG", jmbg);
        unos(licni, "Lična karta", licna);
        sadrzaj.getChildren().add(licni);

        sadrzaj.getChildren().add(naslov("Studij"));
        GridPane studij = mreza();
        unos(studij, "Fakultet", fakultet);
        unos(studij, "Broj indeksa", indeks);
        sadrzaj.getChildren().add(studij);

        sadrzaj.getChildren().add(naslov("Kontakt i prebivalište"));
        GridPane kontakt = mreza();
        unos(kontakt, "E-mail", email);
        unos(kontakt, "Telefon", telefon);
        unos(kontakt, "Telefon roditelja", telRoditelja);
        unos(kontakt, "Adresa", adresa);
        unos(kontakt, "Kanton", kanton);
        unos(kontakt, "Općina", opcina);
        sadrzaj.getChildren().add(kontakt);

        Label napomena = new Label("E-mail je studentu ujedno i korisničko ime: nakon izmjene prijavljuje se novom adresom. "
                + "Godina studija, prosjek i ostali podaci prijave mijenjaju se u bodovanju prijave.");
        napomena.getStyleClass().add("sobe-podnaslov");
        napomena.setWrapText(true);
        sadrzaj.getChildren().add(napomena);

        ScrollPane sp = new ScrollPane(sadrzaj);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(520);
        d.getDialogPane().setContent(sp);
        d.setResizable(true);

        // "Sačuvaj" ne zatvara prozor sam: zatvara se tek kad server prihvati izmjene
        Button sacuvaj = (Button) d.getDialogPane().lookupButton(sacuvajTip);
        sacuvaj.addEventFilter(ActionEvent.ACTION, e -> {
            e.consume();
            StudentIzmjenaDTO z = new StudentIzmjenaDTO(
                    tekst(ime), tekst(prezime), tekst(imeRoditelja), tekst(jmbg), tekst(indeks),
                    fakultet.getValue(), tekst(email), tekst(telefon), tekst(adresa),
                    tekst(licna), tekst(telRoditelja), tekst(kanton), tekst(opcina));
            sacuvaj.setDisable(true);
            SobeController.pozovi(() -> SobeApi.izmijeniStudenta(s.idPrijava(), z), novi -> {
                d.close();
                detalji.close();
                prikazi(novi);   // ponovo otvori detalje s novim podacima
            }, () -> sacuvaj.setDisable(false));
        });

        d.showAndWait();
    }

    private static TextField polje(String v) {
        TextField t = new TextField(v != null ? v : "");
        t.setMaxWidth(Double.MAX_VALUE);
        return t;
    }

    private static String tekst(TextField t) {
        String v = t.getText();
        return v == null || v.isBlank() ? null : v.trim();
    }

    /** Red "naziv: polje za unos". */
    private static void unos(GridPane g, String naziv, Control polje) {
        int r = g.getRowCount();
        Label n = new Label(naziv);
        n.getStyleClass().add("sobe-oznaka-polja");
        GridPane.setHgrow(polje, Priority.ALWAYS);
        g.add(n, 0, r);
        g.add(polje, 1, r);
    }

    // ---------- pomoćne ----------

    private static GridPane mreza() {
        GridPane g = new GridPane();
        g.setHgap(16);
        g.setVgap(6);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(130);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c1, c2);
        return g;
    }

    /** Red "naziv: vrijednost"; kopiraj = link za kopiranje (telefon, email, JMBG). */
    private static void red(GridPane g, String naziv, String vrijednost, boolean kopiraj) {
        int r = g.getRowCount();
        Label n = new Label(naziv);
        n.getStyleClass().add("sobe-oznaka-polja");
        String v = vrijednost == null || vrijednost.isBlank() ? "—" : vrijednost;
        Label val = new Label(v);
        val.setWrapText(true);
        val.getStyleClass().add("detalji-ime");
        HBox h = new HBox(8, val);
        h.setAlignment(Pos.CENTER_LEFT);
        if (kopiraj && !"—".equals(v)) {
            Hyperlink k = new Hyperlink("kopiraj");
            k.setOnAction(e -> {
                ClipboardContent cc = new ClipboardContent();
                cc.putString(v);
                Clipboard.getSystemClipboard().setContent(cc);
                k.setText("kopirano");
            });
            h.getChildren().add(k);
        }
        g.add(n, 0, r);
        g.add(h, 1, r);
    }

    private static Label naslov(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("detalji-naslov");
        return l;
    }

    private static String statusTekst(String s) {
        if (s == null) return "";
        return switch (s) {
            case "PREDLOZEN" -> "prijedlog";
            case "DODIJELJEN" -> "dodijeljeno";
            case "USELJEN" -> "uselio";
            default -> s.toLowerCase();
        };
    }
}