package com.example.AskToLearnAI.service;

import org.springframework.ai.image.ImageClient;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.stereotype.Service;

@Service
public class ImageGenService {

    private final ImageClient imageClient;

    public ImageGenService(ImageClient imageClient) {
        this.imageClient = imageClient;
    }
    public String genImage(String request) {
        ImagePrompt imagePrompt = new ImagePrompt(request);
        ImageResponse response = imageClient.call(imagePrompt);
        String imageUrl = response.getResult().getOutput().getUrl();
        System.out.println(imageUrl);
        return imageUrl;
    }

}
