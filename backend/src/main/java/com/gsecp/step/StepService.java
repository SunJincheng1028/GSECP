package com.gsecp.step;

import com.gsecp.common.Authz;
import com.gsecp.common.BusinessException;
import com.gsecp.common.Dates;
import com.gsecp.common.JdbcIds;
import com.gsecp.common.ResultCode;
import com.gsecp.security.UserPrincipal;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class StepService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbc;
    private final Authz authz;

    private record StepRow(
            Long id, Long courseId, String title, String description, String requirement,
            BigDecimal weight, String publishAt, String dueAt, String status,
            Boolean allowResubmit, Integer sortOrder, Integer submissionCount, String myStatus
    ) {
    }

    private final RowMapper<StepRow> rowMapper = (rs, rowNum) -> new StepRow(
            rs.getLong("id"),
            rs.getLong("course_id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getString("requirement"),
            rs.getBigDecimal("weight"),
            rs.getString("publish_at"),
            rs.getString("due_at"),
            rs.getString("status"),
            rs.getBoolean("allow_resubmit"),
            rs.getInt("sort_order"),
            rs.getInt("submission_count"),
            rs.getString("my_status"));

    private final RowMapper<StepDtos.DeliverableItemView> itemMapper = (rs, rowNum) ->
            new StepDtos.DeliverableItemView(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("type_code"),
                    rs.getString("type_name"),
                    rs.getString("frontend_component"),
                    rs.getBoolean("required"),
                    rs.getString("constraint_json"),
                    rs.getInt("sort_order"));

    private static final String BASE_SELECT =
            "SELECT s.id, s.course_id, s.title, s.description, s.requirement, s.weight, " +
                    "s.publish_at, s.due_at, s.status, s.allow_resubmit, s.sort_order, " +
                    "(SELECT COUNT(*) FROM submission sub WHERE sub.step_id = s.id " +
                    "  AND sub.is_latest = 1 AND sub.is_deleted = 0) AS submission_count, " +
                    "(SELECT sub.status FROM submission sub WHERE sub.step_id = s.id " +
                    "  AND sub.student_id = ? AND sub.is_latest = 1 AND sub.is_deleted = 0 LIMIT 1) AS my_status " +
                    "FROM step s WHERE s.is_deleted = 0";

    public StepService(JdbcTemplate jdbc, Authz authz) {
        this.jdbc = jdbc;
        this.authz = authz;
    }

    public List<StepDtos.StepView> list(UserPrincipal user, long courseId) {
        authz.requireCourseVisible(user, courseId);
        boolean manager = authz.isAdmin(user) || user.getRoles().contains("TEACHER");
        String filter = manager ? "" : " AND s.status = 'PUBLISHED'";
        List<StepRow> rows = jdbc.query(
                BASE_SELECT + " AND s.course_id = ?" + filter + " ORDER BY s.sort_order, s.id",
                rowMapper, user.getUserId(), courseId);
        List<StepDtos.StepView> result = new ArrayList<>();
        for (StepRow row : rows) {
            result.add(toView(row));
        }
        return result;
    }

    public StepDtos.StepView detail(UserPrincipal user, long stepId) {
        authz.requireStepVisible(user, stepId);
        try {
            StepRow row = jdbc.queryForObject(BASE_SELECT + " AND s.id = ?",
                    rowMapper, user.getUserId(), stepId);
            return toView(row);
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException(ResultCode.NOT_FOUND, "步骤不存在");
        }
    }

    private StepDtos.StepView toView(StepRow row) {
        return new StepDtos.StepView(
                row.id(), row.courseId(), row.title(), row.description(), row.requirement(),
                row.weight(), row.publishAt(), row.dueAt(), row.status(), row.allowResubmit(),
                row.sortOrder(), row.submissionCount(), row.myStatus(), items(row.id()));
    }

    public List<StepDtos.DeliverableItemView> items(long stepId) {
        return jdbc.query(
                "SELECT di.id, di.name, di.type_code, t.name AS type_name, t.frontend_component, " +
                        "di.required, di.constraint_json, di.sort_order " +
                        "FROM step_deliverable_item di " +
                        "JOIN dict_deliverable_type t ON t.code = di.type_code " +
                        "WHERE di.step_id = ? AND di.is_deleted = 0 ORDER BY di.sort_order, di.id",
                itemMapper, stepId);
    }

    @Transactional
    public long create(UserPrincipal user, long courseId, StepDtos.StepSaveRequest request) {
        authz.requireCourseManager(user, courseId);
        validate(request);
        String status = normalizeStatus(request.status());
        String publishAt = "PUBLISHED".equals(status)
                ? LocalDateTime.now().format(FORMATTER) : Dates.normalize(request.publishAt());

        long stepId = JdbcIds.insert(jdbc,
                "INSERT INTO step (course_id, title, description, requirement, weight, publish_at, " +
                        "due_at, status, allow_resubmit, sort_order) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                courseId, request.title(), request.description(), request.requirement(),
                weight(request), publishAt, Dates.normalize(request.dueAt()), status,
                Boolean.TRUE.equals(request.allowResubmit()) ? 1 : 0,
                request.sortOrder() == null ? 0 : request.sortOrder());
        insertItems(stepId, request.items());
        return stepId;
    }

    @Transactional
    public void update(UserPrincipal user, long stepId, StepDtos.StepSaveRequest request) {
        authz.requireStepManager(user, stepId);
        validate(request);
        Integer submitted = jdbc.queryForObject(
                "SELECT COUNT(*) FROM submission WHERE step_id = ? AND is_deleted = 0",
                Integer.class, stepId);
        boolean allowResubmit = Boolean.TRUE.equals(request.allowResubmit());
        if (submitted != null && submitted > 0 && !allowResubmit) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "该步骤已有学生提交，请先开启「允许补交」再修改交付项");
        }

        String status = normalizeStatus(request.status());
        jdbc.update(
                "UPDATE step SET title = ?, description = ?, requirement = ?, weight = ?, " +
                        "publish_at = ?, due_at = ?, status = ?, allow_resubmit = ?, sort_order = ? WHERE id = ?",
                request.title(), request.description(), request.requirement(), weight(request),
                "PUBLISHED".equals(status) ? LocalDateTime.now().format(FORMATTER)
                        : Dates.normalize(request.publishAt()),
                Dates.normalize(request.dueAt()), status, allowResubmit ? 1 : 0,
                request.sortOrder() == null ? 0 : request.sortOrder(), stepId);
        jdbc.update("UPDATE step_deliverable_item SET is_deleted = 1 WHERE step_id = ? AND is_deleted = 0",
                stepId);
        insertItems(stepId, request.items());
    }

    public void updateStatus(UserPrincipal user, long stepId, String status) {
        authz.requireStepManager(user, stepId);
        String normalized = normalizeStatus(status);
        if ("PUBLISHED".equals(normalized)) {
            jdbc.update("UPDATE step SET status = 'PUBLISHED', publish_at = COALESCE(publish_at, NOW()) " +
                    "WHERE id = ?", stepId);
        } else {
            jdbc.update("UPDATE step SET status = ? WHERE id = ?", normalized, stepId);
        }
    }

    private void validate(StepDtos.StepSaveRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "至少需要配置一个交付项");
        }
        BigDecimal weight = weight(request);
        if (weight.compareTo(BigDecimal.ZERO) < 0 || weight.compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "权重需介于 0~100 之间");
        }
        for (StepDtos.DeliverableItemInput item : request.items()) {
            if (item.name() == null || item.name().isBlank()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "交付项名称不能为空");
            }
            Integer available = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM dict_deliverable_type WHERE code = ? AND enabled = 1",
                    Integer.class, item.typeCode());
            if (available == null || available == 0) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        "交付物类型不存在或未启用：" + item.typeCode());
            }
        }
    }

    private BigDecimal weight(StepDtos.StepSaveRequest request) {
        return request.weight() == null ? BigDecimal.ZERO : request.weight();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "DRAFT";
        }
        String value = status.toUpperCase();
        if (!"DRAFT".equals(value) && !"PUBLISHED".equals(value) && !"WITHDRAWN".equals(value)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "步骤状态不合法");
        }
        return value;
    }

    private void insertItems(long stepId, List<StepDtos.DeliverableItemInput> items) {
        int order = 0;
        for (StepDtos.DeliverableItemInput item : items) {
            String constraint = item.constraintJson() == null || item.constraintJson().isBlank()
                    ? null : item.constraintJson();
            jdbc.update(
                    "INSERT INTO step_deliverable_item " +
                            "(step_id, name, type_code, required, constraint_json, sort_order) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                    stepId, item.name(), item.typeCode(),
                    Boolean.FALSE.equals(item.required()) ? 0 : 1, constraint, order++);
        }
    }
}
