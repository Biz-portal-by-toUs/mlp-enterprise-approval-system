package com.multi.mlpenterpriseapprovalsystem.search.infra;

import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URISyntaxException;

/**
 * Please explain the class!!!
 *
 * @author : 김승기
 * @filename : ElasticsearchRest5Config
 * @since : 2026. 1. 9. 금요일
 */
@Configuration
@EnableConfigurationProperties(ElasticsearchProps.class)
@RequiredArgsConstructor
public class ElasticsearchRest5Config {

    @Bean(destroyMethod = "close")
    public Rest5Client rest5Client(ElasticsearchProps props) throws URISyntaxException {
        return Rest5Client.builder(HttpHost.create(props.getHost())).build();
    }

    @Bean
    public ObjectMapper esObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}