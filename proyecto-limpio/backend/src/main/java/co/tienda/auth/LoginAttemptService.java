package co.tienda.auth;

import co.tienda.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

/** Bloquea la cuenta tras varios intentos fallidos consecutivos. */
@Service
public class LoginAttemptService {

    private final JdbcTemplate jdbcTemplate;
    private final int maxAttempts;
    private final Duration lockDuration;

    public LoginAttemptService(JdbcTemplate jdbcTemplate,
                               @Value("${auth.lockout.max-attempts}") int maxAttempts,
                               @Value("${auth.lockout.duration-minutes}") long lockMinutes) {
        this.jdbcTemplate = jdbcTemplate;
        this.maxAttempts = maxAttempts;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
    }

    public boolean isLocked(User user) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now());
    }

    public void registerFailure(String username) {
        jdbcTemplate.update(
                "UPDATE users SET failed_attempts = failed_attempts + 1, "
                        + "locked_until = CASE WHEN failed_attempts + 1 >= ? THEN ? ELSE locked_until END "
                        + "WHERE username = ?",
                maxAttempts, Timestamp.from(Instant.now().plus(lockDuration)), username);
    }

    public void registerSuccess(String username) {
        jdbcTemplate.update(
                "UPDATE users SET failed_attempts = 0, locked_until = NULL WHERE username = ?", username);
    }
}
