package io.github.susimsek.springdataaotsamples.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ShareViewControllerTest {

    private final ShareViewController controller = new ShareViewController();

    @Test
    void shareViewShouldForwardToHtml() {
        String view = controller.shareView("token123");
        assertThat(view).isEqualTo("forward:/share.html");
    }
}
