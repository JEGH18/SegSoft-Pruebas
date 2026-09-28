package co.tiendavuln.order;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final JdbcTemplate jdbcTemplate;

    public OrderController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Busqueda avanzada de pedidos desde el panel de administracion.
    @GetMapping("/search")
    public List<Map<String, Object>> search(HttpServletRequest request) {
        return new OrderSearch(request.getParameter("filtro")).query(jdbcTemplate);
    }
}
