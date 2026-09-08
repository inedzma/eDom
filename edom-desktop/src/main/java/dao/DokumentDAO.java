package dao;

import model.Dokument;
import model.VrstaDokumenta;
import util.DBConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DokumentDAO {

    public int unesiDokument(Dokument dokument, int prijavaId) {
        String sqlUpit = "INSERT INTO dokument " +
                "(naziv, datum_upload, broj_bodova, dokumentb64, isdostavljen, " +
                "vrsta_dokumentaid_vrsta, prijavaid_prijava) " +
                "VALUES (?,?,?,?,?,?,?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlUpit, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, dokument.getNaziv());
            stmt.setDate(2, java.sql.Date.valueOf(dokument.getDatumUpload()));
            stmt.setDouble(3, dokument.getBrojBodova());
            stmt.setString(4, dokument.getDokumentB64());
            stmt.setBoolean(5, dokument.isDostavljen());
            stmt.setInt(6, dokument.getVrstaDokumenta().getIdVrsta());
            stmt.setInt(7, prijavaId);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    dokument.setIdDokument(id);
                    return id;
                }
            }

            throw new RuntimeException("ID dokumenta nije generisan!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Dokument> dohvatiSveDokumente() {
        List<Dokument> dokumenti = new ArrayList<>();

        String sqlUpit = "SELECT d.id_dokument, d.naziv, d.datum_upload, d.broj_bodova, " +
                "d.dokumentb64, d.isdostavljen, d.vrsta_dokumentaid_vrsta, d.prijavaid_prijava, " +
                "v.id_vrsta, v.naziv AS naziv_vrste " +
                "FROM dokument d " +
                "JOIN vrsta_dokumenta v ON d.vrsta_dokumentaid_vrsta = v.id_vrsta";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlUpit)) {

            while (rs.next()) {
                Dokument d = new Dokument();

                d.setIdDokument(rs.getInt("id_dokument"));
                d.setNaziv(rs.getString("naziv"));

                LocalDate datum = rs.getDate("datum_upload").toLocalDate();
                d.setDatumUpload(datum);


                d.setBrojBodova(rs.getDouble("broj_bodova"));
                d.setDokumentB64(rs.getString("dokumentb64"));
                d.setDostavljen(rs.getBoolean("isdostavljen"));

                // VrstaDokumenta objekat
                VrstaDokumenta vrsta = new VrstaDokumenta(
                        rs.getInt("id_vrsta"),
                        rs.getString("naziv_vrste")
                );
                d.setVrstaDokumenta(vrsta);

                dokumenti.add(d);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return dokumenti;
    }

    public void azurirajDokument(Dokument dokument, int prijavaId) {
        String sqlUpit = "UPDATE dokument SET " +
                "naziv = ?, datum_upload = ?, broj_bodova = ?, dokumentb64 = ?, " +
                "isdostavljen = ?, vrsta_dokumentaid_vrsta = ?, prijavaid_prijava = ? " +
                "WHERE id_dokument = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlUpit)) {

            stmt.setString(1, dokument.getNaziv());
            stmt.setDate(2, java.sql.Date.valueOf(dokument.getDatumUpload()));
            stmt.setDouble(3, dokument.getBrojBodova());
            stmt.setString(4, dokument.getDokumentB64());
            stmt.setBoolean(5, dokument.isDostavljen());
            stmt.setInt(6, dokument.getVrstaDokumenta().getIdVrsta());
            stmt.setInt(7, prijavaId);
            stmt.setInt(8, dokument.getIdDokument());

            int redovi = stmt.executeUpdate();
            if (redovi > 0)
                System.out.println("Dokument je ažuriran!");
            else
                System.out.println("Dokument sa ID " + dokument.getIdDokument() + " ne postoji!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void obrisiDokument(int id) {
        String sqlUpit = "DELETE FROM dokument WHERE id_dokument = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlUpit)) {

            stmt.setInt(1, id);

            int redovi = stmt.executeUpdate();
            if (redovi > 0)
                System.out.println("Dokument je obrisan!");
            else
                System.out.println("Dokument sa ID " + id + " ne postoji!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void dodajBodove(int dokumentId, double bodovi){
        String sqlUpit = "UPDATE dokument SET broj_bodova = broj_bodova + ? WHERE id_dokument = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlUpit)) {

            stmt.setDouble(1, bodovi);
            stmt.setInt(2, dokumentId);

            int redovi = stmt.executeUpdate();
            if (redovi > 0)
                System.out.println("Bodovi su dodani na dokument!");
            else
                System.out.println("Dokument sa ID " + dokumentId + " ne postoji!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Dokument> dohvatiDokumenteZaPrijavu(int prijavaId) {
        List<Dokument> dokumenti = new ArrayList<>();

        String sql = "SELECT d.*, v.id_vrsta, v.naziv AS naziv_vrste " +
                "FROM dokument d " +
                "JOIN vrsta_dokumenta v ON d.vrsta_dokumentaid_vrsta = v.id_vrsta " +
                "WHERE d.prijavaid_prijava = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, prijavaId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Dokument d = new Dokument();

                    d.setIdDokument(rs.getInt("id_dokument"));
                    d.setNaziv(rs.getString("naziv"));
                    d.setDatumUpload(rs.getDate("datum_upload").toLocalDate());
                    d.setBrojBodova(rs.getDouble("broj_bodova"));
                    d.setDokumentB64(rs.getString("dokumentb64"));
                    d.setDostavljen(rs.getBoolean("isdostavljen"));

                    d.setVrstaDokumenta(new VrstaDokumenta(
                            rs.getInt("id_vrsta"),
                            rs.getString("naziv_vrste")
                    ));

                    dokumenti.add(d);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return dokumenti;
    }

    //metoda za brisanje dokumenata za prijavu
    public void obrisiDokumenteZaPrijavu(int prijavaId) {
        String sql = "DELETE FROM dokument WHERE prijavaid_prijava = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, prijavaId);
            stmt.executeUpdate();

            System.out.println("Svi dokumenti obrisani za prijavu ID = " + prijavaId);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}
