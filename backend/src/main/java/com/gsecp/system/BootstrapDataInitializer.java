package com.gsecp.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 首次启动创建内置管理员账号（密码使用 BCrypt 加密后写入，不在 SQL 中硬编码）。
 * 迭代 1 实现用户管理接口后，可关闭 gsecp.bootstrap.enabled。
 */
@Component
@ConditionalOnProperty(prefix = "gsecp.bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BootstrapDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapDataInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${gsecp.bootstrap.admin-username:admin}")
    private String adminUsername;

    @Value("${gsecp.bootstrap.admin-password:Admin@123456}")
    private String adminPassword;

    public BootstrapDataInitializer(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE username = ?", Integer.class, adminUsername);
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.update(
                "INSERT INTO sys_user (username, password_hash, real_name, status) VALUES (?, ?, ?, ?)",
                adminUsername, passwordEncoder.encode(adminPassword), "系统管理员", "ACTIVE");

        Long userId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_user WHERE username = ?", Long.class, adminUsername);
        jdbcTemplate.update(
                "INSERT INTO sys_user_role (user_id, role_id) SELECT ?, id FROM sys_role WHERE code = 'ADMIN'",
                userId);

        log.warn("已创建初始管理员账号 [{}]，请通过环境变量 GSECP_ADMIN_PASSWORD 修改默认密码", adminUsername);
    }
}
