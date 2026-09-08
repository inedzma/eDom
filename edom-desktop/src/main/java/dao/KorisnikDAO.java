package dao;

import model.Korisnik;
import model.Uloga;
import util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class KorisnikDAO {

    public void unesiKorisnika(Korisnik k) {
        String sql = "INSERT INTO korisnik (ime, prezime, username, password_hash, ulogaid_uloga, email) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, k.getIme());
            stmt.setString(2, k.getPrezime());
            stmt.setString(3, k.getUsername());
            stmt.setString(4, k.getPasswordHash());
            stmt.setInt(5, k.getUloga().getIdUloga());
            stmt.setString(6, k.getEmail());

            stmt.executeUpdate();
            System.out.println("Korisnik je uspješno dodan!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public List<Korisnik> dohvatiSveKorisnike() {
        List<Korisnik> lista = new ArrayList<>();

        String sql = "SELECT k.*, u.id_uloga, u.naziv AS naziv_uloge " +
                "FROM korisnik k " +
                "JOIN uloga u ON k.ulogaid_uloga = u.id_uloga";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Korisnik k = new Korisnik();

                k.setIdKorisnik(rs.getInt("id_korisnik"));
                k.setIme(rs.getString("ime"));
                k.setPrezime(rs.getString("prezime"));
                k.setUsername(rs.getString("username"));
                k.setPasswordHash(rs.getString("password_hash"));
                k.setEmail(rs.getString("email"));
                k.setZadnjaPrijava(rs.getTimestamp("zadnja_prijava"));

                Uloga u = new Uloga(
                        rs.getInt("id_uloga"),
                        rs.getString("naziv_uloge")
                );

                k.setUloga(u);
                lista.add(k);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return lista;
    }

    public void azurirajKorisnika(Korisnik k) {
        String sql = "UPDATE korisnik SET ime=?, prezime=?, username=?, password_hash=?, email=?, " +
                "ulogaid_uloga=? WHERE id_korisnik=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, k.getIme());
            stmt.setString(2, k.getPrezime());
            stmt.setString(3, k.getUsername());
            stmt.setString(4, k.getPasswordHash());
            stmt.setString(5, k.getEmail()); // ispravljeno – prije je bila uloga
            stmt.setInt(6, k.getUloga().getIdUloga());
            stmt.setInt(7, k.getIdKorisnik());

            int rows = stmt.executeUpdate();
            if (rows > 0)
                System.out.println("Korisnik ažuriran!");
            else
                System.out.println("Korisnik sa ID " + k.getIdKorisnik() + " ne postoji!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void obrisiKorisnika(int id) {
        String sql = "DELETE FROM korisnik WHERE id_korisnik=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            int rows = stmt.executeUpdate();
            if (rows > 0)
                System.out.println("Korisnik obrisan!");
            else
                System.out.println("Korisnik sa ID " + id + " ne postoji!");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Korisnik nadjiPoUsernameIPassword(String username, String passwordHash) {
        String sql = "SELECT k.*, u.id_uloga, u.naziv AS naziv_uloge " +
                "FROM korisnik k " +
                "JOIN uloga u ON k.ulogaid_uloga = u.id_uloga " +
                "WHERE k.username = ? AND k.password_hash = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, passwordHash);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {

                    Korisnik k = new Korisnik();
                    k.setIdKorisnik(rs.getInt("id_korisnik"));
                    k.setIme(rs.getString("ime"));
                    k.setPrezime(rs.getString("prezime"));
                    k.setUsername(rs.getString("username"));
                    k.setPasswordHash(rs.getString("password_hash"));
                    k.setEmail(rs.getString("email"));
                    k.setZadnjaPrijava(rs.getTimestamp("zadnja_prijava"));

                    Uloga u = new Uloga(
                            rs.getInt("id_uloga"),
                            rs.getString("naziv_uloge")
                    );

                    k.setUloga(u);

                    return k;
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    public Korisnik nadjiUsername(String username) throws SQLException {
        String sql = "SELECT k.*, u.id_uloga, u.naziv AS naziv_uloge " +
                "FROM korisnik k " +
                "JOIN uloga u ON k.ulogaid_uloga = u.id_uloga " +
                "WHERE k.username = ?";

        try(Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try(ResultSet rd = stmt.executeQuery()) {
                if(rd.next()) {
                    Korisnik k = new Korisnik();
                    k.setIdKorisnik(rd.getInt("id_korisnik"));
                    k.setIme(rd.getString("ime"));
                    k.setPrezime(rd.getString("prezime"));
                    k.setUsername(rd.getString("username"));
                    k.setPasswordHash(rd.getString("password_hash"));
                    k.setEmail(rd.getString("email"));
                    k.setZadnjaPrijava(rd.getTimestamp("zadnja_prijava"));

                    Uloga u = new Uloga(
                            rd.getInt("id_uloga"),
                            rd.getString("naziv_uloge")
                    );

                    k.setUloga(u);

                    return k;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public boolean promijeniLozinku(String username, String newPasswordHash) {
        String sql = "UPDATE korisnik SET password_hash = ? WHERE username = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newPasswordHash);
            stmt.setString(2, username);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean promijeniLozinkuPoId(int idKorisnik, String novaLozinka) {
        String sql = "UPDATE korisnik SET password_hash = ? WHERE id_korisnik = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, novaLozinka);
            stmt.setInt(2, idKorisnik);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void azurirajProfil(Korisnik k) {
        String sql = "UPDATE korisnik SET ime=?, prezime=?, password_hash=?, email=? WHERE id_korisnik=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, k.getIme());
            stmt.setString(2, k.getPrezime());
            stmt.setString(3, k.getPasswordHash());
            stmt.setString(4, k.getEmail());
            stmt.setInt(5, k.getIdKorisnik());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }



    public boolean emailPostoji(String email) {
        String sql = "SELECT 1 FROM korisnik WHERE email=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            return stmt.executeQuery().next();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void spasiResetToken(String email, String token) {
        String sql = "UPDATE korisnik SET reset_token=? WHERE email=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, token);
            stmt.setString(2, email);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean provjeriToken(String email, String token) {
        String sql = "SELECT 1 FROM korisnik WHERE email=? AND reset_token=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            stmt.setString(2, token);
            return stmt.executeQuery().next();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void resetujLozinku(String email, String noviPasswordHash) {
        String sql = "UPDATE korisnik SET password_hash=?, reset_token=NULL WHERE email=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, noviPasswordHash);
            stmt.setString(2, email);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateZadnjaPrijava(int idKorisnik) {
        String sql = "UPDATE korisnik SET zadnja_prijava = NOW() WHERE id_korisnik = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idKorisnik);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
