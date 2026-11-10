package com.gsecp.common;

import com.gsecp.security.UserPrincipal;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 数据级权限：教师只能管理自己的班级，学生只能访问已加入班级的内容。
 */
@Service
public class Authz {

    private final JdbcTemplate jdbc;

    public Authz(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean isAdmin(UserPrincipal user) {
        return user != null && user.getRoles().contains("ADMIN");
    }

    public boolean isStudent(UserPrincipal user) {
        return user != null && user.getRoles().contains("STUDENT");
    }

    public void requireClassManager(UserPrincipal user, long classId) {
        if (isAdmin(user)) {
            return;
        }
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM teaching_class WHERE id = ? AND teacher_id = ? AND is_deleted = 0",
                Integer.class, classId, user.getUserId());
        if (count == null || count == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能管理自己创建的班级");
        }
    }

    public void requireClassMember(UserPrincipal user, long classId) {
        if (isAdmin(user)) {
            return;
        }
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM class_member WHERE class_id = ? AND student_id = ? " +
                        "AND status = 'ACTIVE' AND is_deleted = 0",
                Integer.class, classId, user.getUserId());
        if (count == null || count == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "你不在该班级中");
        }
    }

    public long classIdOfCourse(long courseId) {
        return scalar("SELECT class_id FROM course WHERE id = ? AND is_deleted = 0", courseId);
    }

    public long classIdOfStep(long stepId) {
        return scalar("SELECT c.class_id FROM step s JOIN course c ON c.id = s.course_id " +
                "WHERE s.id = ? AND s.is_deleted = 0", stepId);
    }

    public long classIdOfSubmission(long submissionId) {
        return scalar("SELECT c.class_id FROM submission sub JOIN step s ON s.id = sub.step_id " +
                "JOIN course c ON c.id = s.course_id WHERE sub.id = ? AND sub.is_deleted = 0", submissionId);
    }

    public boolean isClassTeacher(UserPrincipal user, long classId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM teaching_class WHERE id = ? AND teacher_id = ? AND is_deleted = 0",
                Integer.class, classId, user.getUserId());
        return count != null && count > 0;
    }

    public void requireClassVisible(UserPrincipal user, long classId) {
        if (isAdmin(user) || isClassTeacher(user, classId)) {
            return;
        }
        requireClassMember(user, classId);
    }

    public void requireCourseVisible(UserPrincipal user, long courseId) {
        if (isAdmin(user)) {
            return;
        }
        long classId = classIdOfCourse(courseId);
        if (isClassTeacher(user, classId)) {
            return;
        }
        requireClassMember(user, classId);
    }

    public void requireStepVisible(UserPrincipal user, long stepId) {
        if (isAdmin(user)) {
            return;
        }
        long classId = classIdOfStep(stepId);
        if (isClassTeacher(user, classId)) {
            return;
        }
        requireClassMember(user, classId);
    }

    public void requireCourseManager(UserPrincipal user, long courseId) {
        requireClassManager(user, classIdOfCourse(courseId));
    }

    public void requireStepManager(UserPrincipal user, long stepId) {
        requireClassManager(user, classIdOfStep(stepId));
    }

    public void requireCourseMember(UserPrincipal user, long courseId) {
        requireClassMember(user, classIdOfCourse(courseId));
    }

    public void requireStepMember(UserPrincipal user, long stepId) {
        requireClassMember(user, classIdOfStep(stepId));
    }

    private long scalar(String sql, Object... args) {
        try {
            Long value = jdbc.queryForObject(sql, Long.class, args);
            if (value == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在");
            }
            return value;
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException(ResultCode.NOT_FOUND, "资源不存在");
        }
    }
}
