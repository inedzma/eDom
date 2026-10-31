package controllers;

import api.ApiException;
import api.PrijavaApi;
import ba.unze.edom.common.dto.dokumenti.DokumentDTO;
import ba.unze.edom.common.dto.dokumenti.DokumentiSpisakDTO;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import service.DokumentPregled;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Spisak dokumenata kao na webu; admin označava dostavljene i dodaje papirne. */
public class DokumentiPanel extends VBox {

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

    private final int idPrijave;
    private final Runnable osvjezi;

    public DokumentiPanel(int idPrijave, DokumentiSpisakDTO s, Runnable osvjezi) {
        super(10);
        this.idPrijave = idPrijave;
        this.osvjezi = osvjezi;
        setPadding(new Insets(16));
        getStyleClass().add("pr-kartica");

        Label n = new Label("Dokumenti");
        n.getStyleClass().add("pr-kartica-naslov");
        getChildren().addAll(n, sitno("Označite „Dostavljen“ kad student dostavi dokument (original ili ovjerenu kopiju). "
                + "Ako je donio papir koji nije priložio online, dodajte ga dugmetom „+ Dostavljen papirno“."));

        grupa("Obavezni dokumenti", s.obavezni());
        grupa("Dokumenti za dodatne kriterije", s.dodatni());
        if (s.dodatni().isEmpty()) getChildren().add(sitno("Student nema označenih kriterija koji traže poseban dokument."));
        if (!s.kriterijiBezDokaza().isEmpty()) {
            getChildren().add(sitno("Bez posebnog dokumenta: " + String.join(", ", s.kriterijiBezDokaza())));
        }
        if (!s.ostali().isEmpty()) {
            getChildren().add(podnaslov("Ostali priloženi dokumenti"));
            for (DokumentDTO d : s.ostali()) getChildren().add(dokument(d));
        }
    }

    private void grupa(String naslov, List<DokumentiSpisakDTO.Zahtjev> lista) {
        if (lista.isEmpty()) return;
        getChildren().add(podnaslov(naslov));
        for (DokumentiSpisakDTO.Zahtjev z : lista) getChildren().add(zahtjev(z));
    }

    private Node zahtjev(DokumentiSpisakDTO.Zahtjev z) {
        long dostavljeno = z.prilozeni().stream().filter(d -> Boolean.TRUE.equals(d.dostavljen())).count();
        int potrebno = Math.max(1, z.potrebno());
        boolean ok = dostavljeno >= potrebno;

        Label naziv = new Label(z.naziv() + (z.poClanu() ? "  (za " + z.potrebno() + " član/a)" : ""));
        naziv.setStyle("-fx-font-weight: bold;");
        naziv.setWrapText(true);

        String tekst = z.poClanu()
                ? "Dostavljeno " + dostavljeno + " / " + potrebno
                : ok ? "Dostavljeno" : z.prilozeni().isEmpty() ? "Nije priloženo" : "Priloženo, nije dostavljeno";
        Label status = new Label(tekst);
        status.setStyle("-fx-padding: 2 10; -fx-background-radius: 10; -fx-font-weight: bold; "
                + (ok ? "-fx-background-color: #e7f6ef; -fx-text-fill: #1e7a4f;"
                : z.prilozeni().isEmpty() ? "-fx-background-color: #fdecea; -fx-text-fill: #b3261e;"
                : "-fx-background-color: #fff4e5; -fx-text-fill: #b86e00;"));

        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        HBox glava = new HBox(10, naziv, r, status);
        glava.setAlignment(Pos.CENTER_LEFT);

        VBox k = new VBox(6, glava);
        if (z.opis() != null && !z.opis().isBlank()) k.getChildren().add(sitno(z.opis()));
        for (DokumentDTO d : z.prilozeni()) k.getChildren().add(dokument(d));

        Button papir = new Button("+ Dostavljen papirno");
        papir.getStyleClass().add("pr-mali-btn");
        papir.setOnAction(e -> dodajPapir(z));
        k.getChildren().add(papir);

        k.setPadding(new Insets(10, 12, 10, 12));
        k.setStyle("-fx-background-color: #f8fbfb; -fx-background-radius: 10; "
                + "-fx-border-color: #e6eef0; -fx-border-radius: 10;");
        return k;
    }

    private Node dokument(DokumentDTO d) {
        Label naziv = new Label(d.naziv() + (d.vrsta() != null && !d.vrsta().equals(d.naziv()) ? "  ·  " + d.vrsta() : ""));
        naziv.setWrapText(true);
        Label info = sitno((d.datumUpload() != null ? d.datumUpload().format(D) + "  ·  " : "")
                + (d.imaSadrzaj() ? "priloženo online" : "papir"));

        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        HBox red = new HBox(10, new VBox(2, naziv, info), r);
        red.setAlignment(Pos.CENTER_LEFT);

        if (d.imaSadrzaj()) {
            Button pregled = new Button("Pregledaj");
            pregled.getStyleClass().add("pr-mali-btn");
            pregled.setOnAction(e -> pozadina(() -> PrijavaApi.sadrzajDokumenta(idPrijave, d.idDokument()),
                    b64 -> DokumentPregled.prikazi(b64, d.naziv())));
            red.getChildren().add(pregled);
        } else {
            Button obrisi = new Button("Obriši");
            obrisi.getStyleClass().addAll("pr-mali-btn", "pr-mali-btn--crveno");
            obrisi.setOnAction(e -> {
                Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Obrisati papirni dokument „" + d.naziv() + "“?",
                        ButtonType.YES, ButtonType.NO);
                a.setHeaderText(null);
                if (a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
                    pozadina(() -> { PrijavaApi.obrisiPapir(idPrijave, d.idDokument()); return true; }, x -> osvjezi.run());
                }
            });
            red.getChildren().add(obrisi);
        }

        CheckBox dostavljen = new CheckBox("Dostavljen");
        dostavljen.setSelected(Boolean.TRUE.equals(d.dostavljen()));
        dostavljen.setOnAction(e -> pozadina(() -> {
            PrijavaApi.dostavljen(idPrijave, d.idDokument(), dostavljen.isSelected());
            return true;
        }, x -> osvjezi.run()));
        red.getChildren().add(dostavljen);
        return red;
    }

    private void dodajPapir(DokumentiSpisakDTO.Zahtjev z) {
        if (z.vrste().isEmpty()) return;

        DokumentiSpisakDTO.Vrsta vrsta = z.vrste().get(0);
        if (z.vrste().size() > 1) {
            List<String> nazivi = z.vrste().stream().map(DokumentiSpisakDTO.Vrsta::naziv).toList();
            ChoiceDialog<String> cd = new ChoiceDialog<>(nazivi.get(0), nazivi);
            cd.setTitle("Dostavljen papirno");
            cd.setHeaderText("Koji dokument je student donio?");
            Optional<String> izbor = cd.showAndWait();
            if (izbor.isEmpty()) return;
            vrsta = z.vrste().stream().filter(v -> v.naziv().equals(izbor.get())).findFirst().orElse(vrsta);
        }

        String predlog = z.poClanu() ? "Član " + (z.prilozeni().size() + 1) + " – " + vrsta.naziv() : vrsta.naziv();
        TextInputDialog t = new TextInputDialog(predlog);
        t.setTitle("Dostavljen papirno");
        t.setHeaderText("Naziv dokumenta (npr. kojem članu pripada):");
        Optional<String> naziv = t.showAndWait();
        if (naziv.isEmpty()) return;

        int idVrste = vrsta.id();
        pozadina(() -> { PrijavaApi.dodajPapir(idPrijave, idVrste, naziv.get()); return true; }, x -> osvjezi.run());
    }

    // ---------- pomoćne ----------

    private <T> void pozadina(Supplier<T> posao, Consumer<T> uspjeh) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return posao.get();
            }
        };
        task.setOnSucceeded(e -> uspjeh.accept(task.getValue()));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (!(ex instanceof ApiException)) ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    ex instanceof ApiException ? ex.getMessage() : "Greška u komunikaciji sa serverom.").showAndWait();
            osvjezi.run();
        });
        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private static Label podnaslov(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0b6b5c; -fx-padding: 6 0 0 0;");
        return l;
    }

    private static Label sitno(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-text-fill: #6b7a90;");
        l.setWrapText(true);
        return l;
    }
}