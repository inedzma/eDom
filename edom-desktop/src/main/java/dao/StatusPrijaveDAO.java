package dao;

import model.StatusPrijave;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StatusPrijaveDAO {

    public void unesiStatus(StatusPrijave status) {
        String sql = "INSERT INTO status_prijave (naziv) VALUES (?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.getNaziv());
            stmt.executeUpdate();

            System.out.println("Status prijave je uspješno unesen!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<StatusPrijave> dohvatiSveStatuse() {
        List<StatusPrijave> lista = new ArrayList<>();

        String sql = "SELECT * FROM status_prijave";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                StatusPrijave s = new StatusPrijave();
                s.setIdStatus(rs.getInt("id_status"));   // promijeni ako se PK drugačije zove
                s.setNaziv(rs.getString("naziv"));

                lista.add(s);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return lista;
    }

    public void azurirajStatus(StatusPrijave status) {
        String sql = "UPDATE status_prijave SET naziv = ? WHERE id_status = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.getNaziv());
            stmt.setInt(2, status.getIdStatus());

            int redovi = stmt.executeUpdate();

            if (redovi > 0)
                System.out.println("Status prijave je ažuriran!");
            else
                System.out.println("Status prijave sa ID " + status.getIdStatus() + " ne postoji!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void obrisiStatus(int idStatus) {
        String sql = "DELETE FROM status_prijave WHERE id_status = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idStatus);

            int redovi = stmt.executeUpdate();

            if (redovi > 0)
                System.out.println("Status prijave je obrisan!");
            else
                System.out.println("Status prijave sa ID " + idStatus + " ne postoji!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public StatusPrijave dohvatiStatusPoId(int id) {
        String sql = "SELECT * FROM status_prijave WHERE id_status = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new StatusPrijave(
                            rs.getInt("id_status"),
                            rs.getString("naziv")
                    );
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return null;
    }
}
