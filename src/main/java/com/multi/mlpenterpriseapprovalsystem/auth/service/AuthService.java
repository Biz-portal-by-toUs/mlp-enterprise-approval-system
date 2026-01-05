package com.multi.mlpenterpriseapprovalsystem.auth.service;

import com.multi.mlpenterpriseapprovalsystem.auth.dto.CustomUser;
import com.multi.mlpenterpriseapprovalsystem.common.ResponseDto;
import com.multi.mlpenterpriseapprovalsystem.common.api.nts.service.BusinessVerificationService;
import com.multi.mlpenterpriseapprovalsystem.common.enums.RoleType;
import com.multi.mlpenterpriseapprovalsystem.common.exception.CustomException;
import com.multi.mlpenterpriseapprovalsystem.common.exception.ErrorCode;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.dto.ResTokenDto;
import com.multi.mlpenterpriseapprovalsystem.common.jwt.service.TokenService;
import com.multi.mlpenterpriseapprovalsystem.common.storage.dto.StoredFile;
import com.multi.mlpenterpriseapprovalsystem.common.storage.service.StorageService;
import com.multi.mlpenterpriseapprovalsystem.company.domain.Company;
import com.multi.mlpenterpriseapprovalsystem.company.dto.ReqCompanyLoginDto;
import com.multi.mlpenterpriseapprovalsystem.company.dto.ReqCompanySignupDto;
import com.multi.mlpenterpriseapprovalsystem.company.repository.CompanyRepository;
import com.multi.mlpenterpriseapprovalsystem.document.repository.DocumentRepository;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.repository.DocumentFormCategoryRepository;
import com.multi.mlpenterpriseapprovalsystem.document_form.form.repository.DocumentFormRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.dto.ReqAdminEmployeeCreateDto;
import com.multi.mlpenterpriseapprovalsystem.employee.dto.ReqEmployeeLoginDto;
import com.multi.mlpenterpriseapprovalsystem.employee.dto.ResAdminEmployeeCreateDto;
import com.multi.mlpenterpriseapprovalsystem.employee.enums.MsgStat;
import com.multi.mlpenterpriseapprovalsystem.employee.repository.EmployeeRepository;
import com.multi.mlpenterpriseapprovalsystem.employee.service.EmployeeService;
import com.multi.mlpenterpriseapprovalsystem.organization.department.domain.Department;
import com.multi.mlpenterpriseapprovalsystem.organization.department.repository.DepartmentRepository;
import com.multi.mlpenterpriseapprovalsystem.organization.positions.domain.Positions;
import com.multi.mlpenterpriseapprovalsystem.organization.positions.repository.PositionsRepository;
import com.multi.mlpenterpriseapprovalsystem.subscription.domain.Subscription;
import com.multi.mlpenterpriseapprovalsystem.subscription.repository.CompanySubscriptionRepository;
import com.multi.mlpenterpriseapprovalsystem.subscription.repository.SubscriptionRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/**
 * 회원가입, 로그인, 로그아웃 부분 서비스
 *
 * @author : 권지영
 * @filename : AuthService
 * @since : 2025. 12. 17. 수요일
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final CompanyUserDetailService companyUserDetailService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final EmployeeUserDetailService employeeUserDetailService;
    private final CompanyRepository companyRepository;
    private final StorageService storageService;
    private final SubscriptionRepository subscriptionRepository;
    private final BusinessVerificationService businessVerificationService;
    private final EmployeeRepository employeeRepository;
    private final CompanySubscriptionRepository companySubscriptionRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionsRepository positionsRepository;
    private final EmployeeService employeeService;

    private static final String DEFAULT_PASSWORD = "1234";
    private final DocumentRepository documentRepository;
    private final DocumentFormRepository documentFormRepository;
    private final DocumentFormCategoryRepository documentFormCategoryRepository;

    @Transactional(readOnly = true)
    public boolean isRegisteredBusiness(String brn) {
        if (!businessVerificationService.isRegisteredBusiness(brn)) {
            throw new CustomException(ErrorCode.INVALID_BRN);
        } else if (companyRepository.existsByBrn(brn)) {
            throw new CustomException(ErrorCode.BRN_DUPLICATE);
        }
        return true;
    }

    public ResponseDto<ResAdminEmployeeCreateDto> signUpCompany(ReqCompanySignupDto reqCompanySignupDto, MultipartFile logo) {


        // 1) 이메일 중복 체크
        if (companyRepository.existsByEmail(reqCompanySignupDto.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        // 2) 회사 코드 중복 체크
        if (companyRepository.existsByComId(reqCompanySignupDto.getComId())) {
            throw new CustomException(ErrorCode.DUPLICATE_COMID);
        }
        // 회사 코드 ToUpperCase
        String comId = reqCompanySignupDto.getComId().trim().toUpperCase();

        // 3) 비밀번호 암호화
        String encodedPwd = passwordEncoder.encode(reqCompanySignupDto.getPwd());

        // 4) 로고 저장(있으면 저장하고 imgUrl/path 세팅)
        String imgUrl = null;
        String path = null;
        if (logo != null && !logo.isEmpty()) {
            StoredFile stored = storageService.store(logo, "company-logo");
            imgUrl = stored.getUrl();  // /uploads/company-logo/xxx.png
            path = stored.getPath();   // /Users/.../bizportal/uploads/company-logo/xxx.png
        }

        // 5) sub_no=1 연결 (회원가입 시 기본 요금제)
        Subscription basic = subscriptionRepository.findById((long) 1)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

        // 6) Company 생성 + 저장
        Company company = Company.createForSignup(
                comId,
                reqCompanySignupDto.getComName(),
                reqCompanySignupDto.getEmail(),
                encodedPwd,
                reqCompanySignupDto.getBrn(),
                reqCompanySignupDto.getAddr(),
                imgUrl,
                path,
                basic,
                RoleType.COM_ADMIN
        );

        companyRepository.save(company);

        // 사원 등록
        Department defaultDept = departmentRepository.save(
                Department.of(company, "ADM", "관리부")
        );
        Positions defaultPos = positionsRepository.save(
                Positions.of(company, "회사 관리자", 1)
        );

        ReqAdminEmployeeCreateDto adminReq = new ReqAdminEmployeeCreateDto();
        adminReq.setDepNo(defaultDept.getDepNo());
        adminReq.setPosNo(defaultPos.getPosNo());

        adminReq.setEmpName(company.getComName()); // ✅ 자동
        adminReq.setEmail(company.getEmail());               // ✅ DTO에서 가져옴
        adminReq.setPhone("01000000000");                    // ✅ 기본값(임시)
        adminReq.setWorkPhone("0000");                         // ✅ 없으면 null
        adminReq.setGen(null);                               // ✅ 없으면 null (컬럼 nullable이어야)
        adminReq.setHireDate(LocalDate.now());               // ✅ 오늘 날짜로 자동
        adminReq.setRole(RoleType.COM_ADMIN);
        adminReq.setAddr(company.getAddr());                 // ✅ DTO에서 가져옴
        adminReq.setObjectKey(null);
        adminReq.setBirth(null);

        ResAdminEmployeeCreateDto createEmployee = employeeService.createEmployee(company.getComId(), adminReq);



// todo: COM_ADMIN사원 만들면 문서양식 작성자에 넣고 주석풀기
//        // 회사 요금제 정보 등록
//        CompanySubscription newCompanySubscription = CompanySubscription.builder()
//                .company(company)
//                .subscription(basic)
//                .paymentMethod(null)
//                .nextBillingDate(null)
//                .autoRenewal(false)
//                .status(SubStatus.FREE)
//                .build();
//
//        companySubscriptionRepository.save(newCompanySubscription);
//
//        // 기본 양식 및 카테고리 등록
//        DocumentForm newDocForm1 = DocumentForm.builder()
//                .company(company)
//                .docfoName("기본 양식")
//                .writer(null)
//                .cnttJson("")
//                .cnttHtml("")
//                .docfoStat(DocumentFormStats.A)
//                .build();
//        DocumentFormCategory newDocFormCat1 = DocumentFormCategory.builder()
//                .company(company)
//                .name("기본상신")
//                .documentForm(newDocForm1)
//                .build();
//        DocumentFormCategory newDocFormCat2 = DocumentFormCategory.builder()
//                .company(company)
//                .name("인사요청")
//                .documentForm(newDocForm1)
//                .build();
//        DocumentFormCategory newDocFormCat3 = DocumentFormCategory.builder()
//                .company(company)
//                .name("구매요청")
//                .documentForm(newDocForm1)
//                .build();
//        DocumentFormCategory newDocFormCat4 = DocumentFormCategory.builder()
//                .company(company)
//                .name("면담요청")
//                .documentForm(newDocForm1)
//                .build();
//        DocumentFormCategory newDocFormCat5 = DocumentFormCategory.builder()
//                .company(company)
//                .name("기타")
//                .documentForm(newDocForm1)
//                .build();
//        documentFormRepository.save(newDocForm1);
//        documentFormCategoryRepository.save(newDocFormCat1);
//        documentFormCategoryRepository.save(newDocFormCat2);
//        documentFormCategoryRepository.save(newDocFormCat3);
//        documentFormCategoryRepository.save(newDocFormCat4);
//        documentFormCategoryRepository.save(newDocFormCat5);
//
//        // 휴가 신청서 및 카테고리 등록
//        DocumentForm newDocForm2 = DocumentForm.builder()
//                .company(company)
//                .docfoName("휴가 신청서")
//                .writer(null)
//                .cnttJson("{\"type\": \"doc\", \"content\": [{\"type\": \"heading\", \"attrs\": {\"level\": 2, \"textAlign\": \"center\"}, \"content\": [{\"text\": \"휴가 신청서\", \"type\": \"text\"}]}, {\"type\": \"table\", \"content\": [{\"type\": \"tableRow\", \"content\": [{\"type\": \"tableCell\", \"attrs\": {\"editable\": false}, \"content\": [{\"type\": \"paragraph\", \"attrs\": {\"textAlign\": \"center\"}, \"content\": [{\"text\": \"수정/취소 대상\", \"type\": \"text\", \"marks\": [{\"type\": \"bold\"}]}]}]}, {\"type\": \"tableCell\", \"attrs\": {\"colspan\": 3, \"editable\": false, \"data-field\": \"targetAttendance\"}, \"content\": [{\"type\": \"paragraph\", \"attrs\": {\"textAlign\": \"center\"}, \"content\": [{\"text\": \"-\", \"type\": \"text\"}]}]}]}, {\"type\": \"tableRow\", \"content\": [{\"type\": \"tableCell\", \"attrs\": {\"editable\": false}, \"content\": [{\"type\": \"paragraph\", \"attrs\": {\"textAlign\": \"center\"}, \"content\": [{\"text\": \"휴가 구분\", \"type\": \"text\", \"marks\": [{\"type\": \"bold\"}]}]}]}, {\"type\": \"tableCell\", \"attrs\": {\"editable\": true}, \"content\": [{\"type\": \"paragraph\"}]}, {\"type\": \"tableCell\", \"attrs\": {\"editable\": false}, \"content\": [{\"type\": \"paragraph\", \"attrs\": {\"textAlign\": \"center\"}, \"content\": [{\"text\": \"비상 연락처\", \"type\": \"text\", \"marks\": [{\"type\": \"bold\"}]}]}]}, {\"type\": \"tableCell\", \"attrs\": {\"editable\": true}, \"content\": [{\"type\": \"paragraph\"}]}]}, {\"type\": \"tableRow\", \"content\": [{\"type\": \"tableCell\", \"attrs\": {\"editable\": false}, \"content\": [{\"type\": \"paragraph\", \"attrs\": {\"textAlign\": \"center\"}, \"content\": [{\"text\": \"시작일\", \"type\": \"text\", \"marks\": [{\"type\": \"bold\"}]}]}]}, {\"type\": \"tableCell\", \"attrs\": {\"editable\": false, \"data-field\": \"startDate\"}, \"content\": [{\"type\": \"paragraph\", \"attrs\": {\"textAlign\": \"center\"}, \"content\": [{\"text\": \"-\", \"type\": \"text\"}]}]}, {\"type\": \"tableCell\", \"attrs\": {\"editable\": false}, \"content\": [{\"type\": \"paragraph\", \"attrs\": {\"textAlign\": \"center\"}, \"content\": [{\"text\": \"종료일\", \"type\": \"text\", \"marks\": [{\"type\": \"bold\"}]}]}]}, {\"type\": \"tableCell\", \"attrs\": {\"editable\": false, \"data-field\": \"endDate\"}, \"content\": [{\"type\": \"paragraph\", \"attrs\": {\"textAlign\": \"center\"}, \"content\": [{\"text\": \"-\", \"type\": \"text\"}]}]}]}, {\"type\": \"tableRow\", \"content\": [{\"type\": \"tableCell\", \"attrs\": {\"editable\": false}, \"content\": [{\"type\": \"paragraph\", \"attrs\": {\"textAlign\": \"center\"}, \"content\": [{\"text\": \"대직자\", \"type\": \"text\", \"marks\": [{\"type\": \"bold\"}]}]}]}, {\"type\": \"tableCell\", \"attrs\": {\"colspan\": 3, \"editable\": false, \"data-field\": \"delegate\"}, \"content\": [{\"type\": \"paragraph\", \"attrs\": {\"textAlign\": \"center\"}, \"content\": [{\"text\": \"-\", \"type\": \"text\"}]}]}]}, {\"type\": \"tableRow\", \"content\": [{\"type\": \"tableCell\", \"attrs\": {\"editable\": false}, \"content\": [{\"type\": \"paragraph\", \"attrs\": {\"textAlign\": \"center\"}, \"content\": [{\"text\": \"사유\", \"type\": \"text\", \"marks\": [{\"type\": \"bold\"}]}]}]}, {\"type\": \"tableCell\", \"attrs\": {\"colspan\": 3, \"editable\": true}, \"content\": [{\"type\": \"paragraph\"}]}]}]}, {\"type\": \"paragraph\", \"attrs\": {\"textAlign\": \"center\"}, \"content\": [{\"text\": \"위와 같이 휴가를 신청합니다.\", \"type\": \"text\"}]}]}")
//                .cnttHtml("")
//                .docfoStat(DocumentFormStats.A)
//                .build();
//        DocumentFormCategory newDocFormCat6 = DocumentFormCategory.builder()
//                .company(company)
//                .name("휴가신청")
//                .documentForm(newDocForm2)
//                .build();
//        DocumentFormCategory newDocFormCat7 = DocumentFormCategory.builder()
//                .company(company)
//                .name("휴가취소신청")
//                .documentForm(newDocForm2)
//                .build();
//        DocumentFormCategory newDocFormCat8 = DocumentFormCategory.builder()
//                .company(company)
//                .name("휴가수정신청")
//                .documentForm(newDocForm2)
//                .build();
//        documentFormRepository.save(newDocForm2);
//        documentFormCategoryRepository.save(newDocFormCat6);
//        documentFormCategoryRepository.save(newDocFormCat7);
//        documentFormCategoryRepository.save(newDocFormCat8);
//
//        // 출장 신청서 및 카테고리 등록
//        DocumentForm newDocForm3 = DocumentForm.builder()
//                .company(company)
//                .docfoName("출장 신청서")
//                .writer(null)
//                .cnttJson("{\"type\": \"doc\", \"content\": [{\"type\": \"heading\", \"attrs\": {\"level\": 2, \"textAlign\": \"center\"}, \"content\": [{\"text\": \"출장 신청서\", \"type\": \"text\"}]}, {\"type\": \"table\", \"content\": [{\"type\": \"tableRow\", \"content\": [{\"type\": \"tableCell\", \"attrs\": {\"editable\": false}, \"content\": [{\"type\": \"paragraph\", \"attrs\": {\"textAlign\": \"center\"}, \"content\": [{\"text\": \"수정/취소 대상\", \"type\": \"text\", \"marks\": [{\"type\": \"bold\"}]}]}]}, {\"type\": \"tableCell\", \"attrs\": {\"colspan\": 3, \"editable\": false, \"data-field\": \"targetAttendance\"}, \"content\": [{\"type\": \"paragraph\", \"attrs\": {\"textAlign\": \"center\"}, \"content\": [{\"text\": \"-\", \"type\": \"text\"}]}]}]}, {\"type\": \"tableRow\", \"content\": [{\"type\": \"tableCell\", \"attrs\": {\"editable\": false}, \"content\": [{\"type\": \"paragraph\", \"attrs\": {\"textAlign\": \"center\"}, \"content\": [{\"text\": \"출장지\", \"type\": \"text\", \"marks\": [{\"type\": \"bold\"}]}]}]}, {\"type\": \"tableCell\", \"attrs\": {\"editable\": true}, \"content\": [{\"type\": \"paragraph\"}]}, {\"type\": \"tableCell\", \"attrs\": {\"editable\": false}, \"content\": [{\"type\": \"paragraph\", \"attrs\": {\"textAlign\": \"center\"}, \"content\": [{\"text\": \"비상 연락처\", \"type\": \"text\", \"marks\": [{\"type\": \"bold\"}]}]}]}, {\"type\": \"tableCell\", \"attrs\": {\"editable\": true}, \"content\": [{\"type\": \"paragraph\"}]}]}, {\"type\": \"tableRow\", \"content\": [{\"type\": \"tableCell\", \"attrs\": {\"editable\": false}, \"content\": [{\"type\": \"paragraph\", \"attrs\": {\"textAlign\": \"center\"}, \"content\": [{\"text\": \"시작일\", \"type\": \"text\", \"marks\": [{\"type\": \"bold\"}]}]}]}, {\"type\": \"tableCell\", \"attrs\": {\"editable\": false, \"data-field\": \"startDate\"}, \"content\": [{\"type\": \"paragraph\", \"attrs\": {\"textAlign\": \"center\"}, \"content\": [{\"text\": \"-\", \"type\": \"text\"}]}]}, {\"type\": \"tableCell\", \"attrs\": {\"editable\": false}, \"content\": [{\"type\": \"paragraph\", \"attrs\": {\"textAlign\": \"center\"}, \"content\": [{\"text\": \"종료일\", \"type\": \"text\", \"marks\": [{\"type\": \"bold\"}]}]}]}, {\"type\": \"tableCell\", \"attrs\": {\"editable\": false, \"data-field\": \"endDate\"}, \"content\": [{\"type\": \"paragraph\", \"attrs\": {\"textAlign\": \"center\"}, \"content\": [{\"text\": \"-\", \"type\": \"text\"}]}]}]}, {\"type\": \"tableRow\", \"content\": [{\"type\": \"tableCell\", \"attrs\": {\"editable\": false}, \"content\": [{\"type\": \"paragraph\", \"attrs\": {\"textAlign\": \"center\"}, \"content\": [{\"text\": \"목적\", \"type\": \"text\", \"marks\": [{\"type\": \"bold\"}]}]}]}, {\"type\": \"tableCell\", \"attrs\": {\"colspan\": 3, \"editable\": true}, \"content\": [{\"type\": \"paragraph\"}]}]}]}, {\"type\": \"paragraph\", \"attrs\": {\"textAlign\": \"center\"}, \"content\": [{\"text\": \"위와 같이 출장을 신청합니다.\", \"type\": \"text\"}]}]}")
//                .cnttJson("")
//                .docfoStat(DocumentFormStats.A)
//                .build();
//        DocumentFormCategory newDocFormCat9 = DocumentFormCategory.builder()
//                .company(company)
//                .name("출장신청")
//                .documentForm(newDocForm3)
//                .build();
//        DocumentFormCategory newDocFormCat10 = DocumentFormCategory.builder()
//                .company(company)
//                .name("출장취소신청")
//                .documentForm(newDocForm3)
//                .build();
//        DocumentFormCategory newDocFormCat11 = DocumentFormCategory.builder()
//                .company(company)
//                .name("출장수정신청")
//                .documentForm(newDocForm3)
//                .build();
//        documentFormRepository.save(newDocForm3);
//        documentFormCategoryRepository.save(newDocFormCat9);
//        documentFormCategoryRepository.save(newDocFormCat10);
//        documentFormCategoryRepository.save(newDocFormCat11);
        
        return new ResponseDto<>(HttpStatus.CREATED, "회사 회원가입 성공", createEmployee);
    }

    public ResTokenDto loginCompany(ReqCompanyLoginDto reqCompanyLoginDto, HttpServletResponse response) {

        // 1) 회사 사용자 조회 (CompanyUserDetailService가 CustomUser를 반환하도록 구현)
        CustomUser user = companyUserDetailService.loadUserByUsername(reqCompanyLoginDto.getEmail());

        // 2) 비밀번호 검증
        if (!passwordEncoder.matches(reqCompanyLoginDto.getPassword(), user.getPassword())) {
            // 너희 ErrorCode 쓰는 방식이면 이걸 추천
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }


        // 3) 토큰 발급 + refresh 쿠키 세팅
        return tokenService.createToken(user, response);
    }


    public ResTokenDto loginEmployee(ReqEmployeeLoginDto reqEmployeeLoginDto, HttpServletResponse response) {

        String empId = normalizeEmpId(reqEmployeeLoginDto.getEmpId());

        // 1) 회사 사용자 조회 (CompanyUserDetailService가 CustomUser를 반환하도록 구현)
        CustomUser user = employeeUserDetailService.loadUserByUsername(empId);

        log.info("username>>>>>>>>>>>>>>>>>>>> " + user.getUsername());

        // 2) 비밀번호 검증
        if (!passwordEncoder.matches(reqEmployeeLoginDto.getPassword(), user.getPassword())) {
            // 너희 ErrorCode 쓰는 방식이면 이걸 추천
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 3) 토큰 발급 + refresh 쿠키 세팅
        ResTokenDto res = tokenService.createToken(user, response);

        // 로그인 성공 후
        String atte = employeeRepository.findAtteByEmpNo(user.getSubjectId()); // 쿼리 하나 추가
        String next = ("V".equals(atte) || "B".equals(atte)) ? "OFF" : "WORKING";
        employeeRepository.updateMsgStatByEmpNo(user.getSubjectId(), MsgStat.valueOf(next));

        boolean mustChange = passwordEncoder.matches(DEFAULT_PASSWORD, user.getPassword());



        return res.toBuilder()
                .mustChangePassword(mustChange)
                .build();
    }

    private String normalizeEmpId(String empId) {
        if (empId == null) return null;
        return empId.trim().toUpperCase();
    }


    @Transactional(readOnly = true)
    public boolean checkComId(String comId) {

        // comId 무조건 대문자 처리
        comId = comId.trim().toUpperCase();

        if (companyRepository.existsByComId(comId)) {
            throw new CustomException(ErrorCode.DUPLICATE_COMID);
        }
        return true;
    }

    @Transactional(readOnly = true)
    public boolean checkEmail(String email) {

        if (companyRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        return true;
    }


}
