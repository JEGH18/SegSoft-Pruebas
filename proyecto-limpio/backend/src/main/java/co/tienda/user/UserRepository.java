package co.tienda.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public User findByUsername(String username) {
        List<User> users = jdbcTemplate.query(
                "SELECT username, password_hash, role, totp_secret, failed_attempts, locked_until "
                        + "FROM users WHERE username = ?",
                (rs, i) -> {
                    User u = new User();
                    u.setUsername(rs.getString("username"));
                    u.setPasswordHash(rs.getString("password_hash"));
                    u.setRole(rs.getString("role"));
                    u.setTotpSecret(rs.getString("totp_secret"));
                    u.setFailedAttempts(rs.getInt("failed_attempts"));
                    Timestamp lockedUntil = rs.getTimestamp("locked_until");
                    u.setLockedUntil(lockedUntil != null ? lockedUntil.toInstant() : null);
                    return u;
                },
                username);
        return users.isEmpty() ? null : users.get(0);
    }

    public void save(User user) {
        jdbcTemplate.update(
                "INSERT INTO users (username, password_hash, role, failed_attempts) VALUES (?, ?, ?, 0)",
                user.getUsername(), user.getPasswordHash(), user.getRole());
    }
}
