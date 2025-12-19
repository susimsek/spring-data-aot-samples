package io.github.susimsek.springdataaotsamples.config;

import io.github.susimsek.springdataaotsamples.config.audit.SecurityAuditorAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableJpaRepositories(
    basePackages = "io.github.susimsek.springdataaotsamples.repository",
    repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
@EnableTransactionManagement
public class DatabaseConfig {

  @Bean
  public AuditorAware<String> auditorAware() {
    return new SecurityAuditorAware();
  }
}
