package com.multi.mlpenterpriseapprovalsystem.search.infra;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : ElasticsearchProps
 * @since : 2026. 1. 9. 금요일
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.elasticsearch")
public class ElasticsearchProps {
    private String host = "http://localhost:9200";
    private String indexSearchAlias = "bizportal_search";
    private String indexMenuAlias = "bizportal_search_menu";

}