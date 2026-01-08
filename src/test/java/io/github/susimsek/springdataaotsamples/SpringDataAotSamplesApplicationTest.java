package io.github.susimsek.springdataaotsamples;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.SpringApplication;

class SpringDataAotSamplesApplicationTest {

    @Test
    void mainShouldDelegateToSpringApplicationRun() {
        try (var mocked = mockStatic(SpringApplication.class)) {
            ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);

            SpringDataAotSamplesApplication.main(new String[] {"--test"});

            mocked.verify(
                    () ->
                            SpringApplication.run(
                                    eq(SpringDataAotSamplesApplication.class), captor.capture()));
            assertThat(captor.getValue()).containsExactly("--test");
        }
    }
}
