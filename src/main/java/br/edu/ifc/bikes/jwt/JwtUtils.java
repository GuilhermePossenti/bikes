package br.edu.ifc.bikes.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtUtils {

    public static final String JWT_BEARER = "Bearer ";
    public static final String JWT_AUTHORIZATION = "Authorization";

    private final SecretKey key;
    private final long expiraEmMinutos;

    //a chave precisa ter no mínimo 32 caracteres (256 bits) para o algoritmo HS256
    public JwtUtils(@Value("${jwt.secret}") String secret,
                    @Value("${jwt.expiration-minutes:30}") long expiraEmMinutos) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiraEmMinutos = expiraEmMinutos;
    }

    public JwtToken createToken(String username, String role) {
        Instant agora = Instant.now();
        String token = Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(expiraEmMinutos, ChronoUnit.MINUTES)))
                .claim("role", role)
                .signWith(key)
                .compact();
        return new JwtToken(token);
    }

    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(refactorToken(token))
                .getPayload();
    }

    //remove o prefixo "Bearer " caso ele tenha sido enviado junto
    private String refactorToken(String token) {
        if (token != null && token.startsWith(JWT_BEARER)) {
            return token.substring(JWT_BEARER.length());
        }
        return token;
    }
}
