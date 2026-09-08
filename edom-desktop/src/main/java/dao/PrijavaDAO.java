package dao;

import model.Prijava;
import model.StatusPrijave;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PrijavaDAO {

    public void unesiPrijavu(Prijava prijava) {
        String sql = "INSERT INTO prijava " +
                "(datum_prijave, ukupni_bodovi, napomena, studentid_student2, " +
                "status_prijaveid_status, akademska_godina) " +
                "VALUES (?,?,?,?,?,?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setDate(1, java.sql.Date.valueOf(prijava.getDatumPrijava()));
            stmt.setDouble(2, prijava.getUkupniBodovi());
            stmt.setString(3, prijava.getNapomena());
            stmt.setInt(4, prijava.getIdStudent());
            stmt.setInt(5, prijava.getStatusPrijave().getIdStatus());
            stmt.setInt(6, prijava.getAkademskaGodina());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    prijava.setIdPrijava(rs.getInt(1));
                }
            }

            System.out.println("Prijava uspješno unesena!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Prijava> dohvatiSvePrijave() {
        List<Prijava> prijave = new ArrayList<>();

        String sql = """
    SELECT
        p.id_prijava,
        p.datum_prijave,
        p.ukupni_bodovi,
        p.napomena,
        p.studentid_student2,
        p.akademska_godina,

        s.id_status,
        s.naziv AS naziv_statusa,

        st.ime AS ime_studenta,
        st.prezime AS prezime_studenta,
        st.ime_roditelja            -- <=== OVO JE NEDOSTAJALO
        
    FROM prijava p
    JOIN status_prijave s
        ON p.status_prijaveid_status = s.id_status
    JOIN student st
        ON p.studentid_student2 = st.id_student
    """;

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                prijave.add(mapToPrijava(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return prijave;
    }



    public List<Prijava> dohvatiPrijaveZaStudenta(int idStudent) {
        List<Prijava> prijave = new ArrayList<>();

        String sql = "SELECT p.id_prijava, p.datum_prijave, p.ukupni_bodovi, p.napomena, " +
                "p.studentid_student2, p.status_prijaveid_status, p.akademska_godina, " +
                "s.id_status, s.naziv AS naziv_statusa " +
                "FROM prijava p " +
                "JOIN status_prijave s ON p.status_prijaveid_status = s.id_status " +
                "WHERE p.studentid_student2 = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idStudent);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    prijave.add(mapToPrijava(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return prijave;
    }

    public Prijava dohvatiPrijavuPoId(int idPrijava) {
        String sql = "SELECT p.id_prijava, p.datum_prijave, p.ukupni_bodovi, p.napomena, " +
                "p.studentid_student2, p.status_prijaveid_status, p.akademska_godina, " +
                "s.id_status, s.naziv AS naziv_statusa " +
                "FROM prijava p " +
                "JOIN status_prijave s ON p.status_prijaveid_status = s.id_status " +
                "WHERE p.id_prijava = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idPrijava);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapToPrijava(rs);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    public void azurirajPrijavu(Prijava prijava) {
        String sql = "UPDATE prijava SET " +
                "datum_prijave = ?, ukupni_bodovi = ?, napomena = ?, " +
                "studentid_student2 = ?, status_prijaveid_status = ?, akademska_godina = ? " +
                "WHERE id_prijava = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, java.sql.Date.valueOf(prijava.getDatumPrijava()));
            stmt.setDouble(2, prijava.getUkupniBodovi());
            stmt.setString(3, prijava.getNapomena());
            stmt.setInt(4, prijava.getIdStudent());
            stmt.setInt(5, prijava.getStatusPrijave().getIdStatus());
            stmt.setInt(6, prijava.getAkademskaGodina());
            stmt.setInt(7, prijava.getIdPrijava());

            int redovi = stmt.executeUpdate();
            if (redovi > 0)
                System.out.println("Prijava ažurirana!");
            else
                System.out.println("Prijava sa ID " + prijava.getIdPrijava() + " ne postoji!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void obrisiPrijavu(int idPrijava) {
        String sql = "DELETE FROM prijava WHERE id_prijava = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idPrijava);

            int redovi = stmt.executeUpdate();
            if (redovi > 0)
                System.out.println("Prijava obrisana!");
            else
                System.out.println("Prijava sa ID " + idPrijava + " ne postoji!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void promijeniStatusPrijave(int idPrijava, StatusPrijave noviStatus) {
        String sql = "UPDATE prijava SET status_prijaveid_status = ? WHERE id_prijava = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, noviStatus.getIdStatus());
            stmt.setInt(2, idPrijava);

            int redovi = stmt.executeUpdate();
            if (redovi > 0)
                System.out.println("Status prijave promijenjen!");
            else
                System.out.println("Prijava sa ID " + idPrijava + " ne postoji!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Prijava mapToPrijava(ResultSet rs) throws SQLException {
        Prijava p = new Prijava();

        p.setIdPrijava(rs.getInt("id_prijava"));

        java.sql.Date sqlDate = rs.getDate("datum_prijave");
        if (sqlDate != null) {
            p.setDatumPrijava(sqlDate.toLocalDate());
        }

        p.setUkupniBodovi(rs.getDouble("ukupni_bodovi"));
        p.setNapomena(rs.getString("napomena"));
        p.setIdStudent(rs.getInt("studentid_student2"));
        p.setAkademskaGodina(rs.getInt("akademska_godina"));

        StatusPrijave sp = new StatusPrijave(
                rs.getInt("id_status"),
                rs.getString("naziv_statusa")
        );
        p.setStatusPrijave(sp);

        p.setImeStudenta(rs.getString("ime_studenta"));
        p.setPrezimeStudenta(rs.getString("prezime_studenta"));


        try {
            p.setImeRoditelja(rs.getString("ime_roditelja"));
        } catch (SQLException e) {

            p.setImeRoditelja("");
        }

        return p;
    }

    public int countPrijave() {
        String sql = "SELECT COUNT(*) FROM prijava";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next())
                return rs.getInt(1);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return 0;
    }

    public void dodajBodoveNaPrijavu(int prijavaId, double bodoviZaDodati) {
        String sql = """
        UPDATE prijava
        SET ukupni_bodovi = IFNULL(ukupni_bodovi, 0) + ?
        WHERE id_prijava = ?
    """;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, bodoviZaDodati);
            stmt.setInt(2, prijavaId);
            stmt.executeUpdate();

            System.out.println("Dodano " + bodoviZaDodati + " bodova na prijavu ID = " + prijavaId);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Integer> countPrijavePoDanimaUTekucemMjesecu() {
        List<Integer> dani = new ArrayList<>();

        String sql = """
        SELECT DAY(datum_prijave) AS dan, COUNT(*) AS broj
        FROM prijava
        WHERE MONTH(datum_prijave) = MONTH(CURRENT_DATE)
          AND YEAR(datum_prijave) = YEAR(CURRENT_DATE)
        GROUP BY DAY(datum_prijave)
        ORDER BY dan
    """;

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                dani.add(rs.getInt("dan"));
                dani.add(rs.getInt("broj"));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return dani;
    }

    public int countPrijaveByStatusId(int statusId) {
        String sql = "SELECT COUNT(*) FROM prijava WHERE status_prijaveid_status = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, statusId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return rs.getInt(1);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return 0;
    }


    public int countPrijaveBezBodova() {
        String sql = """
        SELECT COUNT(*)
        FROM prijava
        WHERE ukupni_bodovi IS NULL OR ukupni_bodovi = 0
    """;

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next())
                return rs.getInt(1);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return 0;
    }


}
