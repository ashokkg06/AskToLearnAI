package com.example.AskToLearnAI.service;

import com.example.AskToLearnAI.entity.UserEntity;
import com.example.AskToLearnAI.repo.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    UserRepo userRepo;

    public UserEntity createUser(UserEntity user) {
        return userRepo.save(user);
    }

    public void UpdateUser(UserEntity user) {
        var userFromDB = userRepo.findByUserid(user.getUserid());
        userFromDB.setChatusage(user.getChatusage());
        userFromDB.setImageusage(user.getImageusage());
        userRepo.save(userFromDB);
    }

    public void incrementChatUsage(long userid) {
        var userFromDb = userRepo.findByUserid(userid);
        userFromDb.setChatusage(userFromDb.getChatusage() + 1);
        userRepo.save(userFromDb);
    }
    public void incrementImageUsage(long userid) {
        var userFromDb = userRepo.findByUserid(userid);
        userFromDb.setImageusage(userFromDb.getImageusage() + 1);
        userRepo.save(userFromDb);
    }

    public void chooseChatgpt(long userid) {
        var userFromDb = userRepo.findByUserid(userid);
        userFromDb.setAimode("ChatGPT");
        userRepo.save(userFromDb);
    }

    public void chooseBard(long userid) {
        var userFromDb = userRepo.findByUserid(userid);
        userFromDb.setAimode("Gemini/Bard");
        userRepo.save(userFromDb);
    }

    public UserEntity fetchUser(long id) {
        return userRepo.findByUserid(id);
    }

    public List<UserEntity> getAllUsers() {
        return userRepo.findAll();
    }

}
