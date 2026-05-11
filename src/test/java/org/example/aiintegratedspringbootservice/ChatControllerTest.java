package org.example.aiintegratedspringbootservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ChatService chatService;

    @Test
    @DisplayName("Should return chat response when valid request is sent")
    void shouldReturnChatResponseWhenValidRequestIsSent() throws Exception {

        Mockito.when(chatService.chat(Mockito.any(ChatService.ApiChatRequest.class)))
                .thenReturn("Hello from mocked service!");

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "personality": "pirate",
                              "message": "Hello",
                              "sessionId": "test-session"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello from mocked service!"));
    }
}
