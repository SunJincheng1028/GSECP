package com.gsecp.submission;

import com.gsecp.common.BusinessException;
import com.gsecp.common.Constraints;
import com.gsecp.common.ResultCode;
import com.gsecp.dict.ConfigService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 按交付物类型分发校验。新增类型只需在此注册一个 handler；未注册类型走默认校验，
 * 因此「字典加一行 + 前端注册组件」即可扩展，无需改动提交主流程。
 */
@Service
public class DeliverableValidation {

    public record StepItem(Long id, String name, String typeCode, Boolean required, String constraintJson) {
    }

    public record FileInfo(Long id, String originalName, long sizeBytes, String contentType) {
    }

    @FunctionalInterface
    private interface Handler {
        void validate(StepItem item, SubmissionDtos.SubmissionItemInput input,
                      Constraints constraints, FileInfo file);
    }

    private final JdbcTemplate jdbc;
    private final ConfigService configService;
    private final Map<String, Handler> handlers = new HashMap<>();

    public DeliverableValidation(JdbcTemplate jdbc, ConfigService configService) {
        this.jdbc = jdbc;
        this.configService = configService;
        handlers.put("DOCUMENT", this::validateDocument);
        handlers.put("CODE_ARCHIVE", this::validateArchive);
        handlers.put("REPO_URL", this::validateRepoUrl);
        handlers.put("TEXT", this::validateText);
    }

    public void validate(StepItem item, SubmissionDtos.SubmissionItemInput input) {
        Constraints constraints = Constraints.from(item.constraintJson());
        FileInfo file = input.fileId() == null ? null : loadFile(input.fileId());
        handlers.getOrDefault(item.typeCode(), this::validateDefault)
                .validate(item, input, constraints, file);
    }

    private FileInfo loadFile(long fileId) {
        try {
            return jdbc.queryForObject(
                    "SELECT id, original_name, size_bytes, content_type FROM sys_file " +
                            "WHERE id = ? AND is_deleted = 0",
                    (rs, rowNum) -> new FileInfo(
                            rs.getLong("id"), rs.getString("original_name"),
                            rs.getLong("size_bytes"), rs.getString("content_type")),
                    fileId);
        } catch (EmptyResultDataAccessException e) {
            throw bad("上传的附件不存在，请重新上传");
        }
    }

    private void validateDocument(StepItem item, SubmissionDtos.SubmissionItemInput input,
                                  Constraints c, FileInfo file) {
        requireFile(item, file);
        List<String> formats = c.formats().isEmpty() ? List.of("pdf", "docx", "md") : c.formats();
        if (!formats.contains(extension(file.originalName()))) {
            throw bad("「" + item.name() + "」仅支持 " + String.join(" / ", formats) + " 格式");
        }
        checkSize(item, file, c);
    }

    private void validateArchive(StepItem item, SubmissionDtos.SubmissionItemInput input,
                                 Constraints c, FileInfo file) {
        requireFile(item, file);
        List<String> formats = c.formats().isEmpty() ? List.of("zip", "rar", "gz") : c.formats();
        if (!formats.contains(extension(file.originalName()))) {
            throw bad("「" + item.name() + "」仅支持 " + String.join(" / ", formats) + " 格式");
        }
        checkSize(item, file, c);
    }

    private void validateRepoUrl(StepItem item, SubmissionDtos.SubmissionItemInput input,
                                 Constraints c, FileInfo file) {
        String value = input.urlValue();
        if (value == null || value.isBlank()) {
            throw bad("请填写「" + item.name() + "」的仓库地址");
        }
        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException e) {
            throw bad("「" + item.name() + "」的 URL 格式不正确");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw bad("「" + item.name() + "」需以 http(s) 开头");
        }
        List<String> hosts = c.urlHosts().isEmpty() ? List.of("github.com", "gitee.com") : c.urlHosts();
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
        boolean allowed = hosts.stream().anyMatch(h -> host.equals(h) || host.endsWith("." + h));
        if (!allowed) {
            throw bad("「" + item.name() + "」需为 " + String.join(" / ", hosts) + " 仓库链接");
        }
    }

    private void validateText(StepItem item, SubmissionDtos.SubmissionItemInput input,
                              Constraints c, FileInfo file) {
        String text = input.textValue();
        if (text == null || text.isBlank()) {
            throw bad("请填写「" + item.name() + "」");
        }
        int length = text.trim().length();
        if (length < c.minWords()) {
            throw bad("「" + item.name() + "」至少需要 " + c.minWords() + " 字");
        }
        if (length > c.maxWords()) {
            throw bad("「" + item.name() + "」不能超过 " + c.maxWords() + " 字");
        }
    }

    private void validateDefault(StepItem item, SubmissionDtos.SubmissionItemInput input,
                                 Constraints c, FileInfo file) {
        boolean hasContent = (input.textValue() != null && !input.textValue().isBlank())
                || input.fileId() != null
                || (input.urlValue() != null && !input.urlValue().isBlank());
        if (!hasContent) {
            throw bad("请填写「" + item.name() + "」");
        }
    }

    private void requireFile(StepItem item, FileInfo file) {
        if (file == null) {
            throw bad("请上传「" + item.name() + "」的文件");
        }
    }

    private void checkSize(StepItem item, FileInfo file, Constraints c) {
        int limitMb = Math.min(c.maxSizeMb(), configService.maxUploadSizeMb());
        long limit = (long) Math.max(1, limitMb) * 1024 * 1024;
        if (file.sizeBytes() > limit) {
            throw bad("「" + item.name() + "」文件超过 " + limitMb + "MB 限制");
        }
    }

    private String extension(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }

    private BusinessException bad(String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, message);
    }
}
