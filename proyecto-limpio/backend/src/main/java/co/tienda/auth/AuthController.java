package co.tienda.auth;

import co.tienda.security.JwtCookieAuthFilter;
import co.tienda.user.User;
import co.tienda.user.UserRepository;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final Map<String, String> INVALID_CREDENTIALS = Map.of("error", "Credenciales invalidas");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MfaPolicy mfaPolicy;
    private final LoginAttemptService loginAttempts;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                          MfaPolicy mfaPolicy, LoginAttemptService loginAttempts) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mfaPolicy = mfaPolicy;
        this.loginAttempts = loginAttempts;
    }

    @PostMapping("/login") @RateLimiter(name = "auth")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String rawPassword = body.get("password");

        User user = userRepository.findByUsername(username);
        if (user == null || loginAttempts.isLocked(user)) {
            log.warn("Inicio de sesion rechazado usuario={}", username);
            return ResponseEntity.status(401).body(INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())
                || (mfaPolicy.requiresMfa(user) && !mfaPolicy.isValidCode(user, body.get("otp")))) {
            loginAttempts.registerFailure(username);
            log.warn("Inicio de sesion fallido usuario={}", username);
            return ResponseEntity.status(401).body(INVALID_CREDENTIALS);
        }

        loginAttempts.registerSuccess(username);
        log.info("Inicio de sesion exitoso usuario={}", username);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie(jwtService.generateToken(user), JwtService.TOKEN_TTL))
                .body(Map.of("username", user.getUsername(), "role", user.getRole()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, sessionCookie("", Duration.ZERO))
                .build();
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

    private static String sessionCookie(String value, Duration maxAge) {
        return ResponseCookie.from(JwtCookieAuthFilter.COOKIE_NAME, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api")
                .maxAge(maxAge)
                .build()
                .toString();
    }
}
