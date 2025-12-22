package com.multi.mlpenterpriseapprovalsystem.notice.controller;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : FileApiController
 * @since : 2025-12-21 일요일
 */

import com.multi.mlpenterpriseapprovalsystem.notice.dto.FileInfoResponse;
import com.multi.mlpenterpriseapprovalsystem.notice.service.LocalFileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
public class FileApiController {

    private final LocalFileStorageService storageService;

    public FileApiController(LocalFileStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileInfoResponse upload(@RequestPart("file") MultipartFile file) throws IOException {
        return storageService.save(file);
    }

    // ✅ 업로드 전이라도 디스크에 있는 파일 목록 반환
    @GetMapping
    public List<FileInfoResponse> list() throws IOException {
        return storageService.listFromDisk();
    }


    @GetMapping("/{storedName}/download")
    public ResponseEntity<Resource> download(@PathVariable("storedName") String storedName,
                                             HttpServletRequest request) throws IOException {
        var target = storageService.loadForDownloadByStoredName(storedName);

        ContentDisposition cd = ContentDisposition.attachment()
                .filename(target.originalFilename(), StandardCharsets.UTF_8)
                .build();

        String mime = request.getServletContext().getMimeType(target.originalFilename());
        MediaType mediaType = (mime != null) ? MediaType.parseMediaType(mime) : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .contentType(mediaType)
                .body(target.resource());
    }

    @DeleteMapping("/{storedName}")
    public ResponseEntity<Void> delete(@PathVariable("storedName") String storedName) throws IOException {
        storageService.deleteByStoredName(storedName);
        return ResponseEntity.noContent().build(); // 204
    }
}
