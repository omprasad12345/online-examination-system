
package com.example.exam.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageRequest {

    private Long receiverId;

    private String content;
}
