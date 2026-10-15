package controllers;

import api.ApiException;
import api.DashboardApi;
import ba.unze.edom.common.dto.DashboardDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import service.Session;
import util.Ekrani;

import java.util.Map;

public class DashboardController {

    private AdminController adminController;

    @FXML private Label lblStudenti;
    @FXML private Label lblPrijave;
    @FXML private PieChart pieStatusChart;
    @FXML private Label lblAdminIme;

    @FXML private Label lblNaPregledu;
    @FXML private Label lblOdobrene;
    @FXML private Label lblOdbijene;
    @FXML private Label lblBezBodova;     // sad: "čeka provjeru"
    @FXML private Label lblZakljucene;

    @FXML
    public void initialize() {
        if (lblAdminIme != null && Session.getUsername() != null) {
            lblAdminIme.setText(Session.getUsername());
        }
        ucitaj();
    }

    private void ucitaj() {
        Task<DashboardDTO> task = new Task<>() {
            @Override
            protected DashboardDTO call() {
                return DashboardApi.dohvati();
            }
        };
        task.setOnSucceeded(e -> prikazi(task.getValue()));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (!(ex instanceof ApiException)) ex.printStackTrace();
            postaviNule();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void prikazi(DashboardDTO d) {
        Map<String, Long> s = d.poStatusu();

        lblStudenti.setText(String.valueOf(d.brojStudenata()));
        lblPrijave.setText(String.valueOf(d.brojPrijava()));

        lblNaPregledu.setText(String.valueOf(broj(s, "pregl")));
        lblOdobrene.setText(String.valueOf(broj(s, "odob")));
        lblOdbijene.setText(String.valueOf(broj(s, "odbi")));
        lblZakljucene.setText(String.valueOf(broj(s, "zaklj")));
        lblBezBodova.setText(String.valueOf(d.cekaProvjeru()));

        // grafikon: svi statusi koji stvarno postoje, bez fiksne liste
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        s.forEach((naziv, br) -> data.add(new PieChart.Data(naziv + " (" + br + ")", br)));
        pieStatusChart.setData(data);
        pieStatusChart.setTitle(d.akademskaGodina() != null
                ? "Akademska godina " + d.akademskaGodina() + "/" + (d.akademskaGodina() + 1)
                : "Nema prijava");
    }

    /** Zbir prijava čiji naziv statusa sadrži dati dio (npr. "odob" -> "odobreno"). */
    private long broj(Map<String, Long> poStatusu, String dio) {
        return poStatusu.entrySet().stream()
                .filter(e -> e.getKey().toLowerCase().contains(dio))
                .mapToLong(Map.Entry::getValue)
                .sum();
    }

    private void postaviNule() {
        for (Label l : new Label[]{lblStudenti, lblPrijave, lblNaPregledu,
                lblOdobrene, lblOdbijene, lblBezBodova, lblZakljucene}) {
            l.setText("—");
        }
    }

    // ---------- navigacija ----------

    public void setAdminController(AdminController adminController) {
        this.adminController = adminController;
    }

    @FXML
    private void goPrijave(ActionEvent e) {
        if (adminController != null) adminController.showPrijave(null);
    }

    @FXML
    private void goStudenti(ActionEvent e) {
        if (adminController != null) adminController.showStudenti(null);
    }

    @FXML
    private void onNovaPrijava() {
        Ekrani.zamijeni(pieStatusChart, "/views/novi-student.fxml", null);
    }
}