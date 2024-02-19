package com.example.AskToLearnAI.controller;

import com.example.AskToLearnAI.service.ImageGenRequest;
import org.springframework.ai.image.ImageClient;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
public class ImageGenController {

    private final ImageClient imageClient;

    public ImageGenController(ImageClient imageClient) {
        this.imageClient = imageClient;
    }

    @PostMapping("/imagegen")
    public String imageGen(@RequestBody ImageGenRequest request) {
//        ImageOptions options = ImageOptionsBuilder.builder()
//                .withModel("dall-e-3")
//                .build();

        System.out.println(request.prompt());
        ImagePrompt imagePrompt = new ImagePrompt(request.prompt());
        System.out.println(imagePrompt);
        ImageResponse response = imageClient.call(imagePrompt);
        String imageUrl = response.getResult().getOutput().getUrl();
        System.out.println(imageUrl);
        return imageUrl;
    }
}
