package com.gsecp.score;

import com.gsecp.common.Authz;
import com.gsecp.common.BusinessException;
import com.gsecp.common.ResultCode;
import com.gsecp.security.UserPrincipal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScoreService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final JdbcTemplate jdbc;
    private final Authz authz;

    private record Student(long id, String username, String realName, String userNo) {
    }

    private record ScoreCell(long studentId, long stepId, long submissionId,
                             BigDecimal score, String comment) {
    }

    public ScoreService(JdbcTemplate jdbc, Authz authz) {
        this.jdbc = jdbc;
        this.authz = authz;
    }

    @Transactional
    public void grade(UserPrincipal user, long submissionId, ScoreDtos.GradeRequest request) {
        authz.requireClassManager(user, authz.classIdOfSubmission(submissionId));
        BigDecimal score = request.totalScore();
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(HUNDRED) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "分数需介于 0~100 之间");
        }
        Integer latest = jdbc.queryForObject(
                "SELECT COUNT(*) FROM submission WHERE id = ? AND is_latest = 1 AND is_deleted = 0",
                Integer.class, submissionId);
        if (latest == null || latest == 0) {
            throw new BusinessException(ResultCode.CONFLICT, "只能批改学生的最新一次提交");
        }
        jdbc.update(
                "INSERT INTO score (submission_id, teacher_id, total_score, comment, graded_at) " +
                        "VALUES (?, ?, ?, ?, NOW()) " +
                        "ON DUPLICATE KEY UPDATE teacher_id = VALUES(teacher_id), " +
                        "total_score = VALUES(total_score), comment = VALUES(comment), " +
                        "graded_at = NOW(), is_deleted = 0",
                submissionId, user.getUserId(), score, request.comment());
        jdbc.update("UPDATE submission SET status = 'GRADED' WHERE id = ?", submissionId);
    }

    public ScoreDtos.GradeReport report(UserPrincipal user, long courseId) {
        authz.requireCourseManager(user, courseId);
        List<Student> students = jdbc.query(
                "SELECT u.id, u.username, u.real_name, u.user_no FROM class_member m " +
                        "JOIN sys_user u ON u.id = m.student_id " +
                        "JOIN course co ON co.class_id = m.class_id " +
                        "WHERE co.id = ? AND m.status = 'ACTIVE' AND m.is_deleted = 0 " +
                        "ORDER BY u.id",
                (rs, rowNum) -> new Student(
                        rs.getLong("id"), rs.getString("username"),
                        rs.getString("real_name"), rs.getString("user_no")),
                courseId);
        return buildReport(courseId, students, null);
    }

    public ScoreDtos.GradeReport myReport(UserPrincipal user, long courseId) {
        authz.requireCourseMember(user, courseId);
        List<Student> students = jdbc.query(
                "SELECT id, username, real_name, user_no FROM sys_user WHERE id = ?",
                (rs, rowNum) -> new Student(
                        rs.getLong("id"), rs.getString("username"),
                        rs.getString("real_name"), rs.getString("user_no")),
                user.getUserId());
        return buildReport(courseId, students, user.getUserId());
    }

    private ScoreDtos.GradeReport buildReport(long courseId, List<Student> students, Long onlyStudentId) {
        String courseName;
        try {
            courseName = jdbc.queryForObject(
                    "SELECT name FROM course WHERE id = ? AND is_deleted = 0", String.class, courseId);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程不存在");
        }

        List<ScoreDtos.StepColumn> columns = jdbc.query(
                "SELECT id, title, weight FROM step WHERE course_id = ? AND is_deleted = 0 " +
                        "AND status <> 'DRAFT' ORDER BY sort_order, id",
                (rs, rowNum) -> new ScoreDtos.StepColumn(
                        rs.getLong("id"), rs.getString("title"), rs.getBigDecimal("weight")),
                courseId);

        String studentFilter = onlyStudentId == null ? "" : " AND sub.student_id = " + onlyStudentId;
        List<ScoreCell> cells = jdbc.query(
                "SELECT sub.student_id, sub.step_id, sub.id AS submission_id, " +
                        "sc.total_score, sc.comment " +
                        "FROM submission sub " +
                        "JOIN step s ON s.id = sub.step_id AND s.course_id = ? AND s.is_deleted = 0 " +
                        "JOIN score sc ON sc.submission_id = sub.id AND sc.is_deleted = 0 " +
                        "WHERE sub.is_latest = 1 AND sub.is_deleted = 0" + studentFilter,
                (rs, rowNum) -> new ScoreCell(
                        rs.getLong("student_id"), rs.getLong("step_id"),
                        rs.getLong("submission_id"), rs.getBigDecimal("total_score"),
                        rs.getString("comment")),
                courseId);

        Map<String, ScoreCell> cellMap = new HashMap<>();
        for (ScoreCell cell : cells) {
            cellMap.put(cell.studentId() + ":" + cell.stepId(), cell);
        }

        List<ScoreDtos.StudentGradeRow> rows = new ArrayList<>();
        for (Student student : students) {
            List<ScoreDtos.StepGrade> stepGrades = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal gradedWeight = BigDecimal.ZERO;
            for (ScoreDtos.StepColumn column : columns) {
                ScoreCell cell = cellMap.get(student.id() + ":" + column.stepId());
                BigDecimal weight = column.weight() == null ? BigDecimal.ZERO : column.weight();
                BigDecimal score = cell == null ? null : cell.score();
                if (score != null) {
                    total = total.add(score.multiply(weight).divide(HUNDRED, 2, RoundingMode.HALF_UP));
                    gradedWeight = gradedWeight.add(weight);
                }
                stepGrades.add(new ScoreDtos.StepGrade(
                        column.stepId(), column.title(), weight, score,
                        cell == null ? null : cell.comment(),
                        cell == null ? null : cell.submissionId()));
            }
            rows.add(new ScoreDtos.StudentGradeRow(
                    student.id(), student.username(), student.realName(), student.userNo(),
                    stepGrades, total, gradedWeight));
        }
        return new ScoreDtos.GradeReport(courseId, courseName, columns, rows);
    }
}
