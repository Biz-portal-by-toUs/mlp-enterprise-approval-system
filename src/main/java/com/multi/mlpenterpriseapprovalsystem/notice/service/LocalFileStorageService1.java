package com.multi.mlpenterpriseapprovalsystem.notice.service;

import com.multi.mlpenterpriseapprovalsystem.notice.dto.FileInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : LocalFileStorageService
 * @since : 2025-12-21 일요일
 */

public class LocalFileStorageService1 {
    private final Path rootDir;

    // 간단한 예시로 "메모리"에 메타데이터 저장 (DB 없이 구현)
    // 서버 재시작하면 목록이 초기화됩니다.
    private final Map<String, StoredFile> store = new ConcurrentHashMap<>();

    public LocalFileStorageService1(@Value("${file.upload-dir}") String uploadDir) throws IOException {
        this.rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.rootDir);
    }

    public FileInfoResponse save(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드 파일이 비어있습니다.");
        }

        String original = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "file"));
        // 경로 조작 방지
        original = original.replace("\\", "/");
        if (original.contains("..")) {
            throw new IllegalArgumentException("허용되지 않는 파일명입니다.");
        }

        String id = UUID.randomUUID().toString();
        String safeOriginal = original;
        String storedFilename = id + "_" + safeOriginal; // 서버 저장 파일명(충돌 방지)

        Path target = rootDir.resolve(storedFilename).normalize();
        if (!target.startsWith(rootDir)) {
            throw new IllegalArgumentException("저장 경로가 올바르지 않습니다.");
        }

        // 저장
        try (var in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        StoredFile meta = new StoredFile(
                id,
                safeOriginal,
                storedFilename,
                file.getSize(),
                LocalDateTime.now()
        );
        store.put(id, meta);

        return new FileInfoResponse(meta.id, meta.originalFilename, meta.size, meta.createdAt);
    }

    public List<FileInfoResponse> list() {
        return store.values().stream()
                .sorted(Comparator.comparing((StoredFile s) -> s.createdAt).reversed())
                .map(s -> new FileInfoResponse(s.id, s.originalFilename, s.size, s.createdAt))
                .toList();
    }

    public DownloadTarget loadForDownload(String id) throws IOException {
        StoredFile meta = store.get(id);
        if (meta == null) {
            throw new NoSuchElementException("파일이 존재하지 않습니다. id=" + id);
        }

        Path filePath = rootDir.resolve(meta.storedFilename).normalize();
        if (!Files.exists(filePath)) {
            throw new NoSuchElementException("디스크에 파일이 존재하지 않습니다.");
        }

        Resource resource = toResource(filePath);
        return new DownloadTarget(meta.originalFilename, resource);
    }

    private Resource toResource(Path path) throws MalformedURLException {
        return new UrlResource(path.toUri());
    }

    public record DownloadTarget(String originalFilename, Resource resource) {}

    private record StoredFile(
            String id,
            String originalFilename,
            String storedFilename,
            long size,
            LocalDateTime createdAt
    ) {}
}
