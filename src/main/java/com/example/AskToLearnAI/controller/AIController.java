package com.example.AskToLearnAI.controller;

import com.example.AskToLearnAI.service.AIService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }


    @GetMapping("/ask")
    public String getJoke(@RequestParam String industry, @RequestParam String position, @RequestParam String skill) {
        return aiService.getAnswer(industry, position, skill);
    }

    @GetMapping("/weatherInfo")
    public String getWeatherInfo(@RequestParam String weatherUpdate) {
        return aiService.getWeatherUpdate(weatherUpdate);
    }
}

