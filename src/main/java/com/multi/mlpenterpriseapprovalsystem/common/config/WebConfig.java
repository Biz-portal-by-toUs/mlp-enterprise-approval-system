package com.multi.mlpenterpriseapprovalsystem.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * WebConfig
 * 이거 없으면 저장은 되는데 /uploads/**로 접근이 안됨
 *
 * @author : 권지영
 * @filename : WebConfig
 * @since : 2025. 12. 19. 금요일
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.storage.local.root-dir}") // 이게 뭔지 알아야돼. env 에 저장해야되는지도
    private String rootDir;

    @Value("${app.storage.public-url-prefix}")
    private String publicUrlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(rootDir).toAbsolutePath().normalize().toUri().toString();

        registry.addResourceHandler(publicUrlPrefix + "/**")
                .addResourceLocations(location);
    }
}
