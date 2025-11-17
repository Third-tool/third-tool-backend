package com.example.thirdtool.Card.presentation.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class WriteCardDto {
    private String question;
    private String answer;
    private List<MultipartFile> questionImages;
    private List<MultipartFile> answerImages;
}