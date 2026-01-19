package io.github.susimsek.springdataaotsamples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.SpringApplication;

class SpringDataAotSamplesApplicationTest {

    @Test
    void mainShouldDelegateToSpringApplicationRun() {
        try (var mocked = mockStatic(SpringApplication.class)) {
            ArgumentCaptor<Class> classCaptor = ArgumentCaptor.forClass(Class.class);
            ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);

            SpringDataAotSamplesApplication.main(new String[] {"--test"});

            mocked.verify(() -> SpringApplication.run(classCaptor.capture(), captor.capture()));
            assertThat(classCaptor.getValue()).isEqualTo(SpringDataAotSamplesApplication.class);
            assertThat(captor.getValue()).containsExactly("--test");
        }
    }
}
