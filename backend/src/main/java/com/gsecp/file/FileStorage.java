package com.gsecp.file;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

/**
 * 文件存储抽象：迭代 1 使用本地磁盘实现，后续可替换为 MinIO / OSS 而不影响业务层。
 */
public interface FileStorage {

    StoredFile store(MultipartFile file, Long uploaderId);

    StoredResource load(Long fileId);

    record StoredFile(Long id, String originalName, String storagePath, long sizeBytes, String md5) {
    }

    record StoredResource(String originalName, Path path) {
    }
}
