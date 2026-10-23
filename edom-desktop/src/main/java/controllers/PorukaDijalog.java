package controllers;

import api.ApiException;
import api.PorukeApi;
import ba.unze.edom.common.dto.PorukaDTO;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/** Prikaz jedne poruke studenta: odgovori mailom ili označi kao pročitano. */
public class PorukaDijalog {

    private final String id;
    private final Runnable poslije;

    public PorukaDijalog(String id, Runnable poslije) {
        this.id = id;
        this.poslije = poslije;
    }

    public void prikazi() {
        uPozadini(() -> PorukeApi.nepročitane().stream()
                        .filter(p -> p.id().equals(id)).findFirst().orElse(null),
                p -> {
                    if (p == null) {
                        new Alert(Alert.AlertType.INFORMATION, "Poruka je već označena kao pročitana.").showAndWait();
                        poslije.run();
                    } else {
                        dijalog(p);
                    }
                });
    }

    private void dijalog(PorukaDTO p) {
        TextArea tekst = new TextArea(p.tekst());
        tekst.setEditable(false);
        tekst.setWrapText(true);
        tekst.setPrefRowCount(10);

        Label od = new Label("Od: " + p.ime() + " (" + p.sifra() + ")\nE-mail: " + p.email()
                + "\nPoslano: " + p.vrijeme());

        VBox sadrzaj = new VBox(10, od, tekst);
        sadrzaj.setPadding(new Insets(10));
        sadrzaj.setPrefWidth(520);

        ButtonType odgovori = new ButtonType("Odgovori mailom", ButtonBar.ButtonData.LEFT);
        ButtonType procitano = new ButtonType("Pročitano", ButtonBar.ButtonData.OK_DONE);

        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Poruka studenta");
        d.setHeaderText(p.naslov());
        d.getDialogPane().setContent(sadrzaj);
        d.getDialogPane().getButtonTypes().addAll(odgovori, procitano, ButtonType.CLOSE);

        // "Odgovori mailom" ne zatvara dijalog
        ((Button) d.getDialogPane().lookupButton(odgovori)).addEventFilter(
                javafx.event.ActionEvent.ACTION, e -> {
                    e.consume();
                    otvoriMail(p);
                });

        if (d.showAndWait().orElse(ButtonType.CLOSE) == procitano) {
            uPozadini(() -> { PorukeApi.procitano(p.id()); return true; }, ok -> poslije.run());
        }
    }

    private void otvoriMail(PorukaDTO p) {
        try {
            String subject = URLEncoder.encode("Re: " + p.naslov(), StandardCharsets.UTF_8).replace("+", "%20");
            Desktop.getDesktop().mail(new URI("mailto:" + p.email() + "?subject=" + subject));
        } catch (Exception e) {
            new Alert(Alert.AlertType.WARNING,
                    "Mail program se ne može otvoriti. Odgovorite na: " + p.email()).showAndWait();
        }
    }

    private <T> void uPozadini(Supplier<T> posao, java.util.function.Consumer<T> uspjeh) {
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
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}