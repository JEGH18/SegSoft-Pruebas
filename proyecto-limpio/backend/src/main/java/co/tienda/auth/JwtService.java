package co.tienda.auth;

import co.tienda.user.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class JwtService {

    public static final Duration TOKEN_TTL = Duration.ofMinutes(15);

    private final Algorithm algorithm;

    public JwtService(@Value("${jwt.signing-key}") String signingKey) {
        this.algorithm = Algorithm.HMAC256(signingKey);
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        return JWT.create().withIssuedAt(now).withExpiresAt(now.plus(TOKEN_TTL))
                .withIssuer("tienda-backend")
                .withSubject(user.getUsername())
                .withClaim("role", user.getRole())
                .sign(algorithm);
    }

    /** Devuelve el usuario del JWT; lanza JWTVerificationException si es inválido o expiró. */
    public String verify(String jwt) {
        return JWT.require(algorithm).withIssuer("tienda-backend").build().verify(jwt).getSubject();
    }
}
