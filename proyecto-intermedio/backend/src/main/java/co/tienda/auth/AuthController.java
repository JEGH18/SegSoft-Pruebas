package co.tienda.auth;

import co.tienda.user.User;
import co.tienda.user.UserRepository;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MfaPolicy mfaPolicy;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          JwtService jwtService, MfaPolicy mfaPolicy) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mfaPolicy = mfaPolicy;
    }

    @PostMapping("/login") @RateLimiter(name = "auth")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String rawPassword = body.get("password");

        User user = userRepository.findByUsername(username);
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            log.warn("Inicio de sesion fallido usuario={}", username);
            return ResponseEntity.status(401).body(Map.of("error", "Credenciales invalidas"));
        }
        if (mfaPolicy.requiresMfa(user) && !mfaPolicy.isValidCode(user, body.get("otp"))) {
            return ResponseEntity.status(401).body(Map.of("error", "Se requiere un codigo MFA valido"));
        }

        log.info("Inicio de sesion exitoso usuario={}", username);
        return ResponseEntity.ok(Map.of("token", jwtService.generateToken(user)));
    }

    @PostMapping("/register") @RateLimiter(name = "auth")
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String rawPassword = body.get("password");

        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "La contrasena debe tener al menos 12 caracteres"));
        }

        User user = new User();
        user.setUsername(username);
        user.setRole("CUSTOMER");
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Usuario creado"));
    }
}
