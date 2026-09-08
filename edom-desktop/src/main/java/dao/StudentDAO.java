package dao;

import model.SocijalniStatus;
import model.Student;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {

    public void unesiStudent(Student s) {
        String sql = "INSERT INTO student " +
                "(ime, prezime, broj_indeksa, fakultet, godina_studija, prosjek, " +
                "email, telefon, socijalni_status_id_status, adresa, ime_roditelja, jmbg) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, s.getIme());
            stmt.setString(2, s.getPrezime());
            stmt.setString(3, s.getBrojIndeksa());
            stmt.setString(4, s.getFakultet());
            stmt.setInt(5, s.getGodinaStudija());
            stmt.setDouble(6, s.getProsjek());
            stmt.setString(7, s.getEmail());
            stmt.setString(8, s.getTelefon());
            stmt.setInt(9, s.getSocijalniStatus().getIdStatus());
            stmt.setString(10, s.getAdresa());
            stmt.setString(11, s.getImeRoditelja());
            stmt.setString(12, s.getJMBG());



            stmt.executeUpdate();
            System.out.println("Student uspješno unesen!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Student> dohvatiSveStudente() {
        List<Student> studenti = new ArrayList<>();

        String sql = """
        SELECT 
            st.id_student, st.ime, st.prezime, st.broj_indeksa,
            st.fakultet, st.godina_studija, st.prosjek,
            st.email, st.telefon, st.adresa, st.ime_roditelja, st.jmbg,
            ss.id_status, ss.naziv AS naziv_statusa
        FROM student st
        LEFT JOIN socijalni_status ss 
            ON st.socijalni_status_id_status = ss.id_status
        """;

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Student s = new Student();

                s.setIdStudent(rs.getInt("id_student"));
                s.setIme(rs.getString("ime"));
                s.setPrezime(rs.getString("prezime"));
                s.setBrojIndeksa(rs.getString("broj_indeksa"));
                s.setFakultet(rs.getString("fakultet"));
                s.setGodinaStudija(rs.getInt("godina_studija"));
                s.setProsjek(rs.getDouble("prosjek"));
                s.setEmail(rs.getString("email"));
                s.setTelefon(rs.getString("telefon"));
                s.setAdresa(rs.getString("adresa"));
                s.setImeRoditelja(rs.getString("ime_roditelja"));
                s.setJMBG(rs.getString("jmbg"));

                if (rs.getObject("id_status") != null) {
                    SocijalniStatus ss = new SocijalniStatus(
                            rs.getInt("id_status"),
                            rs.getString("naziv_statusa")
                    );
                    s.setSocijalniStatus(ss);
                } else {
                    s.setSocijalniStatus(null);
                }

                studenti.add(s);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return studenti;
    }

    public Student dohvatiStudentaPoId(int id) {
        String sql = "SELECT st.id_student, st.ime, st.prezime, st.broj_indeksa, " +
                "st.fakultet, st.godina_studija, st.prosjek, st.email, st.telefon, " +
                "st.socijalni_status_id_status, st.adresa, st.ime_roditelja, st.jmbg, " +
                "ss.id_status, ss.naziv AS naziv_statusa " +
                "FROM student st " +
                "JOIN socijalni_status ss ON st.socijalni_status_id_status = ss.id_status " +
                "WHERE st.id_student = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Student s = new Student();

                    s.setIdStudent(rs.getInt("id_student"));
                    s.setIme(rs.getString("ime"));
                    s.setPrezime(rs.getString("prezime"));
                    s.setBrojIndeksa(rs.getString("broj_indeksa"));
                    s.setFakultet(rs.getString("fakultet"));
                    s.setGodinaStudija(rs.getInt("godina_studija"));
                    s.setProsjek(rs.getDouble("prosjek"));
                    s.setEmail(rs.getString("email"));
                    s.setTelefon(rs.getString("telefon"));

                    SocijalniStatus ss = new SocijalniStatus(
                            rs.getInt("id_status"),
                            rs.getString("naziv_statusa")
                    );
                    s.setSocijalniStatus(ss);
                    s.setAdresa(rs.getString("adresa"));
                    s.setJMBG(rs.getString("jmbg"));
                    s.setImeRoditelja(rs.getString("ime_roditelja"));

                    return s;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    public void azurirajStudent(Student s) {
        String sql = "UPDATE student SET " +
                "ime = ?, prezime = ?, broj_indeksa = ?, fakultet = ?, godina_studija = ?, " +
                "prosjek = ?, email = ?, telefon = ?, socijalni_status_id_status = ?, " +
                "adresa = ?, ime_roditelja = ?, jmbg = ? " +
                "WHERE id_student = ?";


        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, s.getIme());
            stmt.setString(2, s.getPrezime());
            stmt.setString(3, s.getBrojIndeksa());
            stmt.setString(4, s.getFakultet());
            stmt.setInt(5, s.getGodinaStudija());
            stmt.setDouble(6, s.getProsjek());
            stmt.setString(7, s.getEmail());
            stmt.setString(8, s.getTelefon());
            stmt.setInt(9, s.getSocijalniStatus().getIdStatus());
            stmt.setInt(10, s.getIdStudent());
            stmt.setString(11, s.getAdresa());
            stmt.setString(12, s.getJMBG());
            stmt.setString(13, s.getImeRoditelja());

            int redovi = stmt.executeUpdate();
            if (redovi > 0)
                System.out.println("Student ažuriran!");
            else
                System.out.println("Student sa ID " + s.getIdStudent() + " ne postoji!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void obrisiStudent(int id) {
        String sql = "DELETE FROM student WHERE id_student = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int redovi = stmt.executeUpdate();
            if (redovi > 0)
                System.out.println("Student obrisan!");
            else
                System.out.println("Student sa ID " + id + " ne postoji!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int countStudents() {
        String sql = "SELECT COUNT(*) FROM student";

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

    public Student findByBrojIndeksa(String indeks) {
        String sql = "SELECT st.id_student, st.ime, st.prezime, st.broj_indeksa, st.fakultet, st.godina_studija, " +
                "st.prosjek, st.email, st.telefon, ss.id_status, ss.naziv, st.adresa, st.ime_roditelja, st.jmbg " +
                "FROM student st JOIN socijalni_status ss ON st.socijalni_status_id_status = ss.id_status " +
                "WHERE st.broj_indeksa = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, indeks);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Student(
                            rs.getInt("id_student"),
                            rs.getString("ime"),
                            rs.getString("prezime"),
                            rs.getString("broj_indeksa"),
                            rs.getString("fakultet"),
                            rs.getInt("godina_studija"),
                            rs.getDouble("prosjek"),
                            rs.getString("email"),
                            rs.getString("telefon"),
                            new SocijalniStatus(
                                    rs.getInt("id_status"),
                                    rs.getString("naziv")
                            ),
                            rs.getString("adresa"),
                            rs.getString("ime_roditelja"),
                            rs.getString("jmbg")

                    );
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return null;
    }
    public boolean postojiJmbg(String jmbg) {
        String sql = "SELECT 1 FROM student WHERE jmbg = ? LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, jmbg);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }



}
