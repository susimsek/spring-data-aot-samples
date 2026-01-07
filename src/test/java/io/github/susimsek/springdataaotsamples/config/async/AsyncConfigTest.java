package io.github.susimsek.springdataaotsamples.config.async;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;

class AsyncConfigTest {

    private final AsyncConfig config = new AsyncConfig();

    @Test
    void shouldReturnSimpleAsyncExceptionHandler() {
        AsyncUncaughtExceptionHandler handler = config.getAsyncUncaughtExceptionHandler();
        assertThat(handler).isInstanceOf(SimpleAsyncUncaughtExceptionHandler.class);
    }
}
