package com.gsecp.dict;

import com.gsecp.common.R;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "系统配置")
@RestController
@RequestMapping("/api/admin/config")
@PreAuthorize("hasRole('ADMIN')")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    public record UpdateConfigRequest(String value) {
    }

    @GetMapping
    public R<List<ConfigService.ConfigItem>> list() {
        return R.ok(configService.list());
    }

    @PutMapping("/{key}")
    public R<Void> update(@PathVariable String key, @RequestBody UpdateConfigRequest request) {
        configService.update(key, request.value());
        return R.ok();
    }
}
