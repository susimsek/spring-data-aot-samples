package io.github.susimsek.springdataaotsamples.config;

import io.github.susimsek.springdataaotsamples.config.audit.AuditorEvaluationContextExtension;
import io.github.susimsek.springdataaotsamples.config.audit.HttpHeaderAuditorAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.spel.spi.EvaluationContextExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableJpaRepositories(
        basePackages = "io.github.susimsek.springdataaotsamples.repository",
        repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class
)
@EnableTransactionManagement
public class DatabaseConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return new HttpHeaderAuditorAware();
    }

    @Bean
    public EvaluationContextExtension auditorExtension(AuditorAware<String> auditorAware) {
        return new AuditorEvaluationContextExtension(auditorAware);
    }
}
