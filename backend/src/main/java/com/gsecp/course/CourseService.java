package com.gsecp.course;

import com.gsecp.common.Authz;
import com.gsecp.common.BusinessException;
import com.gsecp.common.ResultCode;
import com.gsecp.security.UserPrincipal;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    private final JdbcTemplate jdbc;
    private final Authz authz;

    private final RowMapper<CourseDtos.CourseView> mapper = (rs, rowNum) -> new CourseDtos.CourseView(
            rs.getLong("id"),
            rs.getLong("class_id"),
            rs.getString("class_name"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("status"),
            rs.getString("start_at"),
            rs.getString("end_at"),
            rs.getInt("step_count"),
            rs.getString("created_at"));

    private static final String BASE_SELECT =
            "SELECT co.id, co.class_id, tc.name AS class_name, co.name, co.description, co.status, " +
                    "co.start_at, co.end_at, co.created_at, " +
                    "(SELECT COUNT(*) FROM step s WHERE s.course_id = co.id AND s.is_deleted = 0) AS step_count " +
                    "FROM course co JOIN teaching_class tc ON tc.id = co.class_id " +
                    "WHERE co.is_deleted = 0";

    public CourseService(JdbcTemplate jdbc, Authz authz) {
        this.jdbc = jdbc;
        this.authz = authz;
    }

    public List<CourseDtos.CourseView> list(UserPrincipal user, long classId) {
        authz.requireClassVisible(user, classId);
        boolean manager = authz.isAdmin(user) || authz.isClassTeacher(user, classId);
        String filter = manager ? "" : " AND co.status <> 'DRAFT'";
        return jdbc.query(BASE_SELECT + " AND co.class_id = ?" + filter + " ORDER BY co.id DESC",
                mapper, classId);
    }

    public List<CourseDtos.CourseView> listForMember(UserPrincipal user, long classId) {
        authz.requireClassMember(user, classId);
        return jdbc.query(BASE_SELECT + " AND co.class_id = ? AND co.status <> 'DRAFT' ORDER BY co.id DESC",
                mapper, classId);
    }

    public CourseDtos.CourseView detail(UserPrincipal user, long courseId) {
        authz.requireCourseVisible(user, courseId);
        try {
            return jdbc.queryForObject(BASE_SELECT + " AND co.id = ?", mapper, courseId);
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程不存在");
        }
    }

    public void create(UserPrincipal user, long classId, CourseDtos.CreateCourseRequest request) {
        authz.requireClassManager(user, classId);
        jdbc.update(
                "INSERT INTO course (class_id, teacher_id, name, description, status) VALUES (?, ?, ?, ?, 'DRAFT')",
                classId, user.getUserId(), request.name(), request.description());
    }

    public void updateStatus(UserPrincipal user, long courseId, String status) {
        authz.requireCourseManager(user, courseId);
        if (!"DRAFT".equals(status) && !"PUBLISHED".equals(status) && !"ARCHIVED".equals(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "课程状态不合法");
        }
        jdbc.update("UPDATE course SET status = ? WHERE id = ?", status, courseId);
    }
}
