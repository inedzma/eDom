package controllers;

import api.ApiException;
import api.PostavkeApi;
import ba.unze.edom.common.dto.KriterijPostavkaDTO;
import ba.unze.edom.common.dto.KriterijiPostavkeDTO;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/** Postavke → Kriteriji bodovanja: pravila (uspjeh, udaljenost, primanja) + tabela kriterija. */
public class KriterijiTab extends VBox {

    private static final Map<String, String> GRUPE = new LinkedHashMap<>();
    static {
        GRUPE.put("BRANIOCI", "Branioci i socijalni – boduje se samo najveći");
        GRUPE.put("DODATNI", "Dodatni – sabiraju se");
    }
    private static final KriterijiPostavkeDTO.Dokument BEZ_DOKAZA =
            new KriterijiPostavkeDTO.Dokument(null, "— bez posebnog dokumenta —");

    private final TableView<KriterijPostavkaDTO> tabela = new TableView<>();
    private final Button btnAktivan = new Button("Deaktiviraj");
    private List<KriterijiPostavkeDTO.Dokument> dokumenti = List.of();

    public KriterijiTab(Node pravila) {
        super(16);

        // ---------- kartica: pravila ----------
        VBox kPravila = new VBox(10,
                naslov("Uspjeh, udaljenost i primanja"),
                sitno("Formule za osnovne bodove. Mijenjaju se dugmetom „Sačuvaj postavke“ gore; "
                        + "neispravan unos server odbija i kaže šta ne valja."),
                new Separator());
        if (pravila != null) kPravila.getChildren().add(pravila);
        kPravila.getStyleClass().add("pr-kartica");

        // ---------- kartica: kriteriji ----------
        Label upozorenje = new Label("⚠  Promjena bodova odmah mijenja bodove svih prijava s tim kriterijem. "
                + "Objavljena rang lista ostaje ista dok je ponovo ne objavite. Kriterij koji više ne vrijedi deaktivirajte, ne brišite.");
        upozorenje.setWrapText(true);
        upozorenje.setMaxWidth(Double.MAX_VALUE);
        upozorenje.getStyleClass().add("pr-upozorenje");

        kolone();
        tabela.getStyleClass().addAll("pr-tabela", "pr-gusta");
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.setPlaceholder(new Label("Nema kriterija."));
        tabela.setPrefHeight(440);
        tabela.setRowFactory(t -> {
            TableRow<KriterijPostavkaDTO> r = new TableRow<>() {
                @Override
                protected void updateItem(KriterijPostavkaDTO k, boolean empty) {
                    super.updateItem(k, empty);
                    setStyle(!empty && k != null && !k.aktivan() ? "-fx-opacity: 0.55;" : "");
                }
            };
            r.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !r.isEmpty()) uredi(r.getItem());
            });
            return r;
        });

        Button btnDodaj = new Button("+  Dodaj kriterij");
        btnDodaj.getStyleClass().add("pr-btn-primarno");
        btnDodaj.setOnAction(e -> uredi(null));
        Button btnUredi = new Button("Uredi");
        btnUredi.getStyleClass().add("pr-btn-sekundarno");
        btnUredi.setOnAction(e -> uredi(odabrani()));
        btnAktivan.getStyleClass().add("pr-btn-sekundarno");
        btnAktivan.setOnAction(e -> promijeniAktivan());
        Button btnObrisi = new Button("Obriši");
        btnObrisi.getStyleClass().add("pr-btn-opasno");
        btnObrisi.setOnAction(e -> obrisi());

        for (Button b : List.of(btnUredi, btnAktivan, btnObrisi)) {
            b.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull());
        }
        tabela.getSelectionModel().selectedItemProperty().addListener((o, a, k) ->
                btnAktivan.setText(k != null && !k.aktivan() ? "Aktiviraj" : "Deaktiviraj"));

        Region razmak = new Region();
        HBox.setHgrow(razmak, Priority.ALWAYS);
        HBox alati = new HBox(8, btnDodaj, btnUredi, btnAktivan, btnObrisi, razmak,
                sitno("Dvoklik na red otvara uređivanje"));
        alati.setAlignment(Pos.CENTER_LEFT);

        VBox kKriteriji = new VBox(12,
                naslov("Branioci, socijalni i dodatni kriteriji"),
                sitno("Kriteriji koje student označava u prijavi. Izmjene u ovoj tabeli snimaju se odmah, "
                        + "bez dugmeta „Sačuvaj postavke“."),
                upozorenje, alati, tabela);
        kKriteriji.getStyleClass().add("pr-kartica");

        getChildren().addAll(kPravila, kKriteriji);
        izvrsi(PostavkeApi::kriteriji, null);
    }

    // =====================================================================
    //  TABELA
    // =====================================================================

    private void kolone() {
        TableColumn<KriterijPostavkaDTO, String> naziv = kolona("NAZIV", 280, KriterijPostavkaDTO::naziv);
        naziv.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty ? null : v);
                setStyle(empty ? "" : "-fx-font-weight: 700;");
            }
        });

        TableColumn<KriterijPostavkaDTO, String> grupa = kolona("GRUPA", 150,
                k -> "BRANIOCI".equals(k.grupa()) ? "Branioci (najveći)" : "Dodatni (zbir)");
        grupa.setCellFactory(c -> pilula(t -> t.startsWith("Branioci") ? "st-zakljuceno" : "st-pregled"));

        TableColumn<KriterijPostavkaDTO, String> bodovi = kolona("BODOVI", 90, k -> k.bodovi() != null
                ? k.bodovi().stripTrailingZeros().toPlainString() : (k.racunaSe() ? "formula" : "—"));
        bodovi.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty ? null : v);
                setStyle(empty ? "" : "formula".equals(v)
                        ? "-fx-font-style: italic; -fx-text-fill: #6b8290;" : "-fx-font-weight: 800;");
            }
        });

        TableColumn<KriterijPostavkaDTO, String> aktivan = kolona("AKTIVAN", 90, k -> k.aktivan() ? "Da" : "Ne");
        aktivan.setCellFactory(c -> pilula(t -> "Da".equals(t) ? "st-odobreno" : "st-iseljen"));

        tabela.getColumns().addAll(List.of(
                naziv, grupa, bodovi,
                kolona("PARAMETAR", 150, k -> k.traziPeriod() ? "period od–do" : k.traziParametar() ? k.nazivParametra() : "—"),
                kolona("DOKAZ", 210, k -> k.dokaz() != null ? k.dokaz() : "—"),
                kolona("PRIJAVA", 80, k -> String.valueOf(k.brojPrijava())),
                aktivan));
    }

    private TableColumn<KriterijPostavkaDTO, String> kolona(String naslov, double sirina,
                                                            Function<KriterijPostavkaDTO, String> f) {
        TableColumn<KriterijPostavkaDTO, String> c = new TableColumn<>(naslov);
        c.setPrefWidth(sirina);
        c.setCellValueFactory(d -> new ReadOnlyStringWrapper(f.apply(d.getValue())));
        return c;
    }

    /** Ćelija s pilulom u boji (klasa prema tekstu). */
    private TableCell<KriterijPostavkaDTO, String> pilula(Function<String, String> klasa) {
        return new TableCell<>() {
            private final Label l = new Label();

            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(null);
                if (empty || v == null) {
                    setGraphic(null);
                    return;
                }
                l.setText(v);
                l.getStyleClass().setAll("label", "pr-status", klasa.apply(v));
                setGraphic(l);
            }
        };
    }

    private KriterijPostavkaDTO odabrani() {
        return tabela.getSelectionModel().getSelectedItem();
    }

    // =====================================================================
    //  AKCIJE
    // =====================================================================

    private void promijeniAktivan() {
        KriterijPostavkaDTO k = odabrani();
        if (k == null) return;
        if (k.aktivan() && !potvrdi("Deaktivirati kriterij „" + k.naziv() + "“?\n\n"
                + "Studenti ga više neće moći označiti. Prijave koje ga već imaju zadržavaju ga i bodove.")) return;
        izvrsi(() -> PostavkeApi.sacuvajKriterij(k.saAktivan(!k.aktivan())), null);
    }

    private void obrisi() {
        KriterijPostavkaDTO k = odabrani();
        if (k == null) return;
        if (!potvrdi("Trajno obrisati kriterij „" + k.naziv() + "“?")) return;
        izvrsi(() -> PostavkeApi.obrisiKriterij(k.id()), "Kriterij je obrisan.");
    }

    /** k == null → novi kriterij. */
    private void uredi(KriterijPostavkaDTO k) {
        boolean novi = k == null;
        boolean formula = !novi && k.racunaSe();

        TextField tfNaziv = new TextField(novi ? "" : k.naziv());
        tfNaziv.setPrefWidth(380);

        ComboBox<String> cbGrupa = new ComboBox<>(FXCollections.observableArrayList(GRUPE.values()));
        cbGrupa.setValue(GRUPE.get(novi ? "DODATNI" : k.grupa()));
        cbGrupa.setMaxWidth(Double.MAX_VALUE);

        TextField tfBodovi = new TextField(!novi && k.bodovi() != null ? k.bodovi().stripTrailingZeros().toPlainString() : "");
        tfBodovi.setPrefWidth(110);
        tfBodovi.setMaxWidth(110);
        if (formula) {
            tfBodovi.setDisable(true);
            tfBodovi.setPromptText("formula");
        }

        CheckBox chkParam = new CheckBox("Student unosi broj (parametar)");
        chkParam.setSelected(!novi && k.traziParametar());
        TextField tfParam = new TextField(!novi && k.nazivParametra() != null ? k.nazivParametra() : "");
        tfParam.setPromptText("npr. Broj priznanja");
        tfParam.disableProperty().bind(chkParam.selectedProperty().not());

        CheckBox chkPeriod = new CheckBox("Student unosi period od – do");
        chkPeriod.setSelected(!novi && k.traziPeriod());

        ComboBox<KriterijiPostavkeDTO.Dokument> cbDokaz = new ComboBox<>();
        cbDokaz.getItems().add(BEZ_DOKAZA);
        cbDokaz.getItems().addAll(dokumenti);
        cbDokaz.setValue(novi || k.idDokaz() == null ? BEZ_DOKAZA
                : dokumenti.stream().filter(d -> Objects.equals(d.id(), k.idDokaz())).findFirst().orElse(BEZ_DOKAZA));
        cbDokaz.setMaxWidth(Double.MAX_VALUE);

        CheckBox chkAktivan = new CheckBox("Aktivan (student ga može označiti)");
        chkAktivan.setSelected(novi || k.aktivan());

        Label napomena = new Label(formula
                ? "Bodovi ovog kriterija računaju se formulom iz parametra (npr. stepen invalidnosti, broj mjeseci)."
                : "Student koji označi kriterij dobija ovoliko bodova. Parametar se samo evidentira i ne mijenja bodove.");
        napomena.setWrapText(true);
        napomena.setMaxWidth(420);

        GridPane g = new GridPane();
        g.setHgap(14);
        g.setVgap(10);
        g.setPadding(new Insets(4, 0, 0, 0));
        int r = 0;
        g.addRow(r++, new Label("Naziv"), tfNaziv);
        g.addRow(r++, new Label("Grupa"), cbGrupa);
        g.addRow(r++, new Label("Bodovi"), tfBodovi);
        g.add(napomena, 1, r++);
        g.add(chkParam, 1, r++);
        g.addRow(r++, new Label("Naziv parametra"), tfParam);
        g.add(chkPeriod, 1, r++);
        g.addRow(r++, new Label("Dokaz"), cbDokaz);
        g.add(chkAktivan, 1, r);
        if (!novi) {
            Label sifra = new Label("Šifra: " + k.sifra() + "   ·   koristi ga " + k.brojPrijava() + " prijava");
            sifra.setStyle("-fx-text-fill: #6b8290;");
            g.add(sifra, 1, ++r);
        }

        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle(novi ? "Novi kriterij" : "Uredi kriterij");
        d.setHeaderText(novi ? "Novi kriterij bodovanja" : k.naziv());
        d.getDialogPane().setContent(g);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        if (d.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        BigDecimal bodovi = null;
        String b = tfBodovi.getText() == null ? "" : tfBodovi.getText().trim().replace(',', '.');
        if (!formula && !b.isEmpty()) {
            try {
                bodovi = new BigDecimal(b);
            } catch (NumberFormatException e) {
                upozori("Bodovi moraju biti broj, npr. 3 ili 2.5");
                return;
            }
        }
        String grupa = GRUPE.entrySet().stream()
                .filter(e -> e.getValue().equals(cbGrupa.getValue()))
                .map(Map.Entry::getKey).findFirst().orElse(null);

        KriterijPostavkaDTO z = new KriterijPostavkaDTO(
                novi ? null : k.id(), novi ? null : k.sifra(),
                tfNaziv.getText(), bodovi, grupa,
                chkParam.isSelected(), tfParam.getText(), chkPeriod.isSelected(), chkAktivan.isSelected(),
                cbDokaz.getValue() != null ? cbDokaz.getValue().id() : null, null,
                0, formula);

        izvrsi(() -> PostavkeApi.sacuvajKriterij(z), novi ? "Kriterij je dodan." : "Kriterij je sačuvan.");
    }

    // =====================================================================
    //  POMOĆNE
    // =====================================================================

    private void izvrsi(Supplier<KriterijiPostavkeDTO> poziv, String poruka) {
        Task<KriterijiPostavkeDTO> task = new Task<>() {
            @Override
            protected KriterijiPostavkeDTO call() {
                return poziv.get();
            }
        };
        task.setOnSucceeded(e -> {
            KriterijiPostavkeDTO s = task.getValue();
            dokumenti = s.dokumenti();
            Integer odabran = odabrani() != null ? odabrani().id() : null;
            tabela.setItems(FXCollections.observableArrayList(s.kriteriji()));
            if (odabran != null) {
                tabela.getItems().stream().filter(x -> Objects.equals(x.id(), odabran)).findFirst()
                        .ifPresent(x -> tabela.getSelectionModel().select(x));
            }
            if (poruka != null) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, poruka);
                a.setHeaderText(null);
                a.showAndWait();
            }
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (!(ex instanceof ApiException)) ex.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR,
                    ex instanceof ApiException ? ex.getMessage() : "Greška u komunikaciji sa serverom.");
            a.setHeaderText(null);
            a.showAndWait();
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private static Label naslov(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("pr-kartica-naslov");
        return l;
    }

    private static Label sitno(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("pr-sitno");
        l.setWrapText(true);
        return l;
    }

    private static boolean potvrdi(String tekst) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, tekst, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private static void upozori(String tekst) {
        Alert a = new Alert(Alert.AlertType.WARNING, tekst);
        a.setHeaderText(null);
        a.showAndWait();
    }
}