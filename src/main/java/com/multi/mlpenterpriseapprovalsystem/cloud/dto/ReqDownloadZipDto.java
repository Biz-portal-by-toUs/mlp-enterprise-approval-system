package com.multi.mlpenterpriseapprovalsystem.cloud.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 사용자가 선택한 파일들을 ZIP으로 묶어 다운로드하기 위한 요청 DTO입니다.
 *
 * fileIds는 다운로드 대상 파일 ID 목록이며, zipName은 생성될 ZIP 파일명(선택값)입니다.
 * zipName을 지정하지 않으면 서버에서 기본 ZIP 파일명을 생성합니다.
 *
 * @author : 송현님
 * @filename : ReqDownloadZipDto
 * @since : 2026-01-07 오후 7:33 수요일
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReqDownloadZipDto {
    private List<Long> fileIds;  // 선택한 파일들 ID
    private String zipName;      // optional: "files.zip"
}
