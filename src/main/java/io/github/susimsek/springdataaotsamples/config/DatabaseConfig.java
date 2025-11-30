package io.github.susimsek.springdataaotsamples.config;

import io.github.susimsek.springdataaotsamples.config.audit.HttpHeaderAuditorAware;
import org.h2.tools.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.sql.SQLException;

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

    @Bean(initMethod = "start", destroyMethod = "stop")
    @Profile("!prod")
    public Server h2TcpServer() throws SQLException {
        return Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092");
    }
}
