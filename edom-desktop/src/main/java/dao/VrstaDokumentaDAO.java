package dao;

import model.VrstaDokumenta;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VrstaDokumentaDAO {

    public void unesiVrstu(VrstaDokumenta vrsta) {
        String sql = "INSERT INTO vrsta_dokumenta (naziv) VALUES (?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, vrsta.getNaziv());
            stmt.executeUpdate();

            System.out.println("Vrsta dokumenta uspješno unesena!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<VrstaDokumenta> dohvatiSveVrste() {
        List<VrstaDokumenta> vrste = new ArrayList<>();

        String sql = "SELECT * FROM vrsta_dokumenta";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                VrstaDokumenta v = new VrstaDokumenta(
                        rs.getInt("id_vrsta"),
                        rs.getString("naziv")
                );

                vrste.add(v);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return vrste;
    }

    public void azurirajVrstu(VrstaDokumenta vrsta) {
        String sql = "UPDATE vrsta_dokumenta SET naziv = ? WHERE id_vrsta = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, vrsta.getNaziv());
            stmt.setInt(2, vrsta.getIdVrsta());

            int redovi = stmt.executeUpdate();

            if (redovi > 0)
                System.out.println("Vrsta dokumenta ažurirana!");
            else
                System.out.println("Vrsta dokumenta sa ID " + vrsta.getIdVrsta() + " ne postoji!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void obrisiVrstu(int idVrsta) {
        String sql = "DELETE FROM vrsta_dokumenta WHERE id_vrsta = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idVrsta);

            int redovi = stmt.executeUpdate();

            if (redovi > 0)
                System.out.println("Vrsta dokumenta obrisana!");
            else
                System.out.println("Vrsta dokumenta sa ID " + idVrsta + " ne postoji!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public VrstaDokumenta dohvatiVrstuPoId(int idVrsta) {
        String sql = "SELECT * FROM vrsta_dokumenta WHERE id_vrsta = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idVrsta);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new VrstaDokumenta(
                            rs.getInt("id_vrsta"),
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
