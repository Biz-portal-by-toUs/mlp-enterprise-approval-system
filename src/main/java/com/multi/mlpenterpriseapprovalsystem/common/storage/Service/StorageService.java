package com.multi.mlpenterpriseapprovalsystem.common.storage.Service;

import com.multi.mlpenterpriseapprovalsystem.common.storage.dto.StoredFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 저장 서비스 인터페이스
 * 인터페이스로 만드는 이유?
 * 1) 저장 방식 교체 쉬움 ( 나중에 s3로 바꾸고 싶으면 S3StorageService implements StorageService만 만들면 끝)
 * 2) 테스트도 쉽다 (FakeStorageService/InMemoryStorageService 같은 걸 만들어서 저장한 척만 하고 StoredFile만 리턴하게 할 수 있다.)
 *
 * @author : 권지영
 * @filename : StorageService
 * @since : 2025. 12. 19. 금요일
 */
public interface StorageService {
    StoredFile store(MultipartFile file, String dirName);
}
