package org.example.aiintegratedspringbootservice;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService){
        this.chatService = chatService;
    }

    @PostMapping
    public String chat(@RequestBody ChatService.ApiChatRequest request){
        return chatService.chat(request);
    }
}
