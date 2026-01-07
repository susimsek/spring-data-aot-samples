package io.github.susimsek.springdataaotsamples.config;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class JacksonTestConfig {

    @Bean
    JsonMapper jsonMapper() {
        return JsonMapper.builder().findAndAddModules().build();
    }
}
