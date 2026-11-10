package com.gsecp.submission;

import com.gsecp.common.Authz;
import com.gsecp.common.BusinessException;
import com.gsecp.common.JdbcIds;
import com.gsecp.common.ResultCode;
import com.gsecp.security.UserPrincipal;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SubmissionService {

    private final JdbcTemplate jdbc;
    private final Authz authz;
    private final DeliverableValidation validation;

    private record StepInfo(long id, long courseId, String title, String status,
                            String dueAt, boolean allowResubmit) {
    }

    private record SubmissionRow(
            Long id, Long stepId, Long studentId, String studentName, Integer version,
            String status, Boolean latest, String submitAt,
            java.math.BigDecimal score, String comment
    ) {
    }

    private final RowMapper<SubmissionRow> submissionMapper = (rs, rowNum) -> new SubmissionRow(
            rs.getLong("id"),
            rs.getLong("step_id"),
            rs.getLong("student_id"),
            rs.getString("student_name"),
            rs.getInt("version"),
            rs.getString("status"),
            rs.getBoolean("is_latest"),
            rs.getString("submit_at"),
            rs.getBigDecimal("total_score"),
            rs.getString("comment"));

    private final RowMapper<SubmissionDtos.SubmissionItemView> itemMapper = (rs, rowNum) ->
            new SubmissionDtos.SubmissionItemView(
                    rs.getLong("id"),
                    rs.getLong("deliverable_item_id"),
                    rs.getString("name"),
                    rs.getString("type_code"),
                    rs.getString("type_name"),
                    rs.getString("frontend_component"),
                    rs.getBoolean("required"),
                    rs.getString("text_value"),
                    rs.getObject("file_id") == null ? null : rs.getLong("file_id"),
                    rs.getString("original_name"),
                    rs.getString("url_value"));

    private static final String SUBMISSION_SELECT =
            "SELECT sub.id, sub.step_id, sub.student_id, u.real_name AS student_name, sub.version, " +
                    "sub.status, sub.is_latest, sub.submit_at, sc.total_score, sc.comment " +
                    "FROM submission sub JOIN sys_user u ON u.id = sub.student_id " +
                    "LEFT JOIN score sc ON sc.submission_id = sub.id AND sc.is_deleted = 0 " +
                    "WHERE sub.is_deleted = 0";

    public SubmissionService(JdbcTemplate jdbc, Authz authz, DeliverableValidation validation) {
        this.jdbc = jdbc;
        this.authz = authz;
        this.validation = validation;
    }

    @Transactional
    public long submit(UserPrincipal user, long stepId, SubmissionDtos.SubmitRequest request) {
        StepInfo step = loadStep(stepId);
        if (!"PUBLISHED".equals(step.status())) {
            throw bad("步骤尚未发布，暂时无法提交");
        }
        authz.requireStepMember(user, stepId);

        List<DeliverableValidation.StepItem> items = loadItems(stepId);
        Map<Long, SubmissionDtos.SubmissionItemInput> provided = new HashMap<>();
        for (SubmissionDtos.SubmissionItemInput input : request.items()) {
            if (input.deliverableItemId() == null) {
                throw bad("交付项 ID 不能为空");
            }
            if (provided.put(input.deliverableItemId(), input) != null) {
                throw bad("同一交付项不能重复提交");
            }
        }
        Set<Long> validIds = new HashSet<>();
        for (DeliverableValidation.StepItem item : items) {
            validIds.add(item.id());
        }
        for (Long id : provided.keySet()) {
            if (!validIds.contains(id)) {
                throw bad("存在不属于该步骤的交付项");
            }
        }

        for (DeliverableValidation.StepItem item : items) {
            SubmissionDtos.SubmissionItemInput input = provided.get(item.id());
            if (Boolean.TRUE.equals(item.required()) && input == null) {
                throw bad("缺少必填交付项：" + item.name());
            }
            if (input != null) {
                validation.validate(item, input);
            }
        }

        Integer graded = jdbc.queryForObject(
                "SELECT COUNT(*) FROM submission sub " +
                        "JOIN score sc ON sc.submission_id = sub.id AND sc.is_deleted = 0 " +
                        "WHERE sub.step_id = ? AND sub.student_id = ? AND sub.is_latest = 1 " +
                        "AND sub.is_deleted = 0",
                Integer.class, stepId, user.getUserId());
        if (graded != null && graded > 0 && !step.allowResubmit()) {
            throw new BusinessException(ResultCode.CONFLICT, "该步骤已批改且未开启「允许补交」");
        }

        Integer maxVersion = jdbc.queryForObject(
                "SELECT COALESCE(MAX(version), 0) FROM submission WHERE step_id = ? AND student_id = ?",
                Integer.class, stepId, user.getUserId());
        Integer late = jdbc.queryForObject(
                "SELECT COUNT(*) FROM step WHERE id = ? AND due_at IS NOT NULL AND due_at < NOW()",
                Integer.class, stepId);
        String status = late != null && late > 0 ? "LATE" : "SUBMITTED";

        jdbc.update("UPDATE submission SET is_latest = 0 WHERE step_id = ? AND student_id = ?",
                stepId, user.getUserId());
        long submissionId = JdbcIds.insert(jdbc,
                "INSERT INTO submission (step_id, student_id, version, status, is_latest, submit_at) " +
                        "VALUES (?, ?, ?, ?, 1, NOW())",
                stepId, user.getUserId(), (maxVersion == null ? 0 : maxVersion) + 1, status);

        for (DeliverableValidation.StepItem item : items) {
            SubmissionDtos.SubmissionItemInput input = provided.get(item.id());
            if (input == null) {
                continue;
            }
            jdbc.update(
                    "INSERT INTO submission_item (submission_id, deliverable_item_id, type_code, " +
                            "text_value, file_id, url_value) VALUES (?, ?, ?, ?, ?, ?)",
                    submissionId, item.id(), item.typeCode(),
                    input.textValue(), input.fileId(), input.urlValue());
        }
        return submissionId;
    }

    public SubmissionDtos.SubmissionView mySubmission(UserPrincipal user, long stepId) {
        authz.requireStepMember(user, stepId);
        try {
            Long id = jdbc.queryForObject(
                    "SELECT id FROM submission WHERE step_id = ? AND student_id = ? " +
                            "AND is_latest = 1 AND is_deleted = 0",
                    Long.class, stepId, user.getUserId());
            return detailInternal(id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<SubmissionDtos.SubmissionView> myVersions(UserPrincipal user, long stepId) {
        authz.requireStepMember(user, stepId);
        List<Long> ids = jdbc.queryForList(
                "SELECT id FROM submission WHERE step_id = ? AND student_id = ? AND is_deleted = 0 " +
                        "ORDER BY version DESC",
                Long.class, stepId, user.getUserId());
        List<SubmissionDtos.SubmissionView> result = new ArrayList<>();
        for (Long id : ids) {
            result.add(detailInternal(id));
        }
        return result;
    }

    public SubmissionDtos.SubmissionView detail(UserPrincipal user, long submissionId) {
        checkReadable(user, submissionId);
        return detailInternal(submissionId);
    }

    private SubmissionDtos.SubmissionView detailInternal(long submissionId) {
        SubmissionRow row;
        try {
            row = jdbc.queryForObject(SUBMISSION_SELECT + " AND sub.id = ?",
                    submissionMapper, submissionId);
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException(ResultCode.NOT_FOUND, "提交记录不存在");
        }
        return new SubmissionDtos.SubmissionView(
                row.id(), row.stepId(), row.studentId(), row.studentName(), row.version(),
                row.status(), row.latest(), row.submitAt(), row.score(), row.comment(),
                items(submissionId));
    }

    private List<SubmissionDtos.SubmissionItemView> items(long submissionId) {
        return jdbc.query(
                "SELECT si.id, si.deliverable_item_id, COALESCE(di.name, '历史交付项') AS name, " +
                        "si.type_code, t.name AS type_name, t.frontend_component, di.required, " +
                        "si.text_value, si.file_id, f.original_name, si.url_value " +
                        "FROM submission_item si " +
                        "LEFT JOIN step_deliverable_item di ON di.id = si.deliverable_item_id " +
                        "LEFT JOIN dict_deliverable_type t ON t.code = si.type_code " +
                        "LEFT JOIN sys_file f ON f.id = si.file_id " +
                        "WHERE si.submission_id = ? AND si.is_deleted = 0 ORDER BY si.id",
                itemMapper, submissionId);
    }

    public List<SubmissionDtos.ProgressView> progress(UserPrincipal user, long stepId) {
        authz.requireStepManager(user, stepId);
        long classId = authz.classIdOfStep(stepId);
        return jdbc.query(
                "SELECT u.id AS student_id, u.username, u.real_name, u.user_no, " +
                        "sub.id AS submission_id, sub.status, sub.version, sub.submit_at, " +
                        "sc.total_score, sc.comment " +
                        "FROM class_member m JOIN sys_user u ON u.id = m.student_id " +
                        "LEFT JOIN submission sub ON sub.step_id = ? AND sub.student_id = u.id " +
                        "  AND sub.is_latest = 1 AND sub.is_deleted = 0 " +
                        "LEFT JOIN score sc ON sc.submission_id = sub.id AND sc.is_deleted = 0 " +
                        "WHERE m.class_id = ? AND m.status = 'ACTIVE' AND m.is_deleted = 0 " +
                        "ORDER BY u.id",
                (rs, rowNum) -> new SubmissionDtos.ProgressView(
                        rs.getLong("student_id"),
                        rs.getString("username"),
                        rs.getString("real_name"),
                        rs.getString("user_no"),
                        rs.getObject("submission_id") == null ? null : rs.getLong("submission_id"),
                        rs.getString("status"),
                        rs.getObject("version") == null ? null : rs.getInt("version"),
                        rs.getString("submit_at"),
                        rs.getBigDecimal("total_score"),
                        rs.getString("comment")),
                stepId, classId);
    }

    private void checkReadable(UserPrincipal user, long submissionId) {
        Long studentId;
        try {
            studentId = jdbc.queryForObject(
                    "SELECT student_id FROM submission WHERE id = ? AND is_deleted = 0",
                    Long.class, submissionId);
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException(ResultCode.NOT_FOUND, "提交记录不存在");
        }
        if (authz.isStudent(user) && !user.getUserId().equals(studentId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能查看自己的提交");
        }
        if (!authz.isStudent(user)) {
            authz.requireClassManager(user, authz.classIdOfSubmission(submissionId));
        }
    }

    private StepInfo loadStep(long stepId) {
        try {
            return jdbc.queryForObject(
                    "SELECT id, course_id, title, status, due_at, allow_resubmit FROM step " +
                            "WHERE id = ? AND is_deleted = 0",
                    (rs, rowNum) -> new StepInfo(
                            rs.getLong("id"), rs.getLong("course_id"), rs.getString("title"),
                            rs.getString("status"), rs.getString("due_at"), rs.getBoolean("allow_resubmit")),
                    stepId);
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException(ResultCode.NOT_FOUND, "步骤不存在");
        }
    }

    private List<DeliverableValidation.StepItem> loadItems(long stepId) {
        return jdbc.query(
                "SELECT id, name, type_code, required, constraint_json FROM step_deliverable_item " +
                        "WHERE step_id = ? AND is_deleted = 0 ORDER BY sort_order, id",
                (rs, rowNum) -> new DeliverableValidation.StepItem(
                        rs.getLong("id"), rs.getString("name"), rs.getString("type_code"),
                        rs.getBoolean("required"), rs.getString("constraint_json")),
                stepId);
    }

    private BusinessException bad(String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, message);
    }
}
