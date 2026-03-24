package com.code.codewarden.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfig {
    @Bean
    public ChatLanguageModel chatLanguageModel(
            @Value("${codewarden.llm.api-key:}") String apiKey,
            @Value("${codewarden.llm.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${codewarden.llm.model:gpt-4o-mini}") String modelName,
            @Value("${codewarden.llm.temperature:0.1}") Double temperature,
            @Value("${codewarden.llm.timeout-seconds:60}") Integer timeoutSeconds
    ) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .build();
    }
}
