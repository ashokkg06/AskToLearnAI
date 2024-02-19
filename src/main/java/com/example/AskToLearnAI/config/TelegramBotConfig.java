package com.example.AskToLearnAI.config;

import com.example.AskToLearnAI.gemini.GeminiInterface;
import com.example.AskToLearnAI.messagehandler.BotMessageHandler;
import com.pengrad.telegrambot.TelegramBot;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class TelegramBotConfig {

    @Value("${bot.token}")
    private String botToken;
    @Bean
    public TelegramBot telegramBot() {
        return new TelegramBot(botToken);
    }

    @Bean
    public TelegramBot updatesListener(BotMessageHandler botMessageHandler, TelegramBot bot) {
        System.out.println("configuring");
        bot.setUpdatesListener(botMessageHandler::handleUpdates);
        return bot;
    }

    @Bean
    public RestClient geminiRestClient(@Value("${gemini.baseurl}") String baseUrl,
                                       @Value("${googleai.api.key}") String apiKey) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Bean
    public GeminiInterface geminiInterface(@Qualifier("geminiRestClient") RestClient client) {
        RestClientAdapter adapter = RestClientAdapter.create(client);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(GeminiInterface.class);
    }


}
