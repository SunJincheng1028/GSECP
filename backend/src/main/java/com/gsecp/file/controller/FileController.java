package com.gsecp.file.controller;

import com.gsecp.common.BusinessException;
import com.gsecp.common.R;
import com.gsecp.common.ResultCode;
import com.gsecp.file.FileStorage;
import com.gsecp.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Tag(name = "文件")
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorage fileStorage;

    public FileController(FileStorage fileStorage) {
        this.fileStorage = fileStorage;
    }

    @Operation(summary = "上传文件（需登录）")
    @PostMapping("/upload")
    public R<FileStorage.StoredFile> upload(@RequestParam("file") MultipartFile file,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        Long uploaderId = principal == null ? null : principal.getUserId();
        return R.ok(fileStorage.store(file, uploaderId));
    }

    @Operation(summary = "下载文件（需登录）")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        FileStorage.StoredResource stored = fileStorage.load(id);
        if (!Files.exists(stored.path())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文件已丢失");
        }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(stored.originalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(stored.path()));
    }
}
