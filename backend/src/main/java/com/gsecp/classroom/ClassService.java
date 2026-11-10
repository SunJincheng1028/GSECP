package com.gsecp.classroom;

import com.gsecp.common.Authz;
import com.gsecp.common.BusinessException;
import com.gsecp.common.ResultCode;
import com.gsecp.security.UserPrincipal;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ClassService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate jdbc;
    private final Authz authz;

    private final RowMapper<ClassDtos.ClassView> classMapper = (rs, rowNum) -> new ClassDtos.ClassView(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getLong("teacher_id"),
            rs.getString("teacher_name"),
            rs.getString("status"),
            rs.getInt("student_count"),
            rs.getInt("course_count"),
            rs.getString("created_at"));

    private final RowMapper<ClassDtos.InviteCodeView> inviteMapper = (rs, rowNum) -> new ClassDtos.InviteCodeView(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("expire_at"),
            rs.getInt("max_uses"),
            rs.getInt("used_count"),
            rs.getBoolean("enabled"),
            rs.getString("created_at"));

    public ClassService(JdbcTemplate jdbc, Authz authz) {
        this.jdbc = jdbc;
        this.authz = authz;
    }

    private static final String BASE_SELECT =
            "SELECT c.id, c.name, c.description, c.teacher_id, u.real_name AS teacher_name, c.status, " +
                    "c.created_at, " +
                    "(SELECT COUNT(*) FROM class_member m WHERE m.class_id = c.id " +
                    "  AND m.status = 'ACTIVE' AND m.is_deleted = 0) AS student_count, " +
                    "(SELECT COUNT(*) FROM course co WHERE co.class_id = c.id AND co.is_deleted = 0) AS course_count " +
                    "FROM teaching_class c JOIN sys_user u ON u.id = c.teacher_id " +
                    "WHERE c.is_deleted = 0";

    public List<ClassDtos.ClassView> list(UserPrincipal user) {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        List<Object> args = new ArrayList<>();
        if (authz.isAdmin(user)) {
            // 管理员查看全部
        } else if (user.getRoles().contains("TEACHER")) {
            sql.append(" AND c.teacher_id = ?");
            args.add(user.getUserId());
        } else {
            sql.append(" AND EXISTS (SELECT 1 FROM class_member m WHERE m.class_id = c.id " +
                    "AND m.student_id = ? AND m.status = 'ACTIVE' AND m.is_deleted = 0)");
            args.add(user.getUserId());
        }
        sql.append(" ORDER BY c.id DESC");
        return jdbc.query(sql.toString(), classMapper, args.toArray());
    }

    public ClassDtos.ClassView detail(UserPrincipal user, long classId) {
        authz.requireClassVisible(user, classId);
        return findVisible(user, classId);
    }

    private ClassDtos.ClassView findVisible(UserPrincipal user, long classId) {
        try {
            return jdbc.queryForObject(BASE_SELECT + " AND c.id = ?", classMapper, classId);
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException(ResultCode.NOT_FOUND, "班级不存在");
        }
    }

    public void create(UserPrincipal user, ClassDtos.CreateClassRequest request) {
        jdbc.update(
                "INSERT INTO teaching_class (name, teacher_id, description, status) VALUES (?, ?, ?, 'ACTIVE')",
                request.name(), user.getUserId(), request.description());
    }

    public List<ClassDtos.MemberView> members(UserPrincipal user, long classId) {
        authz.requireClassManager(user, classId);
        return jdbc.query(
                "SELECT u.id AS student_id, u.username, u.real_name, u.user_no, m.status, m.joined_at " +
                        "FROM class_member m JOIN sys_user u ON u.id = m.student_id " +
                        "WHERE m.class_id = ? AND m.is_deleted = 0 ORDER BY m.joined_at DESC",
                (rs, rowNum) -> new ClassDtos.MemberView(
                        rs.getLong("student_id"),
                        rs.getString("username"),
                        rs.getString("real_name"),
                        rs.getString("user_no"),
                        rs.getString("status"),
                        rs.getString("joined_at")),
                classId);
    }

    public void removeMember(UserPrincipal user, long classId, long studentId) {
        authz.requireClassManager(user, classId);
        jdbc.update(
                "UPDATE class_member SET status = 'REMOVED' WHERE class_id = ? AND student_id = ?",
                classId, studentId);
    }

    public List<ClassDtos.InviteCodeView> inviteCodes(UserPrincipal user, long classId) {
        authz.requireClassManager(user, classId);
        return jdbc.query(
                "SELECT id, code, expire_at, max_uses, used_count, enabled, created_at " +
                        "FROM invite_code WHERE class_id = ? AND is_deleted = 0 ORDER BY id DESC",
                inviteMapper, classId);
    }

    public String createInviteCode(UserPrincipal user, long classId,
                                   ClassDtos.CreateInviteCodeRequest request) {
        authz.requireClassManager(user, classId);
        int expireDays = request.expireDays() == null ? 30 : Math.max(0, request.expireDays());
        int maxUses = request.maxUses() == null ? 0 : Math.max(0, request.maxUses());
        String expireAt = expireDays > 0
                ? LocalDateTime.now().plusDays(expireDays).format(FORMATTER) : null;

        String code = generateCode();
        jdbc.update(
                "INSERT INTO invite_code (class_id, code, expire_at, max_uses, created_by) VALUES (?, ?, ?, ?, ?)",
                classId, code, expireAt, maxUses, user.getUserId());
        return code;
    }

    public void updateInviteCode(UserPrincipal user, long classId, long codeId, boolean enabled) {
        authz.requireClassManager(user, classId);
        jdbc.update("UPDATE invite_code SET enabled = ? WHERE id = ? AND class_id = ?",
                enabled ? 1 : 0, codeId, classId);
    }

    private String generateCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder builder = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
            }
            String code = builder.toString();
            Integer exists = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM invite_code WHERE code = ?", Integer.class, code);
            if (exists == null || exists == 0) {
                return code;
            }
        }
        throw new BusinessException(ResultCode.INTERNAL_ERROR, "邀请码生成失败，请重试");
    }
}
