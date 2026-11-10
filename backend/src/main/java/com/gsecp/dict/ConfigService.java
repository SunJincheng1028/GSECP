package com.gsecp.dict;

import com.gsecp.common.BusinessException;
import com.gsecp.common.ResultCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfigService {

    private final JdbcTemplate jdbc;

    public ConfigService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record ConfigItem(String key, String value, String description) {
    }

    public List<ConfigItem> list() {
        return jdbc.query(
                "SELECT config_key, config_value, description FROM sys_config ORDER BY config_key",
                (rs, rowNum) -> new ConfigItem(
                        rs.getString("config_key"),
                        rs.getString("config_value"),
                        rs.getString("description")));
    }

    public void update(String key, String value) {
        int rows = jdbc.update(
                "UPDATE sys_config SET config_value = ? WHERE config_key = ?", value, key);
        if (rows == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置项不存在");
        }
    }

    public int maxUploadSizeMb() {
        try {
            String value = jdbc.queryForObject(
                    "SELECT config_value FROM sys_config WHERE config_key = 'file.max_size_mb'",
                    String.class);
            int size = Integer.parseInt(value);
            return size > 0 ? size : 50;
        } catch (Exception e) {
            return 50;
        }
    }
}
