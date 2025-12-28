package io.github.susimsek.springdataaotsamples.config.aot;

import jakarta.persistence.metamodel.StaticMetamodel;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

public final class StaticMetamodelScanner {

    private final String basePackage;
    private final ClassLoader classLoader;

    public StaticMetamodelScanner(String basePackage, ClassLoader classLoader) {
        Assert.hasText(basePackage, "'basePackage' must not be null or empty");
        Assert.notNull(classLoader, "'classLoader' must not be null");
        this.basePackage = basePackage;
        this.classLoader = classLoader;
    }

    public Set<Class<?>> scan() {
        Set<Class<?>> result = new HashSet<>();

        if (!StringUtils.hasText(this.basePackage)) {
            return result;
        }

        PathMatchingResourcePatternResolver resolver =
                new PathMatchingResourcePatternResolver(this.classLoader);
        CachingMetadataReaderFactory metadataReaderFactory =
                new CachingMetadataReaderFactory(resolver);

        String packagePath = ClassUtils.convertClassNameToResourcePath(this.basePackage);
        String pattern = "classpath*:" + packagePath + "/**/*.class";

        try {
            Resource[] resources = resolver.getResources(pattern);

            for (Resource resource : resources) {
                var metadataReader = metadataReaderFactory.getMetadataReader(resource);
                var annotationMetadata = metadataReader.getAnnotationMetadata();

                if (!annotationMetadata.hasAnnotation(StaticMetamodel.class.getName())) {
                    continue;
                }

                String className = metadataReader.getClassMetadata().getClassName();
                result.add(ClassUtils.forName(className, this.classLoader));
            }
        } catch (IOException | ClassNotFoundException ex) {
            throw new IllegalStateException(
                    "Failed to scan @StaticMetamodel classes under " + this.basePackage, ex);
        }

        return result;
    }
}
