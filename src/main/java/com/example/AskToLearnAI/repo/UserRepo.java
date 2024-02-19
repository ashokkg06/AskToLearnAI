package com.example.AskToLearnAI.repo;

import com.example.AskToLearnAI.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepo extends JpaRepository<UserEntity, Long> {
    UserEntity findByUserid(long userid);
}
