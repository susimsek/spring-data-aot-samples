package io.github.susimsek.springdataaotsamples.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration(proxyBeanMethods = false)
@Import(SpringDataWebConfig.class)
public class TestWebMvcConfig {}
