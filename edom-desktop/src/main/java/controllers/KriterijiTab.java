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
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/** Postavke → Kriteriji bodovanja: dodavanje, izmjena, (de)aktiviranje, brisanje. */
public class KriterijiTab extends Tab {

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

    public KriterijiTab(javafx.scene.Node pravila) {
        super("Kriteriji bodovanja");

        Label opis = new Label("Kriteriji koje student označava u prijavi. Izmjene se snimaju odmah, bez dugmeta \"Sačuvaj postavke\".");
        opis.setWrapText(true);
        Label upozorenje = new Label("⚠ Promjena bodova odmah mijenja bodove svih prijava s tim kriterijem. "
                + "Objavljena rang lista ostaje ista dok je ponovo ne objavite. Kriterij koji više ne vrijedi deaktivirajte, ne brišite.");
        upozorenje.setWrapText(true);
        upozorenje.setStyle("-fx-text-fill: #b86e00;");

        kolone();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
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
        VBox.setVgrow(tabela, Priority.ALWAYS);

        Button btnDodaj = new Button("+ Dodaj kriterij");
        btnDodaj.getStyleClass().add("btn-primary");
        btnDodaj.setOnAction(e -> uredi(null));
        Button btnUredi = new Button("Uredi");
        btnUredi.setOnAction(e -> uredi(odabrani()));
        btnAktivan.setOnAction(e -> promijeniAktivan());
        Button btnObrisi = new Button("Obriši");
        btnObrisi.setOnAction(e -> obrisi());

        for (Button b : List.of(btnUredi, btnAktivan, btnObrisi)) {
            b.disableProperty().bind(tabela.getSelectionModel().selectedItemProperty().isNull());
        }
        tabela.getSelectionModel().selectedItemProperty().addListener((o, a, k) ->
                btnAktivan.setText(k != null && !k.aktivan() ? "Aktiviraj" : "Deaktiviraj"));

        HBox alati = new HBox(8, btnDodaj, btnUredi, btnAktivan, btnObrisi);
        alati.setAlignment(Pos.CENTER_LEFT);

        Label nPravila = new Label("Uspjeh, udaljenost i primanja");
        nPravila.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label oPravila = new Label("Mijenjaju se dugmetom \"Sačuvaj postavke\" dolje. Neispravan unos server odbija i kaže šta ne valja.");
        oPravila.setWrapText(true);
        oPravila.setStyle("-fx-text-fill: #6b7a90;");

        Label nKriteriji = new Label("Branioci, socijalni i dodatni kriteriji");
        nKriteriji.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        tabela.setPrefHeight(380);
        VBox sadrzaj = new VBox(10, nPravila, oPravila);
        if (pravila != null) sadrzaj.getChildren().add(pravila);
        sadrzaj.getChildren().addAll(new Separator(), nKriteriji, opis, upozorenje, alati, tabela);
        sadrzaj.setPadding(new Insets(18));

        ScrollPane sp = new ScrollPane(sadrzaj);
        sp.setFitToWidth(true);
        setContent(sp);

        izvrsi(PostavkeApi::kriteriji, null);
    }

    // ---------- tabela ----------

    private void kolone() {
        tabela.getColumns().add(kolona("Naziv", 260, KriterijPostavkaDTO::naziv));
        tabela.getColumns().add(kolona("Grupa", 140, k -> "BRANIOCI".equals(k.grupa()) ? "Branioci (najveći)" : "Dodatni (zbir)"));
        tabela.getColumns().add(kolona("Bodovi", 90, k -> k.bodovi() != null
                ? k.bodovi().stripTrailingZeros().toPlainString() : (k.racunaSe() ? "formula" : "—")));
        tabela.getColumns().add(kolona("Parametar", 150, k -> k.traziPeriod() ? "period od–do"
                : k.traziParametar() ? k.nazivParametra() : "—"));
        tabela.getColumns().add(kolona("Dokaz", 200, k -> k.dokaz() != null ? k.dokaz() : "—"));
        tabela.getColumns().add(kolona("Prijava", 70, k -> String.valueOf(k.brojPrijava())));
        tabela.getColumns().add(kolona("Aktivan", 70, k -> k.aktivan() ? "Da" : "Ne"));
    }

    private TableColumn<KriterijPostavkaDTO, String> kolona(String naslov, double sirina,
                                                            Function<KriterijPostavkaDTO, String> f) {
        TableColumn<KriterijPostavkaDTO, String> c = new TableColumn<>(naslov);
        c.setPrefWidth(sirina);
        c.setCellValueFactory(d -> new ReadOnlyStringWrapper(f.apply(d.getValue())));
        return c;
    }

    private KriterijPostavkaDTO odabrani() {
        return tabela.getSelectionModel().getSelectedItem();
    }

    // ---------- akcije ----------

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
        tfNaziv.setPrefWidth(360);

        ComboBox<String> cbGrupa = new ComboBox<>(FXCollections.observableArrayList(GRUPE.values()));
        cbGrupa.setValue(GRUPE.get(novi ? "DODATNI" : k.grupa()));

        TextField tfBodovi = new TextField(!novi && k.bodovi() != null ? k.bodovi().stripTrailingZeros().toPlainString() : "");
        tfBodovi.setPrefWidth(100);
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
        cbDokaz.setPrefWidth(360);

        CheckBox chkAktivan = new CheckBox("Aktivan (student ga može označiti)");
        chkAktivan.setSelected(novi || k.aktivan());

        Label napomena = new Label(formula
                ? "Bodovi ovog kriterija računaju se formulom iz parametra (npr. stepen invalidnosti, broj mjeseci)."
                : "Student koji označi kriterij dobija ovoliko bodova. Parametar se samo evidentira i ne mijenja bodove.");
        napomena.setWrapText(true);
        napomena.setMaxWidth(480);
        napomena.setStyle("-fx-text-fill: #6b7a90;");

        GridPane g = new GridPane();
        g.setHgap(12);
        g.setVgap(10);
        g.setPadding(new Insets(10));
        int r = 0;
        g.addRow(r++, new Label("Naziv:"), tfNaziv);
        g.addRow(r++, new Label("Grupa:"), cbGrupa);
        g.addRow(r++, new Label("Bodovi:"), tfBodovi);
        g.add(napomena, 1, r++);
        g.add(chkParam, 1, r++);
        g.addRow(r++, new Label("Naziv parametra:"), tfParam);
        g.add(chkPeriod, 1, r++);
        g.addRow(r++, new Label("Dokaz:"), cbDokaz);
        g.add(chkAktivan, 1, r);
        if (!novi) {
            Label sifra = new Label("Šifra: " + k.sifra() + "   ·   koristi ga " + k.brojPrijava() + " prijava");
            sifra.setStyle("-fx-text-fill: #6b7a90;");
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

    // ---------- pomoćne ----------

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
            new Alert(Alert.AlertType.ERROR,
                    ex instanceof ApiException ? ex.getMessage() : "Greška u komunikaciji sa serverom.").showAndWait();
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
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