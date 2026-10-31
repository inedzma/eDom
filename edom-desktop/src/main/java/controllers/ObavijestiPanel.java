package controllers;

import ba.unze.edom.common.dto.opste.ObavijestDTO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Lista obavijesti grupisana po vrsti; klik na karticu → naKlik. */
public class ObavijestiPanel extends ScrollPane {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

    /** Naslovi grupa; nepoznate vrste (budući zahtjevi) idu na kraj pod "Ostalo". */
    private static final Map<String, String> GRUPE = new LinkedHashMap<>();
    static {
        GRUPE.put("NOVA_PRIJAVA", "Nove prijave za pregled");
        GRUPE.put("ZAHTJEV_SOBA", "Zahtjevi za promjenu sobe");
        GRUPE.put("BEZ_SOBE", "Smještaj");
        GRUPE.put("PORUKA", "Poruke studenata");
    }

    public ObavijestiPanel(List<ObavijestDTO> stavke, Set<String> vidjene, Consumer<ObavijestDTO> naKlik) {
        VBox sadrzaj = new VBox(12);
        sadrzaj.getStyleClass().add("obav-root");
        sadrzaj.setPadding(new Insets(24));

        Label naslov = new Label("Obavijesti");
        naslov.getStyleClass().add("obav-naslov");
        sadrzaj.getChildren().add(naslov);

        if (stavke.isEmpty()) {
            Label prazno = new Label("Nema obavijesti — sve je obrađeno.");
            prazno.getStyleClass().add("obav-prazno");
            sadrzaj.getChildren().add(prazno);
        }

        Map<String, List<ObavijestDTO>> poVrsti = stavke.stream().collect(
                Collectors.groupingBy(ObavijestDTO::vrsta, LinkedHashMap::new, Collectors.toList()));

        List<String> redoslijed = new ArrayList<>(GRUPE.keySet());
        poVrsti.keySet().stream().filter(v -> !redoslijed.contains(v)).forEach(redoslijed::add);

        for (String vrsta : redoslijed) {
            List<ObavijestDTO> grupa = poVrsti.get(vrsta);
            if (grupa == null) continue;

            Label g = new Label(GRUPE.getOrDefault(vrsta, "Ostalo") + " (" + grupa.size() + ")");
            g.getStyleClass().add("obav-grupa");
            sadrzaj.getChildren().add(g);

            for (ObavijestDTO o : grupa) {
                sadrzaj.getChildren().add(kartica(o, !vidjene.contains(o.kljuc()), naKlik));
            }
        }

        setContent(sadrzaj);
        setFitToWidth(true);
        getStyleClass().add("obav-scroll");
    }

    private Node kartica(ObavijestDTO o, boolean nova, Consumer<ObavijestDTO> naKlik) {
        Label naslov = new Label(o.naslov());
        naslov.getStyleClass().add("obav-stavka-naslov");
        HBox red1 = new HBox(8, naslov);
        red1.setAlignment(Pos.CENTER_LEFT);
        if (nova) {
            Label n = new Label("NOVO");
            n.getStyleClass().add("obav-novo");
            red1.getChildren().add(n);
        }

        VBox tekst = new VBox(4, red1);
        if (o.opis() != null) {
            Label opis = new Label(o.opis());
            opis.setWrapText(true);
            opis.getStyleClass().add("obav-opis");
            tekst.getChildren().add(opis);
        }
        if (o.upozorenje() != null) {
            Label u = new Label("⚠ " + o.upozorenje());
            u.setWrapText(true);
            u.getStyleClass().add("obav-upozorenje");
            tekst.getChildren().add(u);
        }
        HBox.setHgrow(tekst, Priority.ALWAYS);

        Label datum = new Label(o.datum() != null ? o.datum().format(DATUM) : "");
        datum.getStyleClass().add("obav-datum");
        Label strelica = new Label("›");
        strelica.getStyleClass().add("obav-strelica");

        HBox k = new HBox(14, tekst, datum, strelica);
        k.setAlignment(Pos.CENTER_LEFT);
        k.getStyleClass().add("obav-kartica");
        if (o.upozorenje() != null) k.getStyleClass().add("obav-kartica--upozorenje");
        k.setCursor(Cursor.HAND);
        k.setOnMouseClicked(e -> naKlik.accept(o));
        return k;
    }
}