package io.github.susimsek.springdataaotsamples.config.aot;

import io.github.susimsek.springdataaotsamples.config.audit.RevisionInfoListener;
import io.github.susimsek.springdataaotsamples.web.error.Violation;
import java.time.Instant;
import liquibase.change.core.AddDefaultValueChange;
import liquibase.change.core.LoadDataChange;
import liquibase.change.core.LoadDataColumnConfig;
import lombok.RequiredArgsConstructor;
import org.hibernate.cache.jcache.internal.JCacheRegionFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryImpl;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(NativeConfig.class)
@RequiredArgsConstructor
public class NativeConfig implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("i18n/**");
        registerLiquibaseHints(hints);
        registerHibernateHints(hints, classLoader);
        registerEnversHints(hints);
        registerConstraintValidatorHints(hints, classLoader);
        registerSpelHints(hints);
        registerWebErrorHints(hints);
    }

    private void registerLiquibaseHints(RuntimeHints hints) {
        hints.resources().registerPattern("db/**");

        hints.reflection()
                .registerType(
                        LoadDataChange.class,
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                        MemberCategory.INVOKE_PUBLIC_METHODS);

        hints.reflection()
                .registerType(
                        AddDefaultValueChange.class,
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                        MemberCategory.INVOKE_PUBLIC_METHODS);

        hints.reflection()
                .registerType(
                        LoadDataColumnConfig.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
    }

    private void registerHibernateHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection()
                .registerType(JCacheRegionFactory.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
        registerStaticMetamodelHints(hints, classLoader);
    }

    private void registerEnversHints(RuntimeHints hints) {
        hints.reflection()
                .registerType(
                        EnversRevisionRepositoryFactoryBean.class,
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
        hints.reflection()
                .registerType(
                        EnversRevisionRepositoryImpl.class,
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                        MemberCategory.INVOKE_PUBLIC_METHODS);
        hints.reflection()
                .registerType(
                        RevisionInfoListener.class,
                        MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                        MemberCategory.INVOKE_PUBLIC_METHODS);
    }

    private static void registerSpelHints(RuntimeHints hints) {
        hints.reflection()
                .registerType(JwtAuthenticationToken.class, MemberCategory.INVOKE_PUBLIC_METHODS);
        hints.reflection().registerType(Instant.class, MemberCategory.INVOKE_PUBLIC_METHODS);
    }

    private static void registerStaticMetamodelHints(RuntimeHints hints, ClassLoader classLoader) {
        String basePackage = "io.github.susimsek.springdataaotsamples.domain";

        StaticMetamodelScanner scanner = new StaticMetamodelScanner(basePackage, classLoader);

        for (Class<?> metamodelClass : scanner.scan()) {
            hints.reflection().registerType(metamodelClass, MemberCategory.ACCESS_PUBLIC_FIELDS);
        }
    }

    private static void registerConstraintValidatorHints(
            RuntimeHints hints, ClassLoader classLoader) {
        String basePackage = "io.github.susimsek.springdataaotsamples.service.validation";

        ConstraintValidatorScanner scanner =
                new ConstraintValidatorScanner(basePackage, classLoader);

        for (Class<?> validatorClass : scanner.scan()) {
            hints.reflection()
                    .registerType(validatorClass, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
        }
    }

    private static void registerWebErrorHints(RuntimeHints hints) {
        hints.reflection().registerType(Violation.class, MemberCategory.INVOKE_PUBLIC_METHODS);
    }
}
