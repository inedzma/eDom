package ba.unze.edom.server.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey kljuc;
    private final long trajanjeSati;

    public JwtService(@Value("${edom.jwt.secret}") String secret,
                      @Value("${edom.jwt.trajanje-sati}") long trajanjeSati) {
        this.kljuc = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.trajanjeSati = trajanjeSati;
    }

    public String generisi(String username) {
        Instant sada = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(sada))
                .expiration(Date.from(sada.plus(trajanjeSati, ChronoUnit.HOURS)))
                .signWith(kljuc)
                .compact();
    }

    public String izvuciUsername(String token) {
        return Jwts.parser().verifyWith(kljuc).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    public boolean validan(String token) {
        try {
            Jwts.parser().verifyWith(kljuc).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}