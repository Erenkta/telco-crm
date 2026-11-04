package com.bootcamp.common.config;

import com.bootcamp.common.exception.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

// Services scan only their own base package (e.g. com.bootcamp.identity_service), so beans under
// com.bootcamp.common are never found by component scan. Spring Boot instead loads the classes listed in
// META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports from every jar on the classpath.
// Shared beans (exception handler, pageable resolver) are registered here with @Import.
@AutoConfiguration
@Import({GlobalExceptionHandler.class,PageableConfig.class})
@EnableConfigurationProperties(PageableProperties.class)
public class CommonWebAutoConfiguration {
}
