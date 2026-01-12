package io.github.susimsek.springdataaotsamples.config.aot;

import jakarta.validation.ConstraintValidator;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

public final class ConstraintValidatorScanner {

    private final String basePackage;
    private final @Nullable ClassLoader classLoader;

    public ConstraintValidatorScanner(String basePackage, @Nullable ClassLoader classLoader) {
        this.basePackage = basePackage;
        this.classLoader = classLoader;
    }

    public Set<Class<?>> scan() {
        Set<Class<?>> result = new HashSet<>();

        if (!StringUtils.hasText(basePackage)) {
            return result;
        }

        PathMatchingResourcePatternResolver resolver =
                new PathMatchingResourcePatternResolver(classLoader);
        CachingMetadataReaderFactory metadataReaderFactory =
                new CachingMetadataReaderFactory(resolver);

        String packagePath = ClassUtils.convertClassNameToResourcePath(basePackage);
        String pattern = "classpath*:" + packagePath + "/**/*.class";

        try {
            Resource[] resources = resolver.getResources(pattern);

            for (Resource resource : resources) {
                var metadataReader = metadataReaderFactory.getMetadataReader(resource);
                String className = metadataReader.getClassMetadata().getClassName();

                Class<?> clazz = ClassUtils.forName(className, classLoader);

                if (ConstraintValidator.class.isAssignableFrom(clazz)) {
                    result.add(clazz);
                }
            }
        } catch (IOException | ClassNotFoundException ex) {
            throw new IllegalStateException(
                    "Failed to scan ConstraintValidator classes under " + basePackage, ex);
        }

        return result;
    }
}
