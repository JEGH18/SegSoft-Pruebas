package co.tiendavuln.order;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

public class OrderSearch {

    private final String filtro;

    public OrderSearch(String filtro) {
        this.filtro = filtro;
    }

    public List<Map<String, Object>> query(JdbcTemplate jdbcTemplate) {
        String sql = "SELECT * FROM orders WHERE " + filtro + " ORDER BY created_at DESC";
        return jdbcTemplate.queryForList(sql);
    }
}
