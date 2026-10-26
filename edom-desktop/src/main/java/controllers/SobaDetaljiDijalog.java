package controllers;

import api.SobeApi;
import ba.unze.edom.common.dto.*;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Detalji jedne sobe: stanari (useli, iseli, premjesti, ukloni, oznaka),
 * dodavanje studenta bez sobe i soba van upotrebe.
 * Poslije svake izmjene dijalog se zatvori, pregled se osvježi i soba se ponovo otvori.
 */
final class SobaDetaljiDijalog {

    private SobaDetaljiDijalog() {}

    static void prikazi(SobaPregledDTO s, SobePregledDTO sve, int godina,
                        List<OznakaStanaraDTO> oznake, Runnable osvjezi) {

        Dialog<Void> d = new Dialog<>();
        d.setTitle("Soba " + s.broj());
        d.setHeaderText("Soba " + s.broj() + " · " + s.sprat() + " · " + s.kapacitet() + " kreveta"
                + (s.spol() != null ? " · " + ("M".equals(s.spol()) ? "muška" : "ženska") : "")
                + " · " + s.brojKljuceva() + " ključa");
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        d.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(SobaDetaljiDijalog.class.getResource("/styles/sobe.css")).toExternalForm());

        // akcija = poziv servera, pa zatvori dijalog i osvježi pregled
        Akcija akcija = poziv -> SobeController.pozovi(() -> { poziv.run(); return Boolean.TRUE; }, ok -> {
            d.close();
            osvjezi.run();
        });

        VBox sadrzaj = new VBox(14);
        sadrzaj.setPadding(new Insets(6, 4, 4, 4));
        sadrzaj.setPrefWidth(640);

        Label kategorija = new Label(s.kategorija() + (s.napomena() != null ? " · " + s.napomena() : ""));
        kategorija.getStyleClass().add("sobe-podnaslov");
        sadrzaj.getChildren().add(kategorija);

        // ---------- stanari ----------
        sadrzaj.getChildren().add(naslov("Stanari (" + s.zauzeto() + "/" + s.kapacitet() + ")"));
        if (s.stanari().isEmpty()) sadrzaj.getChildren().add(sitno("Soba je prazna."));
        for (StanarDTO st : s.stanari()) {
            sadrzaj.getChildren().add(stanar(st, s, sve, oznake, akcija));
        }

        // ---------- dodaj studenta ----------
        if (s.zaStudente() && s.aktivna() && s.slobodno() > 0) {
            sadrzaj.getChildren().add(naslov("Dodaj studenta"));
            ComboBox<StudentBezSobeDTO> cmb = new ComboBox<>();
            cmb.setPromptText("Učitavam odobrene bez sobe…");
            cmb.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(cmb, Priority.ALWAYS);
            cmb.setCellFactory(c -> studentCell());
            cmb.setButtonCell(studentCell());
            Button dodaj = new Button("Dodaj u sobu");
            dodaj.getStyleClass().add("sobe-btn-glavno");
            dodaj.setOnAction(e -> {
                StudentBezSobeDTO izbor = cmb.getValue();
                if (izbor == null) return;
                akcija.izvrsi(() -> SobeApi.dodijeli(izbor.idPrijava(), s.idSoba()));
            });
            Button detalji = new Button("Detalji");
            detalji.getStyleClass().add("sobe-btn");
            detalji.setOnAction(e -> {
                if (cmb.getValue() != null) StudentDetaljiDijalog.otvori(cmb.getValue().idPrijava());
            });
            HBox red = new HBox(10, cmb, detalji, dodaj);
            red.setAlignment(Pos.CENTER_LEFT);
            sadrzaj.getChildren().add(red);
            sadrzaj.getChildren().add(sitno("Prikazani su odobreni studenti bez sobe, po rang listi"
                    + (s.spol() != null ? ", samo " + ("M".equals(s.spol()) ? "muški" : "ženski") : "") + "."));

            SobeController.pozovi(() -> SobeApi.bezSobe(godina), lista -> {
                List<StudentBezSobeDTO> zaSobu = lista.stream()
                        .filter(b -> s.spol() == null || Objects.equals(s.spol(), b.spol()))
                        .toList();
                cmb.setItems(FXCollections.observableArrayList(zaSobu));
                cmb.setPromptText(zaSobu.isEmpty() ? "Nema odobrenih bez sobe" : "Odaberite studenta");
            });
        }

        // ---------- soba u upotrebi ----------
        if (s.zaStudente()) {
            sadrzaj.getChildren().add(naslov("Stanje sobe"));
            CheckBox aktivna = new CheckBox("Soba je u upotrebi");
            aktivna.setSelected(s.aktivna());
            TextField napomena = new TextField(s.napomena() != null ? s.napomena() : "");
            napomena.setPromptText("Napomena (npr. kvar na grijanju)");
            HBox.setHgrow(napomena, Priority.ALWAYS);
            Button sacuvaj = new Button("Sačuvaj");
            sacuvaj.getStyleClass().add("sobe-btn");
            sacuvaj.setOnAction(e -> {
                if (!aktivna.isSelected() && s.zauzeto() > 0
                        && !SobeController.potvrda("Soba ima stanare",
                        "Soba ostaje van upotrebe, ali stanare treba premjestiti ručno. Nastaviti?")) {
                    return;
                }
                akcija.izvrsi(() -> SobeApi.postaviAktivna(s.idSoba(), aktivna.isSelected(), napomena.getText()));
            });
            HBox red = new HBox(10, aktivna, napomena, sacuvaj);
            red.setAlignment(Pos.CENTER_LEFT);
            sadrzaj.getChildren().add(red);
        }

        ScrollPane sp = new ScrollPane(sadrzaj);
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(Math.min(560, 160 + s.stanari().size() * 110 + 160));
        d.getDialogPane().setContent(sp);
        d.setResizable(true);
        d.showAndWait();
    }

    /** Jedan stanar s akcijama. */
    private static VBox stanar(StanarDTO st, SobaPregledDTO s, SobePregledDTO sve,
                               List<OznakaStanaraDTO> oznake, Akcija akcija) {
        Hyperlink ime = new Hyperlink(st.imePrezime());
        ime.getStyleClass().add("detalji-ime");
        ime.setOnAction(e -> StudentDetaljiDijalog.otvori(st.idPrijava()));
        Label status = new Label(statusTekst(st.status()));
        status.getStyleClass().addAll("detalji-status", "status-" + st.status().toLowerCase());
        Region razmak = new Region();
        HBox.setHgrow(razmak, Priority.ALWAYS);
        Label info = new Label(SobeController.godinaTekst(st.godinaStudija())
                + (st.rang() != null ? " · " + st.rang() + ". na rang listi" : ""));
        info.getStyleClass().add("sobe-podnaslov");
        HBox glava = new HBox(10, ime, status, razmak, info);
        glava.setAlignment(Pos.CENTER_LEFT);

        VBox k = new VBox(8, glava);
        k.getStyleClass().add("detalji-stanar");
        if (st.razlog() != null) k.getChildren().add(sitno(st.razlog()));
        if ("USELJEN".equals(st.status())) k.getChildren().add(sitno("Preuzeto ključeva: " + st.kljuceviPreuzeti()));

        // ---- dugmad ----
        List<javafx.scene.Node> dugmad = new ArrayList<>();

        if (!"USELJEN".equals(st.status())) {
            Button useli = new Button("Useli");
            useli.getStyleClass().add("sobe-btn-glavno");
            useli.setOnAction(e -> pitajBroj("Useljenje", st.imePrezime() + " preuzima ključeve.",
                    "Broj preuzetih ključeva:", 1)
                    .ifPresent(n -> akcija.izvrsi(() -> SobeApi.useli(st.idSmjestaj(), n))));
            dugmad.add(useli);
        } else {
            Button iseli = new Button("Iseli");
            iseli.getStyleClass().add("sobe-btn");
            iseli.setOnAction(e -> pitajBroj("Iseljenje", st.imePrezime() + " vraća ključeve i više nije u domu "
                            + "(prijava dobija status \"iseljen\"). Napomenu studentu upišite klikom na ime.",
                    "Broj vraćenih ključeva:", st.kljuceviPreuzeti())
                    .ifPresent(n -> akcija.izvrsi(() -> {
                        int fali = SobeApi.iseli(st.idSmjestaj(), n);
                        if (fali > 0) {
                            javafx.application.Platform.runLater(() ->
                                    SobeController.info("Pažnja: nije vraćeno " + fali + " ključ(a)."));
                        }
                    })));
            dugmad.add(iseli);
        }

        Button premjesti = new Button("Premjesti…");
        premjesti.getStyleClass().add("sobe-btn");
        premjesti.setOnAction(e -> {
            List<SobaPregledDTO> slobodne = sve.sobe().stream()
                    .filter(x -> x.zaStudente() && x.aktivna() && x.slobodno() > 0)
                    .filter(x -> !x.idSoba().equals(s.idSoba()))
                    .filter(x -> x.spol() == null || st.spol() == null || x.spol().equals(st.spol()))
                    .toList();
            if (slobodne.isEmpty()) {
                SobeController.info("Nema druge sobe sa slobodnim mjestom za ovog studenta.");
                return;
            }
            ChoiceDialog<SobaIzbor> cd = new ChoiceDialog<>(null,
                    slobodne.stream().map(SobaIzbor::new).toList());
            cd.setTitle("Premještaj");
            cd.setHeaderText("Premjestiti " + st.imePrezime() + " u sobu:");
            cd.showAndWait().ifPresent(izbor ->
                    akcija.izvrsi(() -> SobeApi.dodijeli(st.idPrijava(), izbor.soba().idSoba())));
        });
        dugmad.add(premjesti);

        if (!"USELJEN".equals(st.status())) {
            Button ukloni = new Button("Ukloni iz sobe");
            ukloni.getStyleClass().add("sobe-btn-opasno");
            ukloni.setOnAction(e -> {
                if (SobeController.potvrda("Ukloniti iz sobe?", st.imePrezime() + " ostaje odobren, ali bez sobe.")) {
                    akcija.izvrsi(() -> SobeApi.ukloni(st.idSmjestaj()));
                }
            });
            dugmad.add(ukloni);
        }

        Region r2 = new Region();
        HBox.setHgrow(r2, Priority.ALWAYS);
        dugmad.add(r2);

        // ---- oznaka (boja imena) ----
        ComboBox<OznakaStanaraDTO> oznaka = new ComboBox<>();
        List<OznakaStanaraDTO> izbor = new ArrayList<>();
        izbor.add(new OznakaStanaraDTO(null, "bez oznake", null));
        izbor.addAll(oznake);
        oznaka.setItems(FXCollections.observableArrayList(izbor));
        oznaka.setCellFactory(c -> oznakaCell());
        oznaka.setButtonCell(oznakaCell());
        izbor.stream().filter(o -> Objects.equals(o.id(), st.idOznaka())).findFirst().ifPresent(oznaka::setValue);
        oznaka.valueProperty().addListener((o, a, b) -> {
            if (b != null && !Objects.equals(b.id(), st.idOznaka())) {
                akcija.izvrsi(() -> SobeApi.postaviOznaku(st.idSmjestaj(), b.id()));
            }
        });
        dugmad.add(oznaka);

        HBox red = new HBox(8);
        red.getChildren().addAll(dugmad);
        red.setAlignment(Pos.CENTER_LEFT);
        k.getChildren().add(red);
        return k;
    }

    // ---------- pomoćne ----------

    @FunctionalInterface
    interface Akcija {
        void izvrsi(Runnable poziv);
    }

    /** Za ChoiceDialog: lijep tekst sobe. */
    private record SobaIzbor(SobaPregledDTO soba) {
        @Override
        public String toString() {
            return "Soba " + soba.broj() + " · " + soba.sprat() + " · slobodno " + soba.slobodno() + "/" + soba.kapacitet();
        }
    }

    private static Optional<Integer> pitajBroj(String naslov, String zaglavlje, String pitanje, int pocetno) {
        TextInputDialog t = new TextInputDialog(String.valueOf(pocetno));
        t.setTitle(naslov);
        t.setHeaderText(zaglavlje);
        t.setContentText(pitanje);
        Optional<String> odg = t.showAndWait();
        if (odg.isEmpty()) return Optional.empty();
        try {
            return Optional.of(Math.max(0, Integer.parseInt(odg.get().trim())));
        } catch (NumberFormatException e) {
            SobeController.info("Unesite broj.");
            return Optional.empty();
        }
    }

    private static String statusTekst(String s) {
        return switch (s) {
            case "PREDLOZEN" -> "prijedlog";
            case "DODIJELJEN" -> "dodijeljeno";
            case "USELJEN" -> "uselio";
            default -> s.toLowerCase();
        };
    }

    private static Label naslov(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("detalji-naslov");
        return l;
    }

    private static Label sitno(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("sobe-podnaslov");
        l.setWrapText(true);
        return l;
    }

    private static ListCell<StudentBezSobeDTO> studentCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(StudentBezSobeDTO b, boolean empty) {
                super.updateItem(b, empty);
                setText(empty || b == null ? "" : b.rang() + ". " + b.imePrezime()
                        + (b.brojIndeksa() != null ? " · indeks " + b.brojIndeksa() : "")
                        + (b.email() != null ? " · " + b.email() : "")
                        + " · " + SobeController.godinaTekst(b.godinaStudija())
                        + (b.zelja() != null ? " · želja: " + b.zelja() : ""));
            }
        };
    }

    private static ListCell<OznakaStanaraDTO> oznakaCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(OznakaStanaraDTO o, boolean empty) {
                super.updateItem(o, empty);
                if (empty || o == null) {
                    setText("");
                    setGraphic(null);
                    return;
                }
                setText(o.naziv());
                if (o.boja() != null) {
                    Region r = new Region();
                    r.setMinSize(12, 12);
                    r.setMaxSize(12, 12);
                    r.setStyle("-fx-background-color: " + o.boja() + "; -fx-background-radius: 6;");
                    setGraphic(r);
                } else {
                    setGraphic(null);
                }
            }
        };
    }
}