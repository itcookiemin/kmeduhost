package io.itcookies.edu.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
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
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "message 필드가 필요합니다."));
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String systemPrompt = "당신은 베이킹 레시피 전문가입니다. 사용자가 요청한 베이킹 레시피에 대해 반드시 아래 JSON 형식으로만 응답하세요. "
                + "다른 텍스트, 마크다운 코드블록, 설명을 절대 포함하지 마세요. 순수 JSON만 반환하세요.\n"
                + "{\"status\": \"ok\", \"ingredients\": [\"재료1\", \"재료2\"], "
                + "\"steps\": [{\"step\": \"단계 설명\", \"duration\": \"소요시간\"}, ...]}";

        String fullMessage = systemPrompt + "\n\n사용자 요청: " + userMessage;

        // responseMimeType으로 JSON 응답 강제
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("responseMimeType", "application/json");

        Map<String, Object> body = new HashMap<>();
        body.put("contents", List.of(
                Map.of("parts", List.of(
                        Map.of("text", fullMessage)
                ))
        ));
        body.put("generationConfig", generationConfig);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<?, ?> geminiBody = response.getBody();

            if (geminiBody == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("status", "error", "message", "Gemini API 응답이 비어있습니다."));
            }

            // Gemini 응답에서 텍스트 추출: candidates[0].content.parts[0].text
            List<?> candidates = (List<?>) geminiBody.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("status", "error", "message", "Gemini 응답에 candidates가 없습니다."));
            }

            Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
            Map<?, ?> content = (Map<?, ?>) candidate.get("content");
            List<?> parts = (List<?>) content.get("parts");
            Map<?, ?> part = (Map<?, ?>) parts.get(0);
            String text = (String) part.get("text");

            if (text == null || text.isBlank()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("status", "error", "message", "Gemini 응답 텍스트가 비어있습니다."));
            }

            // 마크다운 코드블록 제거 후 JSON 파싱
            text = text.trim();
            if (text.startsWith("```")) {
                text = text.replaceAll("(?s)^```[a-zA-Z]*\\n?", "").replaceAll("```\\s*$", "").trim();
            }

            // { } 사이 JSON 추출 (앞뒤 불필요한 텍스트 방어 처리)
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start != -1 && end != -1 && end > start) {
                text = text.substring(start, end + 1);
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> result = mapper.readValue(text, Map.class);
            return ResponseEntity.ok(result);

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            // Gemini API 자체가 4xx/5xx 반환한 경우 → 실제 오류 내용 포함
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message",
                            "Gemini API 오류 [" + e.getStatusCode() + "]: " + e.getResponseBodyAsString()));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "JSON 파싱 실패: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "서버 오류: " + e.getMessage()));
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