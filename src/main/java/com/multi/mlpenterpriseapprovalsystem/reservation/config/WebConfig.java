package com.multi.mlpenterpriseapprovalsystem.reservation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Please explain the class!!!
 *
 * @author : 송현님
 * @filename : Webconfig
 * @since : 2025-12-17 오후 4:12 수요일
 */

//@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${image.image-dir}")
    private String IMAGE_DIR;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = IMAGE_DIR.replace("\\", "/");
        if (!dir.endsWith("/")) dir += "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:///" + dir);
    }




}
