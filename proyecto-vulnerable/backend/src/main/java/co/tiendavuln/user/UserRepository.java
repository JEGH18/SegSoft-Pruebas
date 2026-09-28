package co.tiendavuln.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = '" + username + "'";
        List<User> users = jdbcTemplate.query(sql, (rs, i) -> {
            User u = new User();
            u.setUsername(rs.getString("username"));
            u.setPassword(rs.getString("password"));
            u.setCardNumber(rs.getString("card_number"));
            return u;
        });
        return users.isEmpty() ? null : users.get(0);
    }

    public void save(User user) {
        jdbcTemplate.update(
                "INSERT INTO users (username, password, card_number) VALUES ('"
                        + user.getUsername() + "', '" + user.getPassword() + "', '" + user.getCardNumber() + "')");
    }
}
