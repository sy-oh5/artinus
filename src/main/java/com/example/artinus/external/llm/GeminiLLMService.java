package com.example.artinus.external.llm;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiLLMService implements LLMService {

    private final Client geminiClient;

    @Override
    public String generatePrompt(String prompt) {
        try {
            GenerateContentResponse response = geminiClient.models.generateContent(
                    "gemini-2.5-flash",
                    prompt,
                    null
            );

            return response.text();

        } catch (Exception e) {
            log.error("Gemini API 호출 실패: {}", e.getMessage());
            return "요약을 생성할 수 없습니다.";
        }
    }
}
