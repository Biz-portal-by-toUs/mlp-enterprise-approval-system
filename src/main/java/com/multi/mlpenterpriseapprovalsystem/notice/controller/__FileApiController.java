package com.multi.mlpenterpriseapprovalsystem.notice.controller;

/**
 * Please explain the class!!!
 *
 * @author : kim youngkwan
 * @filename : FileApiController
 * @since : 2025-12-21 일요일
 */

import com.multi.mlpenterpriseapprovalsystem.notice.dto.FileInfoResponse;
import com.multi.mlpenterpriseapprovalsystem.notice.service.LocalFileStorageService1;
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

public class __FileApiController {

    private final LocalFileStorageService1 storageService;

    public __FileApiController(LocalFileStorageService1 storageService) {
        this.storageService = storageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileInfoResponse upload(@RequestPart("file") MultipartFile file) throws IOException {
        return storageService.save(file);
    }

    @GetMapping
    public List<FileInfoResponse> list() {
        return storageService.list();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable("id") String id,
                                             HttpServletRequest request) throws IOException {
        var target = storageService.loadForDownload(id);

        ContentDisposition cd = ContentDisposition.attachment()
                .filename(target.originalFilename(), StandardCharsets.UTF_8)
                .build();

        // MIME 추측 (예: xlsx, png 등)
        String contentType = request.getServletContext()
                .getMimeType(target.originalFilename());
        MediaType mediaType = (contentType != null)
                ? MediaType.parseMediaType(contentType)
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .contentType(mediaType)
                .body(target.resource());
    }
}
