package controllers;

import dao.PrijavaDAO;
import dao.StatusPrijaveDAO;
import dao.StudentDAO;
import dao.DokumentDAO;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Dokument;
import model.Prijava;
import model.StatusPrijave;
import model.Student;
import service.PdfService;
import service.EmailService;

import java.util.List;

public class DetaljiPrijaveController {

    @FXML private Label lblNaslov;
    @FXML private Label lblInfo;
    @FXML private Label lblPrijavaId;
    @FXML private Label lblStatus;

    @FXML private Label lblKompletiranostText;
    @FXML private ProgressBar pbKompletiranost;

    @FXML private Label lblUkupniBodoviValue;
    @FXML private Label lblDostavljeniCount;
    @FXML private Label lblStatusObrade;
    @FXML private Label lblNedostajuCount;

    @FXML private TableView<Dokument> tblDokumenti;
    @FXML private TableColumn<Dokument, String> colNaziv;
    @FXML private TableColumn<Dokument, String> colVrsta;
    @FXML private TableColumn<Dokument, String> colDatum;
    @FXML private TableColumn<Dokument, Double> colBodovi;
    @FXML private TableColumn<Dokument, String> colDostavljen;
    @FXML private TableColumn<Dokument, Void> colPregled;

    @FXML private ComboBox<StatusPrijave> cmbStatusPrijave;
    @FXML private Button btnSacuvajStatus;

    @FXML private VBox emptyState;


    private final DokumentDAO dokumentDAO = new DokumentDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private final PrijavaDAO prijavaDAO = new PrijavaDAO();

    private Prijava trenutnaPrijava;


    @FXML
    private void onZatvori() {
        ((Stage) lblNaslov.getScene().getWindow()).close();
    }


    public void setPrijava(Prijava prijava) {
        this.trenutnaPrijava = prijava;

        Student student = studentDAO.dohvatiStudentaPoId(prijava.getIdStudent());
        lblNaslov.setText(student != null
                ? "Detalji prijave – " + student.getIme() + " " + student.getPrezime()
                : "Detalji prijave #" + prijava.getIdPrijava());

        lblInfo.setText("Akademska godina: " + prijava.getAkademskaGodina());
        lblPrijavaId.setText("Prijava #" + prijava.getIdPrijava());

        String statusNaziv = prijava.getStatusPrijave() != null
                ? prijava.getStatusPrijave().getNaziv()
                : "na pregledu";

        lblStatus.setText(statusNaziv);
        lblStatusObrade.setText(statusNaziv);
        setStatusPillStyle(statusNaziv);

        inicijalizujStatusCombo(prijava.getStatusPrijave());

        lblUkupniBodoviValue.setText(String.valueOf(prijava.getUkupniBodovi()));

        inicijalizujTabelu();

        List<Dokument> docs = dokumentDAO.dohvatiDokumenteZaPrijavu(prijava.getIdPrijava());
        tblDokumenti.setItems(FXCollections.observableArrayList(docs));

        updateStatsAndProgress(docs);
        updateEmptyState(docs);
    }


    private void inicijalizujStatusCombo(StatusPrijave trenutni) {

        StatusPrijaveDAO spDao = new StatusPrijaveDAO();
        List<StatusPrijave> statusi = spDao.dohvatiSveStatuse();
        cmbStatusPrijave.setItems(FXCollections.observableArrayList(statusi));

        cmbStatusPrijave.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(StatusPrijave item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNaziv()); // prikazuje samo naziv
            }
        });

        cmbStatusPrijave.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(StatusPrijave item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNaziv()); // prikazuje samo naziv
            }
        });

        if (trenutni != null) {
            cmbStatusPrijave.getItems().stream()
                    .filter(s -> s.getIdStatus() == trenutni.getIdStatus())
                    .findFirst()
                    .ifPresent(cmbStatusPrijave::setValue);
        }
    }


    @FXML
    private void onSacuvajStatus() {
        StatusPrijave noviStatus = cmbStatusPrijave.getValue();

        if (noviStatus == null || trenutnaPrijava == null) return;

        // Update status u bazi
        prijavaDAO.promijeniStatusPrijave(trenutnaPrijava.getIdPrijava(), noviStatus);

        trenutnaPrijava.setStatusPrijave(noviStatus);
        lblStatus.setText(noviStatus.getNaziv());
        lblStatusObrade.setText(noviStatus.getNaziv());
        setStatusPillStyle(noviStatus.getNaziv());

        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Status prijave");
        a.setHeaderText(null);
        a.setContentText("Status prijave je uspješno promijenjen.");
        a.showAndWait();
    }

    private void inicijalizujTabelu() {

        colNaziv.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNaziv()));
        colVrsta.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getVrstaDokumenta() != null ? d.getValue().getVrstaDokumenta().getNaziv() : ""
        ));
        colDatum.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getDatumUpload() != null ? d.getValue().getDatumUpload().toString() : ""
        ));
        colBodovi.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getBrojBodova()).asObject());
        colDostavljen.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isDostavljen() ? "DA" : "NE"));

        colDostavljen.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();
            {
                badge.getStyleClass().add("badge");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    badge.setText(item);
                    badge.getStyleClass().removeAll("badge-yes", "badge-no");
                    badge.getStyleClass().add(item.equals("DA") ? "badge-yes" : "badge-no");
                    setGraphic(badge);
                }
            }
        });

        colPregled.setCellFactory(col -> new TableCell<>() {

            private final Button btn = new Button("Pregledaj");

            {
                btn.getStyleClass().add("btn-action");

                btn.setOnAction(e -> {
                    Dokument d = getTableView().getItems().get(getIndex());

                    if (d == null || d.getDokumentB64() == null || d.getDokumentB64().isEmpty()) {
                        return;
                    }

                    PdfService.prikaziPdf(d.getDokumentB64(), d.getNaziv());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                Dokument d = getTableView().getItems().get(getIndex());

                boolean imaDokument = d != null && d.getDokumentB64() != null && !d.getDokumentB64().isEmpty();

                btn.setDisable(!imaDokument);

                btn.getStyleClass().remove("btn-action-disabled");
                if (!imaDokument) {
                    btn.getStyleClass().add("btn-action-disabled");
                }

                setGraphic(btn);
            }
        });

    }

    private void updateStatsAndProgress(List<Dokument> docs) {
        int total = docs.size();
        long dostavljeno = docs.stream().filter(Dokument::isDostavljen).count();
        long nedostaje = total - dostavljeno;

        lblDostavljeniCount.setText(dostavljeno + " / " + total);
        if (lblNedostajuCount != null) {
            lblNedostajuCount.setText(String.valueOf(nedostaje));
        }


        double progress = total == 0 ? 0 : (double) dostavljeno / total;
        pbKompletiranost.setProgress(progress);
        lblKompletiranostText.setText("Kompletiranost: " + (int) (progress * 100) + "%");
    }

    private void updateEmptyState(List<Dokument> docs) {
        boolean empty = docs == null || docs.isEmpty();
        emptyState.setVisible(empty);
        emptyState.setManaged(empty);
    }

    private void setStatusPillStyle(String status) {
        lblStatus.getStyleClass().removeAll(
                "status-info", "status-success", "status-warning", "status-danger"
        );

        String s = status.toLowerCase();

        if (s.contains("odob")) {
            lblStatus.getStyleClass().add("status-success");
        } else if (s.contains("odbij")) {
            lblStatus.getStyleClass().add("status-danger");
        } else if (s.contains("zaklj")) {
            lblStatus.getStyleClass().add("status-warning");
        } else {
            lblStatus.getStyleClass().add("status-info");
        }
    }

        //dugme posalji email studentu za odobrenje prijave
    @FXML
    private void onPosaljiEmail() {

        if (trenutnaPrijava == null) return;

        StatusPrijave status = trenutnaPrijava.getStatusPrijave();

        // Email se smije poslati samo ako je prijava odobrena
        if (status == null || !status.getNaziv().toLowerCase().contains("odob")) {
            new Alert(Alert.AlertType.WARNING,
                    "Email se može poslati samo za odobrenu prijavu."
            ).showAndWait();
            return;
        }

        Student student = studentDAO.dohvatiStudentaPoId(trenutnaPrijava.getIdStudent());

        if (student == null || student.getEmail() == null || student.getEmail().isBlank()) {
            new Alert(Alert.AlertType.ERROR,
                    "Student nema validnu email adresu."
            ).showAndWait();
            return;
        }

        EmailService.sendPrijavaOdobrenaEmail(
                student.getEmail(),
                student.getIme(),
                student.getPrezime(),
                trenutnaPrijava.getAkademskaGodina()
        );

        new Alert(Alert.AlertType.INFORMATION,
                "Email je uspješno poslan studentu."
        ).showAndWait();
    }

    //brisanje prijave
    @FXML
    private void onObrisiPrijavu() {

        if (trenutnaPrijava == null) return;

        Alert potvrda = new Alert(Alert.AlertType.CONFIRMATION);
        potvrda.setTitle("Potvrda brisanja");
        potvrda.setHeaderText("Brisanje prijave");
        potvrda.setContentText(
                "Da li ste sigurni da želite obrisati ovu prijavu?\n\n" +
                        "Svi dokumenti vezani za prijavu će biti trajno obrisani."
        );

        if (potvrda.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            int prijavaId = trenutnaPrijava.getIdPrijava();

            // obriši dokumente
            dokumentDAO.obrisiDokumenteZaPrijavu(prijavaId);

            // obriši prijavu
            prijavaDAO.obrisiPrijavu(prijavaId);

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Uspjeh");
            info.setHeaderText(null);
            info.setContentText("Prijava je uspješno obrisana.");
            info.showAndWait();

            // zatvori prozor detalja
            ((Stage) lblNaslov.getScene().getWindow()).close();

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                    "Došlo je do greške prilikom brisanja prijave."
            ).showAndWait();
            e.printStackTrace();
        }
    }
}
