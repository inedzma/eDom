package controllers;

import api.SobeApi;
import ba.unze.edom.common.dto.sobe.ZahtjevSobeDTO;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Zahtjevi studenata za promjenu sobe.
 * Admin odobri ili odbije i upiše odgovor; premještanje radi ručno u detaljima sobe.
 */
final class ZahtjeviDijalog {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy. HH:mm");

    private ZahtjeviDijalog() {}

    static void prikazi(int godina, Runnable poslije) {
        TableView<ZahtjevSobeDTO> t = new TableView<>();
        t.getColumns().add(SobeController.kolona("Podnesen", 120,
                z -> z.datum() != null ? z.datum().format(DATUM) : ""));
        t.getColumns().add(SobeController.kolona("Student", 170, ZahtjevSobeDTO::imePrezime));
        t.getColumns().add(SobeController.kolona("Soba", 55,
                z -> z.trenutnaSoba() != null ? String.valueOf(z.trenutnaSoba()) : "—"));
        t.getColumns().add(SobeController.kolona("Traži", 230, ZahtjeviDijalog::opis));
        t.getColumns().add(SobeController.kolona("Drugi student podnio isti zahtjev", 120, ZahtjeviDijalog::drugi));
        t.getColumns().add(SobeController.kolona("Razlog", 260, ZahtjevSobeDTO::razlog));
        t.getColumns().add(SobeController.kolona("Status", 90, z -> statusTekst(z.status())));
        t.getColumns().add(SobeController.kolona("Odgovor", 180, z -> z.odgovor() != null ? z.odgovor() : ""));
        t.setPrefSize(1180, 480);
        t.setPlaceholder(new Label("Nema zahtjeva."));

        CheckBox samoNaCekanju = new CheckBox("Samo na čekanju");
        samoNaCekanju.setSelected(true);

        Label napomena = new Label("Odobravanje samo mijenja status zahtjeva. Studenta premjestite ručno: "
                + "klik na sobu → Premjesti. Kod zamjene provjerite da je i drugi student podnio zahtjev.");
        napomena.setWrapText(true);
        napomena.getStyleClass().add("sobe-podnaslov");

        Button odobri = new Button("Odobri");
        odobri.getStyleClass().add("sobe-btn-glavno");
        Button odbij = new Button("Odbij");
        odbij.getStyleClass().add("sobe-btn-opasno");
        Region razmak = new Region();
        HBox.setHgrow(razmak, Priority.ALWAYS);
        HBox traka = new HBox(10, samoNaCekanju, razmak, odobri, odbij);
        traka.setAlignment(Pos.CENTER_LEFT);

        Runnable[] ucitaj = new Runnable[1];
        ucitaj[0] = () -> SobeController.pozovi(() -> SobeApi.zahtjevi(godina), lista -> {
            List<ZahtjevSobeDTO> prikaz = samoNaCekanju.isSelected()
                    ? lista.stream().filter(z -> "NA_CEKANJU".equals(z.status())).toList()
                    : lista;
            t.setItems(FXCollections.observableArrayList(prikaz));
        });
        samoNaCekanju.selectedProperty().addListener((o, a, b) -> ucitaj[0].run());

        odobri.setOnAction(e -> odgovori(t, "ODOBREN", ucitaj[0]));
        odbij.setOnAction(e -> odgovori(t, "ODBIJEN", ucitaj[0]));

        Dialog<Void> d = new Dialog<>();
        d.setTitle("Zahtjevi za promjenu sobe");
        d.setHeaderText("Zahtjevi za promjenu sobe · " + godina + "/" + (godina + 1));
        d.getDialogPane().setContent(new VBox(10, napomena, traka, t));
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        d.getDialogPane().getStylesheets().add(
                ZahtjeviDijalog.class.getResource("/styles/sobe.css").toExternalForm());
        d.setResizable(true);

        ucitaj[0].run();
        d.showAndWait();
        poslije.run();
    }

    private static void odgovori(TableView<ZahtjevSobeDTO> t, String status, Runnable osvjezi) {
        ZahtjevSobeDTO z = t.getSelectionModel().getSelectedItem();
        if (z == null) {
            SobeController.info("Odaberite zahtjev u tabeli.");
            return;
        }
        if (!"NA_CEKANJU".equals(z.status())) {
            SobeController.info("Ovaj zahtjev je već riješen.");
            return;
        }
        TextInputDialog td = new TextInputDialog();
        td.setTitle("ODOBREN".equals(status) ? "Odobri zahtjev" : "Odbij zahtjev");
        td.setHeaderText(z.imePrezime() + ": " + opis(z));
        td.setContentText("Odgovor studentu (vidi ga na svojoj stranici):");
        Optional<String> odg = td.showAndWait();
        if (odg.isEmpty()) return;

        SobeController.pozovi(() -> {
            SobeApi.odgovoriNaZahtjev(z.id(), status, odg.get());
            return Boolean.TRUE;
        }, ok -> {
            osvjezi.run();
            if ("ODOBREN".equals(status)) {
                SobeController.info("Zahtjev je odobren. Sada studenta premjestite ručno: klik na sobu "
                        + (z.trenutnaSoba() != null ? z.trenutnaSoba() + " " : "") + "→ Premjesti.");
            }
        });
    }

    private static String opis(ZahtjevSobeDTO z) {
        if ("ZAMJENA".equals(z.vrsta())) {
            String ko = z.saStudentomIme() != null ? z.saStudentomIme() : z.saStudentom();
            return "Zamjena sa: " + ko + (z.saStudentomSoba() != null ? " (soba " + z.saStudentomSoba() + ")" : "");
        }
        return "Premještaj" + (z.zeljenaSoba() != null ? " u sobu " + z.zeljenaSoba() : " u drugu sobu");
    }

    private static String drugi(ZahtjevSobeDTO z) {
        if (!"ZAMJENA".equals(z.vrsta())) return "";
        if (z.drugiPodnio() == null) return "student nije pronađen";
        return z.drugiPodnio() ? "da" : "ne";
    }

    private static String statusTekst(String s) {
        return switch (s) {
            case "NA_CEKANJU" -> "na čekanju";
            case "ODOBREN" -> "odobren";
            case "ODBIJEN" -> "odbijen";
            case "POVUCEN" -> "povučen";
            default -> s;
        };
    }
}