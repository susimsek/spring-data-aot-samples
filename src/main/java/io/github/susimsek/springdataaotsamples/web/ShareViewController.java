package io.github.susimsek.springdataaotsamples.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ShareViewController {

    @GetMapping("/share/{token}")
    public String shareView(@PathVariable String token) {
        return "forward:/share.html";
    }
}
