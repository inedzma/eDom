package dao;

import model.SocijalniStatus;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SocijalniStatusDAO {

    public void unesiStatus(SocijalniStatus status) {
        String sql = "INSERT INTO socijalni_status (naziv) VALUES (?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, status.getNaziv());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    status.setIdStatus(rs.getInt(1)); // sada objekat zna svoj ID
                }
            }

            System.out.println("Socijalni status uspješno unesen!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public List<SocijalniStatus> dohvatiSveStatuse() {
        List<SocijalniStatus> lista = new ArrayList<>();

        String sql = "SELECT * FROM socijalni_status";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                SocijalniStatus s = new SocijalniStatus();

                s.setIdStatus(rs.getInt("id_status"));
                s.setNaziv(rs.getString("naziv"));

                lista.add(s);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return lista;
    }

    public void azurirajStatus(SocijalniStatus status) {
        String sql = "UPDATE socijalni_status SET naziv = ? WHERE id_status = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.getNaziv());
            stmt.setInt(2, status.getIdStatus());

            int redovi = stmt.executeUpdate();

            if (redovi > 0)
                System.out.println("Socijalni status ažuriran!");
            else
                System.out.println("Status sa ID " + status.getIdStatus() + " ne postoji!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void obrisiStatus(int idStatus) {
        String sql = "DELETE FROM socijalni_status WHERE id_status = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idStatus);

            int redovi = stmt.executeUpdate();

            if (redovi > 0)
                System.out.println("Socijalni status obrisan!");
            else
                System.out.println("Status sa ID " + idStatus + " ne postoji!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public SocijalniStatus dohvatiStatusPoId(int id) {
        String sql = "SELECT * FROM socijalni_status WHERE id_status = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new SocijalniStatus(
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
