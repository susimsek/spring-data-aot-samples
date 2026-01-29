package io.github.susimsek.springdataaotsamples;

import io.github.susimsek.springdataaotsamples.config.JacksonTestConfig;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Execution(ExecutionMode.CONCURRENT)
@Transactional
@SpringBootTest(classes = {SpringDataAotSamplesApplication.class, JacksonTestConfig.class})
@Import(JacksonTestConfig.class)
public @interface IntegrationTest {}
