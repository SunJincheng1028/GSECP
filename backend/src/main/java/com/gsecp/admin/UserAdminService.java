package com.gsecp.admin;

import com.gsecp.common.BusinessException;
import com.gsecp.common.ResultCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserAdminService {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public UserAdminService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserAdminDtos.UserView> list() {
        return jdbc.query(
                "SELECT u.id, u.username, u.real_name, u.user_no, u.status, u.created_at, " +
                        "GROUP_CONCAT(r.code ORDER BY r.code SEPARATOR ',') AS role_codes " +
                        "FROM sys_user u " +
                        "LEFT JOIN sys_user_role ur ON ur.user_id = u.id AND ur.is_deleted = 0 " +
                        "LEFT JOIN sys_role r ON r.id = ur.role_id AND r.is_deleted = 0 " +
                        "WHERE u.is_deleted = 0 " +
                        "GROUP BY u.id, u.username, u.real_name, u.user_no, u.status, u.created_at " +
                        "ORDER BY u.id",
                (rs, rowNum) -> new UserAdminDtos.UserView(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("real_name"),
                        rs.getString("user_no"),
                        rs.getString("role_codes") == null ? "" : rs.getString("role_codes"),
                        rs.getString("status"),
                        rs.getString("created_at")));
    }

    @Transactional
    public void create(UserAdminDtos.CreateUserRequest request) {
        String role = request.role().toUpperCase();
        if (!"ADMIN".equals(role) && !"TEACHER".equals(role)) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "此处仅支持创建管理员或教师账号；学生请凭邀请码自行注册");
        }
        Integer exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE username = ?", Integer.class, request.username());
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "用户名已被占用");
        }
        jdbc.update(
                "INSERT INTO sys_user (username, password_hash, real_name, user_no, status) " +
                        "VALUES (?, ?, ?, ?, 'ACTIVE')",
                request.username(), passwordEncoder.encode(request.password()),
                request.realName(), request.userNo());
        Long userId = jdbc.queryForObject(
                "SELECT id FROM sys_user WHERE username = ?", Long.class, request.username());
        jdbc.update(
                "INSERT INTO sys_user_role (user_id, role_id) SELECT ?, id FROM sys_role WHERE code = ?",
                userId, role);
    }

    public void updateStatus(long userId, String status) {
        if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态值不合法");
        }
        int rows = jdbc.update(
                "UPDATE sys_user SET status = ? WHERE id = ? AND is_deleted = 0", status, userId);
        if (rows == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
    }

    public void resetPassword(long userId, String password) {
        int rows = jdbc.update(
                "UPDATE sys_user SET password_hash = ? WHERE id = ? AND is_deleted = 0",
                passwordEncoder.encode(password), userId);
        if (rows == 0) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
    }
}
