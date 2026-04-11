package io.itcookies.edu.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/claude")
public class ClaudeApiController {

    @Value("${claude.api.key}")
    private String apiKey;

    @Value("${claude.api.url:https://api.anthropic.com/v1/messages}")
    private String apiUrl;

    @Value("${claude.api.model:claude-sonnet-4-6}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Claude API에 메시지를 전송하고 응답을 반환합니다.
     * POST /api/claude/chat
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");
        if (userMessage == null || userMessage.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "message 필드가 필요합니다."));
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("max_tokens", 1024);
        body.put("messages", List.of(
                Map.of("role", "user", "content", userMessage)
        ));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Claude API 호출 실패: " + e.getMessage()));
        }
    }

    /**
     * API 키 유효성 확인 (모델 목록 조회)
     * GET /api/claude/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.anthropic.com/v1/models",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "message", "Claude API 키가 유효합니다.",
                    "models", response.getBody()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "API 키 확인 실패: " + e.getMessage()));
        }
    }
}
