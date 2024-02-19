package com.example.AskToLearnAI.service;

import com.example.AskToLearnAI.gemini.GeminiService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Map;

@Service
public class AIService {

    private final ChatClient chatClient;

    private final OpenAiChatClient openAiChatClient;

    private final GeminiService geminiService;

    @Value("${app.id}")
    private String APPID;

    public AIService(ChatClient chatClient, OpenAiChatClient openAiChatClient, GeminiService geminiService) {
        this.chatClient = chatClient;
        this.openAiChatClient = openAiChatClient;
        this.geminiService = geminiService;
    }

    public String getJoke(String topic) {
        PromptTemplate promptTemplate = new PromptTemplate(topic);

        return this.chatClient.call(promptTemplate.getTemplate());
    }

    public String getAnswer(String industry, String position, String skill) {
        PromptTemplate promptTemplate = new PromptTemplate("""
                I want you to act as an interviewer for {industry} industry. 
                I’m applying for {position} position. 
                Ask me the interview questions related to {skill}. My first sentence is “Hi”.
                """);

        Prompt prompt = promptTemplate.create(Map.of("industry", industry, "position", position, "skill" , skill));

        return this.chatClient.call(prompt.getContents());
    }

    public String getAns(String question) {
        PromptTemplate promptTemplate = new PromptTemplate(question);

        return this.chatClient.call(promptTemplate.getTemplate());
    }

    public String getWeatherUpdate(String input) {

        Prompt prompts = new Prompt(String.valueOf(List.of("\"" + input + "\"", "from this text, guess only the city name, if there is no city name, say as 'No city Name Found in your text, give me only the city name to take weather conditions'")));

        System.out.println("Prompt: " + prompts.getInstructions());
        ChatResponse res = this.openAiChatClient.call(prompts);

        String answer = res.getResult().getOutput().getContent();

        System.out.println("City info: " + answer);
        if(answer.toLowerCase().contains("sorry") || answer.toLowerCase().contains("no city"))
            return answer;

        String userText = getJson(answer);

        System.out.println("Data found info: " + userText);
        if(userText.equals("Sorry, No city data found"))
            return userText;

        Message userMessage = new UserMessage(userText);

        String systemText = """
            You are a helpful AI assistant that helps people find information. Your task is to get the json string and give it in readable format like how you read weather information and not in json format. please convert all kelvin units to celsius for better understanding.           \s
        """;

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemText);
        Message systemMessage = systemPromptTemplate.createMessage();

        Prompt prompt = new Prompt(List.of(userMessage, systemMessage));

        List<Generation> response = chatClient.call(prompt).getResults();

        return response.get(0).getOutput().getContent();
    }

    public String getJson(String city) {
        OkHttpClient client = new OkHttpClient();
        String url = "http://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + APPID;
        Request request = new Request.Builder()
                .url(url)
                .build();

        try {
            Response response = client.newCall(request).execute();
            System.out.println(response.message());
            return response.message().equals("Not Found") ? "Sorry, No city data found" : response.body().string();
//            Example of getting temperature from json,
//                JSONObject jsonObject = new JSONObject(s);
//                JSONObject mainObject = jsonObject.getJSONObject("main");

            // Get the "temp" value as a double
//                double temp = mainObject.getDouble("temp");
            //{"visibility":10000,"timezone":19800,"main":{"temp":297.98,"temp_min":297.98,"grnd_level":1000,"humidity":68,"pressure":1016,"sea_level":1016,"feels_like":298.29,"temp_max":297.98},"clouds":{"all":29},"sys":{"country":"IN","sunrise":1707872871,"sunset":1707915350},"dt":1707930837,"coord":{"lon":78.1167,"lat":9.9333},"weather":[{"icon":"03n","description":"scattered clouds","main":"Clouds","id":802}],"name":"Madurai","cod":200,"id":1264521,"base":"stations","wind":{"deg":54,"speed":5.02,"gust":9.01}}
//                System.out.println("Temperature: " + temp);

        } catch (HttpClientErrorException.Unauthorized e) {
            System.out.println(e.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "No data found";
    }

    public String describeAnImage(String caption, String imageFile) {
        try {
            String text = geminiService.getCompletionWithImage(
                    caption,
                    imageFile);
            return text;
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    public String getBardResponse(String question) {
        try {
            return geminiService.getCompletion(question);
        } catch (Exception ex) {
            return ex.toString();
        }
    }

}

