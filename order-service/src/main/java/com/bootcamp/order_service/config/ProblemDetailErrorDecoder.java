package com.bootcamp.order_service.config;

import com.bootcamp.order_service.exception.CustomerClientNotAvailable;
import com.bootcamp.order_service.exception.RemoteResourceNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ProblemDetailErrorDecoder implements ErrorDecoder {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final ObjectMapper objectMapper;
    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        // Body tek seferlik: bir kez oku, sonra byte[] ile response'u yeniden kur
        byte[] body = readBody(response);
        Response buffered = response.toBuilder().body(body).build();

        ProblemDetail problem = parseProblem(body, response);
        String detail = (problem != null && problem.getDetail() != null)
                ? problem.getDetail()
                : "%s failed with status %d".formatted(methodKey, response.status());

        log.warn("Remote call failed: method={}, url={}, status={}, detail={}",
                methodKey, response.request().url(), response.status(), detail);

        return switch (response.status()) {
            case 404 -> new RemoteResourceNotFoundException(detail);
            case 500 -> new CustomerClientNotAvailable("Client is not available.");
            default -> defaultDecoder.decode(methodKey, buffered); // 503 + Retry-After → RetryableException
        };
    }

    private byte[] readBody(Response response) {
        if (response.body() == null) return new byte[0];
        try (InputStream is = response.body().asInputStream()) {
            return is.readAllBytes();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private ProblemDetail parseProblem(byte[] body, Response response) {
        if (body.length == 0 || !isProblemJson(response)) return null;
        try {
            return objectMapper.readValue(body, ProblemDetail.class);
        } catch (Exception e) { // bozuk JSON, beklenmeyen format: asıl hatayı kaybetme
            log.debug("Could not parse ProblemDetail body", e);
            return null;
        }
    }

    private boolean isProblemJson(Response response) {
        return response.headers().getOrDefault("Content-Type", List.of()).stream()
                .anyMatch(v -> v.startsWith(PROBLEM_JSON)); // "; charset=UTF-8" gelebilir
    }
}
