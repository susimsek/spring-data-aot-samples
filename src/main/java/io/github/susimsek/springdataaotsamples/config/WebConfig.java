package io.github.susimsek.springdataaotsamples.config;

import io.github.susimsek.springdataaotsamples.web.filter.SpaWebFilter;
import jakarta.servlet.DispatcherType;
import java.util.EnumSet;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration(proxyBeanMethods = false)
public class WebConfig {

    @Bean
    public FilterRegistrationBean<SpaWebFilter> spaWebFilter() {
        FilterRegistrationBean<SpaWebFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SpaWebFilter());
        registration.setDispatcherTypes(
                EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
