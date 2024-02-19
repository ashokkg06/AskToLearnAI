package com.example.AskToLearnAI.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "userentity")
@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor()
public class UserEntity {

    @Id
    @GeneratedValue
    private Long id;

    private long userid;

    private String name;

    private String username;

    private int chatusage;

    private int imageusage;

    private String aimode;
}
