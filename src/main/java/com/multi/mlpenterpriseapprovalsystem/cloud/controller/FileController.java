package com.multi.mlpenterpriseapprovalsystem.cloud.controller;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.cloud.dto.ReqDownloadZipDto;
import com.multi.mlpenterpriseapprovalsystem.cloud.service.FileService;
import com.multi.mlpenterpriseapprovalsystem.common.storage.domain.Attachment;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 파일 관련 유틸/다운로드 기능 API를 제공하는 컨트롤러입니다.
 *
 * 제공 기능:
 * 파일 저장용 이름 생성: 원본 파일명 기반으로 표시명(displayName)을 정리하고,
 * 충돌 방지를 위한 저장명(storedName)을 생성합니다.
 * 선택한 파일 다중 다운로드: 선택한 파일들을 ZIP으로 묶어 스트리밍 다운로드합니다.
 *
 * @author : 송현님
 * @filename : FileController
 * @since : 2026-01-07 오후 5:39 수요일
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    @PostMapping("/download-zip")
    public void downloadZip(
            @RequestBody ReqDownloadZipDto req,
            HttpServletResponse response,
            @AuthenticationPrincipal CustomUser user
    ) throws Exception {

        List<Long> fileIds = Optional.ofNullable(req.getFileIds()).orElse(List.of());
        if (fileIds.isEmpty()) throw new IllegalArgumentException("fileIds is required");

        // ✅ Attachment 조회 (CLOUD + ACTIVE + comId)
        List<Attachment> files = fileService.getAttachmentsForDownload(user, fileIds);

        String zipName = Optional.ofNullable(req.getZipName()).filter(StringUtils::hasText)
                .orElse("files_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".zip");
        if (!zipName.toLowerCase().endsWith(".zip")) zipName += ".zip";

        response.setStatus(200);
        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDispositionAttachment(zipName));

        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(response.getOutputStream()))) {
            Set<String> usedEntryNames = new HashSet<>();

            for (Attachment a : files) {
                String entryName = makeUniqueZipEntryName(
                        sanitizeZipEntryName(a.getOriginalName()),
                        usedEntryNames
                );

                zos.putNextEntry(new ZipEntry(entryName));
                fileService.writeAttachmentToStream(a, zos);
                zos.closeEntry();
                zos.flush();
            }
        }
    }

    // ---------- helpers ----------
    private static String sanitizeZipEntryName(String name) {
        String cleaned = Optional.ofNullable(name).orElse("file");
        cleaned = cleaned.replace("\\", "/");
        cleaned = cleaned.replaceAll("\\.\\./", "");
        cleaned = cleaned.replaceAll("^/+", "");
        cleaned = cleaned.replaceAll("[\\\\:*?\"<>|]", "_");
        if (!StringUtils.hasText(cleaned)) cleaned = "file";
        return cleaned;
    }

    private static String makeUniqueZipEntryName(String name, Set<String> used) {
        if (used.add(name)) return name;

        String base = name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot < name.length() - 1) {
            base = name.substring(0, dot);
            ext = name.substring(dot);
        }

        int i = 1;
        while (true) {
            String candidate = base + " (" + i + ")" + ext;
            if (used.add(candidate)) return candidate;
            i++;
        }
    }

    private static String contentDispositionAttachment(String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        return "attachment; filename*=UTF-8''" + encoded;
    }
}
