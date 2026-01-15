package com.multi.mlpenterpriseapprovalsystem.notice.service;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : LocalFileStorageService
 * @since : 2025-12-21 일요일
 */

import com.multi.mlpenterpriseapprovalsystem.notice.dto.FileInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

//@Service
public class LocalFileStorageService {

    private final Path rootDir;

    public LocalFileStorageService(@Value("${file.upload-dir}") String uploadDir) throws IOException {
        this.rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.rootDir);
    }

    // 업로드: 기존처럼 저장 (UUID_원본명)
    public FileInfoResponse save(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드 파일이 비어있습니다.");
        }

        String original = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "file"));
        original = original.replace("\\", "/");
        if (original.contains("..")) throw new IllegalArgumentException("허용되지 않는 파일명입니다.");

        String uuid = UUID.randomUUID().toString();
        String storedName = uuid + "_" + original;

        Path target = rootDir.resolve(storedName).normalize();
        if (!target.startsWith(rootDir)) throw new IllegalArgumentException("저장 경로가 올바르지 않습니다.");

        try (var in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        LocalDateTime createdAt = LocalDateTime.ofInstant(
                Files.getLastModifiedTime(target).toInstant(),
                ZoneId.systemDefault()
        );

        // id에는 storedName을 그대로 사용 (다운로드 식별자)
        return new FileInfoResponse(storedName, original, file.getSize(), createdAt);
    }

    //  목록: c디렉토리 upload 폴더를 스캔해서 보여줌 (업로드 전에도 보임)
    public List<FileInfoResponse> listFromDisk() throws IOException {
        if (!Files.exists(rootDir)) return List.of();

        try (var stream = Files.list(rootDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> {
                        try {
                            String storedName = path.getFileName().toString();
                            String originalName = extractOriginalName(storedName);
                            long size = Files.size(path);
                            LocalDateTime createdAt = LocalDateTime.ofInstant(
                                    Files.getLastModifiedTime(path).toInstant(),
                                    ZoneId.systemDefault()
                            );
                            return new FileInfoResponse(storedName, originalName, size, createdAt);
                        } catch (IOException e) {
                            // 실패한 파일은 목록에서 제외
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(FileInfoResponse::createdAt).reversed())
                    .collect(Collectors.toList());
        }
    }

    //  다운로드: storedName으로 파일 로드
    public DownloadTarget loadForDownloadByStoredName(String storedName) throws IOException {
        String safe = sanitizeStoredName(storedName);

        Path filePath = rootDir.resolve(safe).normalize();
        if (!filePath.startsWith(rootDir)) throw new IllegalArgumentException("허용되지 않은 경로입니다.");
        if (!Files.exists(filePath)) throw new NoSuchElementException("파일이 존재하지 않습니다.");

        Resource resource = toResource(filePath);
        String originalName = extractOriginalName(safe);
        return new DownloadTarget(originalName, resource);
    }

    private String sanitizeStoredName(String storedName) {
        // 경로조작 방지
        String s = storedName.replace("\\", "/");
        if (s.contains("..") || s.contains("/")) {
            throw new IllegalArgumentException("허용되지 않는 파일명입니다.");
        }
        return s;
    }

    private String extractOriginalName(String storedName) {
        // UUID_원본명 형식이면 "_" 뒤를 원본명으로 간주
        int idx = storedName.indexOf("_");
        if (idx > 0 && idx < storedName.length() - 1) {
            return storedName.substring(idx + 1);
        }
        // 그냥 파일이면 그대로 보여줌
        return storedName;
    }

    private Resource toResource(Path path) throws MalformedURLException {
        return new UrlResource(path.toUri());
    }

    public record DownloadTarget(String originalFilename, Resource resource) {}

    public void deleteByStoredName(String storedName) throws IOException {
        String safe = sanitizeStoredName(storedName);

        Path filePath = rootDir.resolve(safe).normalize();
        if (!filePath.startsWith(rootDir)) {
            throw new IllegalArgumentException("허용되지 않은 경로입니다.");
        }

        try {
            Files.delete(filePath); // 실제 파일 삭제
        } catch (NoSuchFileException e) {
            throw new NoSuchElementException("파일이 존재하지 않습니다.");
        }
    }
}

