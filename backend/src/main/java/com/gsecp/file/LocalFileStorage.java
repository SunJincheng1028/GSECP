package com.gsecp.file;

import com.gsecp.common.BusinessException;
import com.gsecp.common.ResultCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

@Component
public class LocalFileStorage implements FileStorage {

    private static final DateTimeFormatter MONTH_DIR = DateTimeFormatter.ofPattern("yyyy/MM");

    private final JdbcTemplate jdbcTemplate;
    private final Path baseDir;

    public LocalFileStorage(JdbcTemplate jdbcTemplate,
                            @Value("${gsecp.storage.local-dir:./data/uploads}") String localDir) {
        this.jdbcTemplate = jdbcTemplate;
        this.baseDir = Path.of(localDir).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(MultipartFile file, Long uploaderId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "上传文件不能为空");
        }

        String originalName = StringUtils.cleanPath(
                Objects.requireNonNullElse(file.getOriginalFilename(), "unnamed"));
        String extension = "";
        int dot = originalName.lastIndexOf('.');
        if (dot >= 0 && dot < originalName.length() - 1) {
            extension = originalName.substring(dot);
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + extension;

        Path dir = baseDir.resolve(LocalDate.now().format(MONTH_DIR)).normalize();
        Path target = dir.resolve(storedName).normalize();
        try {
            Files.createDirectories(dir);
            String md5 = DigestUtils.md5DigestAsHex(file.getInputStream());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            long size = Files.size(target);
            String relativePath = baseDir.relativize(target).toString().replace('\\', '/');

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO sys_file (original_name, storage_type, storage_path, content_type, " +
                                "size_bytes, md5, uploader_id) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, originalName);
                ps.setString(2, "LOCAL");
                ps.setString(3, relativePath);
                ps.setString(4, file.getContentType());
                ps.setLong(5, size);
                ps.setString(6, md5);
                if (uploaderId == null) {
                    ps.setNull(7, java.sql.Types.BIGINT);
                } else {
                    ps.setLong(7, uploaderId);
                }
                return ps;
            }, keyHolder);

            Long fileId = Objects.requireNonNull(keyHolder.getKey()).longValue();
            return new StoredFile(fileId, originalName, relativePath, size, md5);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "文件保存失败：" + e.getMessage());
        }
    }

    @Override
    public StoredResource load(Long fileId) {
        try {
            StoredResource resource = jdbcTemplate.queryForObject(
                    "SELECT original_name, storage_path FROM sys_file WHERE id = ? AND is_deleted = 0",
                    (rs, rowNum) -> new StoredResource(
                            rs.getString("original_name"),
                            baseDir.resolve(rs.getString("storage_path")).normalize()),
                    fileId);
            Objects.requireNonNull(resource);
            if (!resource.path().startsWith(baseDir)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "非法的文件路径");
            }
            return resource;
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件不存在");
        }
    }
}
