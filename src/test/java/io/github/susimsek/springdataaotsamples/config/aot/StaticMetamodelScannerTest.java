package io.github.susimsek.springdataaotsamples.config.aot;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.metamodel.StaticMetamodel;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StaticMetamodelScannerTest {

    @Test
    void scanShouldFindMetamodelClasses() {
        StaticMetamodelScanner scanner =
                new StaticMetamodelScanner(
                        "io.github.susimsek.springdataaotsamples.domain",
                        getClass().getClassLoader());

        Set<Class<?>> metas = scanner.scan();

        assertThat(metas).anyMatch(c -> c.isAnnotationPresent(StaticMetamodel.class));
    }

    @Test
    void scanShouldReturnEmptyForMissingPackage() {
        StaticMetamodelScanner scanner =
                new StaticMetamodelScanner("invalid.package", getClass().getClassLoader());

        Set<Class<?>> result = scanner.scan();

        assertThat(result).isEmpty();
    }
}
