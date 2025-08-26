package org.example.APIManagementSvc.config;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {
    
    @Value("${gemini.api.key:#{environment.GEMINI_API_KEY}}")
    private String apiKey;
    
    @Bean
    public Client geminiClient() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Gemini API 키 가 정의되어있지 않습니다. GEMINI_API_KEY 환경변수를 정의해주세요.");
        }
        return new Client();
    }
}