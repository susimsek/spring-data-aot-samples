package io.github.susimsek.springdataaotsamples;

import io.github.susimsek.springdataaotsamples.config.JacksonTestConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest(classes = {
    SpringDataAotSamplesApplication.class,
    JacksonTestConfig.class
})
@Import(JacksonTestConfig.class)
public @interface IntegrationTest {}
