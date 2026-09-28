package co.tienda.order;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final JdbcTemplate jdbcTemplate;

    public OrderController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Solo devuelve pedidos del usuario autenticado.
    @GetMapping("/search")
    public List<Map<String, Object>> search(Principal principal, @RequestParam OrderStatus status) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM orders WHERE customer_username = ? AND status = ? ORDER BY created_at DESC",
                principal.getName(), status.name());
    }
}
