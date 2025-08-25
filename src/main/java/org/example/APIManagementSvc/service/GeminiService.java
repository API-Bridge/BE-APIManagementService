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