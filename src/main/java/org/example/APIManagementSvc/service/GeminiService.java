package org.example.APIManagementSvc.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {
    
    private final Client geminiClient;
    
    public String generateText(String prompt) {
        try {
            log.debug("Generating text with prompt: {}", prompt);
            
            GenerateContentResponse response = geminiClient.models.generateContent(
                    "gemini-2.5-flash",
                prompt, 
                null
            );
            
            String generatedText = response.text();
            log.debug("Generated text: {}", generatedText);
            
            return generatedText;
        } catch (Exception e) {
            log.error("Error generating text with Gemini API", e);
            throw new RuntimeException("Failed to generate text using Gemini API", e);
        }
    }
    
    public String classifyApiContent(String apiName, String apiDescription, String apiUrl) {
        String prompt = String.format(
            "Classify the following API and provide a category classification:\n\n" +
            "API Name: %s\n" +
            "API Description: %s\n" +
            "API URL: %s\n\n" +
            "Please classify this API into one of the following categories: " +
            "Data, Payment, Social, Communication, Analytics, AI/ML, E-commerce, Government, Weather, Maps, Finance, News, Other. " +
            "Provide only the category name as the response.",
            apiName, apiDescription, apiUrl
        );
        
        return generateText(prompt);
    }
    
    public String classifyApiWithContext(String apiName, String apiDescription, String apiUrl, 
                                       String existingDomains, String existingKeywords) {
        String prompt = String.format(
            "기존 도메인 목록: [%s]\n" +
            "기존 키워드 목록: [%s]\n\n" +
            "분류할 API 정보:\n" +
            "- API 이름: %s\n" +
            "- API 설명: %s\n" +
            "- API URL: %s\n\n" +
            "위 API를 분석하여 다음 형식으로 응답해주세요:\n" +
            "DOMAIN: [기존 도메인 중 가장 적합한 것 또는 새로운 도메인명]\n" +
            "KEYWORD: [기존 키워드 중 가장 적합한 것 또는 새로운 키워드명]\n\n" +
            "규칙:\n" +
            "1. 기존 도메인/키워드가 적합하면 정확히 같은 이름으로 응답\n" +
            "2. 적합한 것이 없다면 새로운 이름을 제안\n" +
            "3. 도메인과 키워드 이름만 응답 (설명 불필요)",
            existingDomains, existingKeywords, apiName, apiDescription, apiUrl
        );
        
        return generateText(prompt);
    }
    
    public String analyzeApiSecurity(String apiUrl, String apiDescription) {
        String prompt = String.format(
            "Analyze the security aspects of the following API:\n\n" +
            "API URL: %s\n" +
            "API Description: %s\n\n" +
            "Please provide a security assessment including potential risks and recommendations. " +
            "Keep the response concise and professional.",
            apiUrl, apiDescription
        );
        
        return generateText(prompt);
    }
}