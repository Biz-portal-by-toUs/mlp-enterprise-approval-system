package com.multi.mlpenterpriseapprovalsystem;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;


@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class MlpEnterpriseApprovalSystemApplication {

    @PostConstruct
    public void started() {
        // 애플리케이션의 기본 타임존을 서울로 설정
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }
      // test
    public static void main(String[] args) {
        SpringApplication.run(MlpEnterpriseApprovalSystemApplication.class, args);
    }
}
