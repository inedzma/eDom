package dao;

import model.Uloga;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UlogaDAO {

    public void unesiUlogu(Uloga uloga) {
        String sql = "INSERT INTO uloga (naziv) VALUES (?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, uloga.getNaziv());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    uloga.setIdUloga(rs.getInt(1));
                }
            }

            System.out.println("Uloga uspješno unesena!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public List<Uloga> dohvatiSveUloge() {
        List<Uloga> uloge = new ArrayList<>();

        String sql = "SELECT * FROM uloga";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Uloga u = new Uloga();
                u.setIdUloga(rs.getInt("id_uloga"));
                u.setNaziv(rs.getString("naziv"));

                uloge.add(u);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return uloge;
    }

    public void azurirajUlogu(Uloga uloga) {
        String sql = "UPDATE uloga SET naziv = ? WHERE id_uloga = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, uloga.getNaziv());
            stmt.setInt(2, uloga.getIdUloga());

            int redovi = stmt.executeUpdate();

            if (redovi > 0)
                System.out.println("Uloga je ažurirana!");
            else
                System.out.println("Uloga sa ID " + uloga.getIdUloga() + " ne postoji!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void obrisiUlogu(int idUloga) {
        String sql = "DELETE FROM uloga WHERE id_uloga = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idUloga);

            int redovi = stmt.executeUpdate();

            if (redovi > 0)
                System.out.println("Uloga uspješno obrisana!");
            else
                System.out.println("Uloga sa ID " + idUloga + " ne postoji!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Uloga dohvatiUloguPoId(int id) {
        String sql = "SELECT * FROM uloga WHERE id_uloga = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Uloga(
                            rs.getInt("id_uloga"),
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
