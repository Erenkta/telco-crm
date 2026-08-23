package com.bootcamp.hello_service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RefreshScope
public class HelloController {

    @Value("${hello.message:Default Hello}")
    private String message;

    @GetMapping("/hello")
    public String sayHello() {
        return message;
    }
}