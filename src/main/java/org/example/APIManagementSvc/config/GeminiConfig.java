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
            throw new IllegalStateException("Gemini API key is not configured. Please set GEMINI_API_KEY environment variable or gemini.api.key property.");
        }
        return new Client(apiKey);
    }
}