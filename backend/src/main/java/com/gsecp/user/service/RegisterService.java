package com.gsecp.user.service;

import com.gsecp.common.BusinessException;
import com.gsecp.common.ResultCode;
import com.gsecp.config.JwtProperties;
import com.gsecp.security.JwtTokenProvider;
import com.gsecp.user.dto.AuthDtos;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RegisterService {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;

    public RegisterService(JdbcTemplate jdbc,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider tokenProvider,
                           JwtProperties jwtProperties) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.jwtProperties = jwtProperties;
    }

    private record Invite(long id, long classId, String expireAt, int maxUses,
                          int usedCount, boolean enabled, String classStatus) {
    }

    @Transactional
    public AuthDtos.LoginResponse register(AuthDtos.RegisterRequest request) {
        Integer exists = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE username = ?", Integer.class, request.username());
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "用户名已被占用");
        }

        Invite invite;
        try {
            invite = jdbc.queryForObject(
                    "SELECT ic.id, ic.class_id, ic.expire_at, ic.max_uses, ic.used_count, ic.enabled, " +
                            "tc.status AS class_status " +
                            "FROM invite_code ic JOIN teaching_class tc ON tc.id = ic.class_id " +
                            "WHERE ic.code = ? AND ic.is_deleted = 0 AND tc.is_deleted = 0",
                    (rs, rowNum) -> new Invite(
                            rs.getLong("id"),
                            rs.getLong("class_id"),
                            rs.getString("expire_at"),
                            rs.getInt("max_uses"),
                            rs.getInt("used_count"),
                            rs.getBoolean("enabled"),
                            rs.getString("class_status")),
                    request.inviteCode().trim().toUpperCase());
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邀请码无效");
        }

        if (!invite.enabled() || !"ACTIVE".equals(invite.classStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邀请码已停用或班级已归档");
        }
        if (invite.expireAt() != null) {
            Integer expired = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM invite_code WHERE id = ? AND expire_at <= NOW()",
                    Integer.class, invite.id());
            if (expired != null && expired > 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "邀请码已过期");
            }
        }
        if (invite.maxUses() > 0 && invite.usedCount() >= invite.maxUses()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邀请码使用次数已达上限");
        }

        jdbc.update(
                "INSERT INTO sys_user (username, password_hash, real_name, user_no, status) " +
                        "VALUES (?, ?, ?, ?, 'ACTIVE')",
                request.username(), passwordEncoder.encode(request.password()),
                request.realName(), request.userNo());

        Long userId = jdbc.queryForObject(
                "SELECT id FROM sys_user WHERE username = ?", Long.class, request.username());
        jdbc.update(
                "INSERT INTO sys_user_role (user_id, role_id) SELECT ?, id FROM sys_role WHERE code = 'STUDENT'",
                userId);
        jdbc.update(
                "INSERT INTO class_member (class_id, student_id, invite_code_id, status) " +
                        "VALUES (?, ?, ?, 'ACTIVE') " +
                        "ON DUPLICATE KEY UPDATE status = 'ACTIVE', invite_code_id = VALUES(invite_code_id), " +
                        "is_deleted = 0",
                invite.classId(), userId, invite.id());
        jdbc.update("UPDATE invite_code SET used_count = used_count + 1 WHERE id = ?", invite.id());

        List<String> roles = List.of("STUDENT");
        String token = tokenProvider.generate(userId, request.username(), roles);
        AuthDtos.UserInfo userInfo = new AuthDtos.UserInfo(
                userId, request.username(), request.realName(), request.userNo(), roles);
        return new AuthDtos.LoginResponse(token, "Bearer", jwtProperties.expirationMs() / 1000, userInfo);
    }
}
