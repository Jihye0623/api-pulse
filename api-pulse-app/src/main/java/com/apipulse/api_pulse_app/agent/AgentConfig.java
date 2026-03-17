package com.apipulse.api_pulse_app.agent;

import com.apipulse.api_pulse_app.agent.tools.*;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AgentConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String openAiApiKey;

    private final SearchLogsTool searchLogsTool;
    private final AnalyzePatternTool analyzePatternTool;
    private final RestartServiceTool restartServiceTool;
    private final SendAlertTool sendAlertTool;
    private final GetMetricsTool getMetricsTool;
    private final GenerateReportTool generateReportTool;

    @Bean
    public FinGuardAgent finGuardAgent() {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName("gpt-4o-mini")
                .temperature(0.3)
                .build();

        return AiServices.builder(FinGuardAgent.class)
                .chatLanguageModel(model)
                .tools(
                        searchLogsTool,
                        analyzePatternTool,
                        restartServiceTool,
                        sendAlertTool,
                        getMetricsTool,
                        generateReportTool
                )
                .build();
    }
}