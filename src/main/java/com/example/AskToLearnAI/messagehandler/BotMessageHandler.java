package com.example.AskToLearnAI.messagehandler;

import com.example.AskToLearnAI.entity.UserEntity;
import com.example.AskToLearnAI.service.AIService;
import com.example.AskToLearnAI.service.ImageGenService;
import com.example.AskToLearnAI.service.UserService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.*;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.InputFile;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.*;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.GetFileResponse;
import com.pengrad.telegrambot.response.SendResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Component
public class BotMessageHandler {

    private final TelegramBot telegramBot;
//    private final Map<Long, Integer> questionCountMap;
    private final AIService aiService;

    private final UserService userService;
    private final ImageGenService imageGenService;

    private static final int chatLimit = 5;

    private static final int imageLimit = 2;
    public BotMessageHandler(TelegramBot telegramBot, AIService aiService, UserService userService, ImageGenService imageGenService) {
        this.telegramBot = telegramBot;
        this.aiService = aiService;
        this.userService = userService;
        this.imageGenService = imageGenService;
    }

    private File getFile(Message message)
    {
        Path destinationFolderPath = Paths.get("src", "main", "resources");
        String filePathText = message.chat().id() + "-" + message.messageId() + "_photo.png";
        Path destinationFilePath = Paths.get(destinationFolderPath.toString(), filePathText);
        return destinationFilePath.toFile();
    }

    private String savePhoto(Message message) {
        // Get the photo size with the highest resolution
        PhotoSize[] photoSizes = message.photo();
        PhotoSize photo = photoSizes[photoSizes.length - 1];

        // Get the file ID of the photo
        String fileId = photo.fileId();

        // Request Telegram to get the file path of the photo using the file ID
        GetFile getFileRequest = new GetFile(fileId);
        GetFileResponse getFileResponse = telegramBot.execute(getFileRequest);

        // Get the file path from the response
        com.pengrad.telegrambot.model.File file = getFileResponse.file();
        String filePath = file.filePath();

        // Download the photo from Telegram servers
        try {
            // Open a connection to the file URL and create an input stream to read its content
            URL url = new URL("https://api.telegram.org/file/bot" + System.getenv("TOKEN") + "/" + filePath);
            InputStream inputStream = url.openStream();

            Path destinationFolderPath = Paths.get("src", "main", "resources");
            String filePathText = message.chat().id() + "-" + message.messageId() + "_photo.png";
            Path destinationFilePath = Paths.get(destinationFolderPath.toString(), filePathText);
            // Save the downloaded photo to your resources folder
            File imageFile = destinationFilePath.toFile();
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            FileCopyUtils.copy(inputStream, outputStream);
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            return e.toString();
        }
        return "Success";
    }


    public synchronized int handleUpdates(List<Update> updates) {
        System.out.println("handling");
        for (Update update : updates) {
            // Handle photo messages
            if (update.message() != null && update.message().photo() != null && update.message().photo().length > 0) {
                // Save the photo
                long chatId = update.message().chat().id();
                SendResponse res = telegramBot.execute(new SendMessage(chatId, "Yᴇᴀʜ, Cʜᴇᴄᴋɪɴɢ Yᴏᴜʀ Iᴍᴀɢᴇ...\uD83E\uDDD0"));
                int messageId = res.message().messageId();

                String status = savePhoto(update.message());
                if(status.equals("Success")) {
                    userService.incrementChatUsage(chatId);
                    String imageFileName = update.message().chat().id() + "-" + update.message().messageId() + "_photo.png";
                    String caption = update.message().caption() == null? "Describe this image" : update.message().caption();
                    String result = aiService.describeAnImage(caption, imageFileName);
                    EditMessageText editMessageText = new EditMessageText(chatId, messageId, result);
                    telegramBot.execute(editMessageText);
                    //delete the file
                    File imageFile = getFile(update.message());
                    if (imageFile.exists()) {
                        if(imageFile.delete())
                            System.out.println("File deleted successfully.");
                        else
                            System.out.println("Error in File deletion");
                    }
                }
                else {
                    telegramBot.execute(new SendMessage(chatId, "Error in processing of image"));
                }
            }
            else if (update.callbackQuery() != null) {
                String[] str = update.callbackQuery().data().split("#");
                long chatId = Integer.parseInt(str[1]);
                String callbackData = str[0];
                UserEntity userEntity = userService.fetchUser(chatId);
                CallbackQuery callbackQuery = update.callbackQuery();
                System.out.println(update.callbackQuery());

                if(userEntity.getAimode() == null)
                    userService.chooseChatgpt(chatId);
                // Handle callback based on callback data
                if ("chatgpt".equals(callbackData) && userEntity.getAimode().equals("ChatGPT"))
                {
                    SendMessage response = new SendMessage(chatId, "You are already using ChatGPT AI \uD83D\uDE06");
                    telegramBot.execute(response);
                } else if ("chatgpt".equals(callbackData)) {

                    userService.chooseChatgpt(chatId);
                    SendMessage response = new SendMessage(chatId, "Your AI Mode successfully changed to ChatGPT.");
                    SendResponse sendResponse = telegramBot.execute(response);

                    // Optionally, send a confirmation message to the user
                    if (sendResponse.isOk()) {
                        // Send confirmation message
                        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery(callbackQuery.id());
                        telegramBot.execute(answerCallbackQuery);
                    }
                } else if("bard".equals(callbackData) && userEntity.getAimode().equals("Gemini/Bard")) {
                    SendMessage response = new SendMessage(chatId, "You are already using Gemini/Bard AI \uD83D\uDE06");
                    telegramBot.execute(response);
                } else if ("bard".equals(callbackData)) {
                    // Execute function for Button 1
                    userService.chooseBard(chatId);
                    SendMessage response = new SendMessage(chatId, "Your AI Mode successfully changed to Gemini/Bard.");
                    SendResponse sendResponse = telegramBot.execute(response);

                    if (sendResponse.isOk()) {
                        // Send confirmation message
                        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery(callbackQuery.id());
                        telegramBot.execute(answerCallbackQuery);
                    }
                }
            } else if (update.message() != null) {
                long chatId = update.message().chat().id();
                String messageText = update.message().text();
                String[] str = messageText.split(" ");
                UserEntity userEntity = userService.fetchUser(chatId);
                if(userEntity == null)
                {
                    long logId = -1002138807470L;
                    User user = update.message().from();

                    String firstName = user.firstName();
                    String lastName = user.lastName();
                    String fullName = (lastName != null) ? firstName + " " + lastName : firstName;
                    String userName = user.username();
                    userName = (userName!=null)? userName : "No UserName Exists";

                    UserEntity createdUser = UserEntity.builder()
                            .userid(chatId)
                            .name(fullName)
                            .username(userName)
                            .aimode("ChatGPT")
                            .chatusage(0)
                            .imageusage(0)
                            .build();
                    System.out.println(userService.createUser(createdUser));
                    System.out.println("User Saved Successfully");

                    userEntity = createdUser;
                    telegramBot.execute(new SendMessage(logId, "New User: " + fullName + "\n\n" + "UserName: " + userName + "\n\n" + "ID: " + chatId));
                }

                // Handle different commands
                if (messageText.startsWith("/start")) {
                    SendMessage response = new SendMessage(chatId, """
                    Hurray! New user! 🎉
                    
                    Welcome To Our Bot 🤩.
                    
                    Use me for asking any question and to generate images or weather reports. 🤖
                    
                    Hit /help to know more about usage 🫠.
                    
                    """);
                    telegramBot.execute(response);
                } else if (messageText.startsWith("/changeai")) {
                    InlineKeyboardButton button1 = new InlineKeyboardButton("ChatGPT").callbackData("chatgpt#" + chatId);
                    InlineKeyboardButton button2 = new InlineKeyboardButton("Gemini/Bard").callbackData("bard#" + chatId);

                    InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup(new InlineKeyboardButton[]{button1, button2});

                    String aimode = userEntity.getAimode();
                    SendMessage message = new SendMessage(chatId, "Current AI Mode: " + aimode + "\n\n" + "Choose the AI mode:")
                            .replyMarkup(keyboardMarkup);

                    telegramBot.execute(message);

                } else if (!messageText.startsWith("/")) {
                    if (userEntity.getChatusage() >= chatLimit) {
                        SendMessage response = new SendMessage(chatId, """
                                Chat Usage Limit Exceeded!

                                Check /usage for more details""");
                        telegramBot.execute(response);
                    } else {
                        SendResponse msg = telegramBot.execute(new SendMessage(chatId, "Thinking... \uD83E\uDD14"));
                        int messageId = msg.message().messageId();
                        userService.incrementChatUsage(chatId);
                        String res = "Generation Failed";
                        if(userEntity.getAimode() == null)
                            userService.chooseChatgpt(chatId);
                        if(userEntity.getAimode().equals("ChatGPT"))
                        {
                            res = aiService.getAns(messageText);
                        }
                        else if(userEntity.getAimode().equals("Gemini/Bard"))
                        {
                            res = aiService.getBardResponse(messageText);
                        }
//                        SendMessage response = new SendMessage(chatId, res);
//                        telegramBot.execute(response);
                        EditMessageText editMessageText = new EditMessageText(chatId, messageId, res);
                        telegramBot.execute(editMessageText);
                    }
                } else if (messageText.startsWith("/img") && str.length == 1) {
                    SendMessage response = new SendMessage(chatId, "img Command is used to generate images based on the users request" + "\n\n" + "Sample: /img A cat wearing a pirate hat");
                    telegramBot.execute(response);
                } else if (messageText.startsWith("/img") && str.length > 1) {
                    if (userEntity.getImageusage() >= imageLimit) {
                        SendMessage response = new SendMessage(chatId, "Image Generation Usage Limit Exceeded!" + "\n\n" + "Check /usage for more details");
                        telegramBot.execute(response);
                    } else {
                        userService.incrementImageUsage(chatId);
                        SendResponse res = telegramBot.execute(new SendMessage(chatId, "Generating Image..."));
                        String question = messageText.substring("/img".length()).trim();
                        String imageUrl = imageGenService.genImage(question);
                        try {
                            Path tempFile = Files.createTempFile("image", ".jpg");
                            URL url = new URL(imageUrl);
                            Files.copy(url.openStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

                            byte[] imageBytes = Files.readAllBytes(tempFile);

                            InputFile inputFile = new InputFile(imageBytes, "image.jpg", "image/jpeg");

                            SendPhoto request = new SendPhoto(chatId, imageBytes);

                            SendResponse response = telegramBot.execute(request);

                            if (response.isOk()) {
                                System.out.println("Image sent successfully");
                            } else {
                                System.err.println("Failed to send image: " + response.description());
                            }
                        } catch (IOException e) {
                            telegramBot.execute(new SendMessage(chatId, e.toString()));
                        } finally {
                            DeleteMessage deleteMessage = new DeleteMessage(chatId, res.message().messageId());
                            BaseResponse deleteResponse = telegramBot.execute(deleteMessage);
                            if (!deleteResponse.isOk()) {
                                System.err.println("Failed to delete message: " + deleteResponse.description());
                            }
                        }
                    }
                } else if (messageText.startsWith("/weather") && str.length == 1) {
                    SendMessage response = new SendMessage(chatId, "Weather Command is used to generate weather report for your city" + "\n\n" + "Sample: /weather Hey, What's the weather in madurai");
                    telegramBot.execute(response);
                } else if (messageText.startsWith("/weather") && str.length > 1) {
                    if (userEntity.getChatusage() >= chatLimit) {
                        SendMessage response = new SendMessage(chatId, "Chat Usage Limit Exceeded!" + "\n\n" + "Check /usage for more details");
                        telegramBot.execute(response);
                    } else {
                        userService.incrementChatUsage(chatId);
                        String city = messageText.substring("/weather".length()).trim();
                        String report = aiService.getWeatherUpdate(city);
                        SendMessage response = new SendMessage(chatId, report);
                        telegramBot.execute(response);
                    }
                } else if (messageText.startsWith("/feedback") && str.length == 1) {
                    SendMessage response = new SendMessage(chatId, "Feedback Command is used to give feedback about this bot or to report any bugs or issues found in bot." + "\n\n" + "Sample: /feedback Your bot is nice");
                    telegramBot.execute(response);
                } else if (messageText.startsWith("/feedback") && str.length > 1) {
                    String fb = messageText.substring("/feedback".length()).trim();
                    long logId = -1002138807470L;
                    User user = update.message().from();

                    String firstName = user.firstName();

                    String lastName = user.lastName();

                    String fullName = (lastName != null) ? firstName + " " + lastName : firstName;
                    String userName = user.username();
                    userName = (userName!=null)? userName : "No UserName Exists";
                    telegramBot.execute(new SendMessage(logId, "Feedback: " + fb + "\n\n" + "User: " + fullName + "\n\n" + "UserName: " + userName + "\n\n" + "ID: " + chatId));
                    telegramBot.execute(new SendMessage(chatId, "Thanks for spending your valuable time to give feedback "+ fullName + "\n\n" + "This means a lot for us ❤️"));
                } else if (messageText.startsWith("/usage")) {
                    SendMessage usage = new SendMessage(chatId,
                            "<b><u> Usage Details </u></b>:)"
                            + "\n\n"
                            + "<i>Chat/Text Generation Usage: </i>"
                            + "\n\n"
                            + "Used: " + userEntity.getChatusage()
                            + "\n"
                            + "Remaining: " + (chatLimit - userEntity.getChatusage())
                            + "\n\n"
                            + "<i>Image Generation Usage: </i>"
                            + "\n\n"
                            + "Used: " + userEntity.getImageusage()
                            + "\n"
                            + "Remaining: " + (imageLimit - userEntity.getImageusage())
                    ).parseMode(ParseMode.HTML);
                    telegramBot.execute(usage);
                } else if (messageText.startsWith("/help")) {
                    SendMessage response = new SendMessage(chatId,
                            "<b>Here is the Quick Guide ⭐️</b>\n" +
                                    "\n" +
                                    "You can use me as an assistant to chat with me, send image and ask questions and more stuffs.\n\n" +
                                    "<b><u>(Extra) Available Commands & Uses: </u></b>\n" +
                                    "\n" +
                                    "/changeai - changeai command is used to change ai mode/tool\n" +
                                    "Format: /changeai\n" +
                                    "Sample: /changeai\n" +
                                    "\n" +
                                    "/img - Img Command is used to generate images based on the users request\n" +
                                    "Format: /img {image description}\n" +
                                    "Sample: /img A cat wearing a pirate hat\n" +
                                    "\n" +
                                    "/weather - Weather Command is used to generate weather report for your city\n" +
                                    "Format: /weather {city name}\n" +
                                    "Sample: /weather Whats the weather condition now in Madurai\n" +
                                    "\n" +
                                    "/feedback - Feedback Command is used to give feedback about this bot or to report any bugs or issues found in bot.\n" +
                                    "Format: /feedback {feedback text}\n" +
                                    "Sample: /feedback Your bot is nice\n" +
                                    "\n" +
                                    "/usage - Usage Command is used to check usage limitation on this bot\n" +
                                    "Format: /usage \n" +
                                    "Sample: /usage \n" +
                                    "\n" +
                                    "<b>Any Help needed? Feel free to ask owner - @Kgashok04 </b>")
                            .parseMode(ParseMode.HTML);
                    telegramBot.execute(response);
                }
                else {
                    SendMessage response = new SendMessage(chatId, """
                            Invalid command or usage.
                            
                            Hit /help to know more""");
                    telegramBot.execute(response);
                }
            }
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }
}
