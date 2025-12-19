package com.multi.mlpenterpriseapprovalsystem.common.storage.Service;

import com.multi.mlpenterpriseapprovalsystem.common.storage.dto.StoredFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 로컬 스토리지 저장 서비스
 *
 * @author : 권지영
 * @filename : LocalStorageSerivce
 * @since : 2025. 12. 19. 금요일
 */
@Service
public class LocalStorageService implements StorageService {

    private final Path rootDir;
    private final String publicUrlPrefix;

    public LocalStorageService(
            @Value("${app.storage.local.root-dir}") String rootDir,
            @Value("${app.storage.public-url-prefix}") String publicUrlPrefix
    ) {
        this.rootDir = Paths.get(rootDir).toAbsolutePath().normalize();
        this.publicUrlPrefix = publicUrlPrefix;
    }

    @Override
    public StoredFile store(MultipartFile file, String dirName) {
        if (file == null || file.isEmpty()) return null;

        try {
            // 1) 저장 폴더 준비
            Path dirPath = rootDir.resolve(dirName).normalize();
            Files.createDirectories(dirPath);

            // 2) 안전한 파일명 만들기
            String original = file.getOriginalFilename();
            String ext = "";
            if (StringUtils.hasText(original) && original.contains(".")) {
                ext = original.substring(original.lastIndexOf("."));
            }
            String savedName = UUID.randomUUID() + ext;

            // 3) 저장
            Path savedPath = dirPath.resolve(savedName).normalize();
            Files.copy(file.getInputStream(), savedPath, StandardCopyOption.REPLACE_EXISTING);

            // 4) URL, PATH 반환
            String url = publicUrlPrefix + "/" + dirName + "/" + savedName; // /uploads/company-logo/xxx.png
            String path = savedPath.toString(); // /Users/.../bizportal/uploads/company-logo/xxx.png

            return new StoredFile(url, path);

        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패", e);
        }
    }
}
