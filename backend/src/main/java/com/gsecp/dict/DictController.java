package com.gsecp.dict;

import com.gsecp.common.R;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "字典")
@RestController
@RequestMapping("/api/dict")
public class DictController {

    private final JdbcTemplate jdbc;

    public DictController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record DeliverableType(
            String code,
            String name,
            String frontendComponent,
            String backendHandler,
            Boolean enabled,
            Integer sortOrder,
            Integer builtIn
    ) {
    }

    public record UpdateEnabledRequest(Boolean enabled) {
    }

    @GetMapping("/deliverable-types")
    public R<List<DeliverableType>> types() {
        return R.ok(jdbc.query(
                "SELECT code, name, frontend_component, backend_handler, enabled, sort_order, 1 AS built_in " +
                        "FROM dict_deliverable_type ORDER BY sort_order",
                (rs, rowNum) -> new DeliverableType(
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("frontend_component"),
                        rs.getString("backend_handler"),
                        rs.getBoolean("enabled"),
                        rs.getInt("sort_order"),
                        rs.getInt("built_in"))));
    }

    @PatchMapping("/deliverable-types/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> updateEnabled(@PathVariable String code,
                                 @RequestBody UpdateEnabledRequest request) {
        int rows = jdbc.update(
                "UPDATE dict_deliverable_type SET enabled = ? WHERE code = ?",
                Boolean.TRUE.equals(request.enabled()) ? 1 : 0, code);
        if (rows == 0) {
            return R.fail(com.gsecp.common.ResultCode.NOT_FOUND, "交付物类型不存在");
        }
        return R.ok();
    }
}
