package co.tiendavuln.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final String secret = "TiendaVuln2024SuperSecretKey";

    public String generateToken(String username) {
        return JWT.create()
                .withSubject(username)
                .withClaim("role", "CUSTOMER")
                .sign(Algorithm.HMAC256(secret));
    }

    public String verify(String token) {
        return JWT.require(Algorithm.HMAC256(secret)).build().verify(token).getSubject();
    }
}
