package io.itcookies.edu.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gemini")
public class GeminiApiController {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.0-flash}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Gemini API에 메시지를 전송하고 응답을 반환합니다.
     * POST /api/gemini/chat
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");
        if (userMessage == null || userMessage.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "message 필드가 필요합니다."));
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(
                Map.of("parts", List.of(
                        Map.of("text", userMessage)
                ))
        ));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Gemini API 호출 실패: " + e.getMessage()));
        }
    }

    /**
     * Gemini API 키 유효성 확인 (모델 목록 조회)
     * GET /api/gemini/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey;

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "message", "Gemini API 키가 유효합니다.",
                    "models", response.getBody()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "API 키 확인 실패: " + e.getMessage()));
        }
    }

    @RequestMapping("/hello")
    public String hello() {
        return "{message: 'hello'}";
    }
}
