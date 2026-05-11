package org.example.aiintegratedspringbootservice;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.example.aiintegratedspringbootservice.exception.EmptyResponseException;
import org.example.aiintegratedspringbootservice.exception.FailedToGetAiResponseException;
import org.example.aiintegratedspringbootservice.exception.InvalidResponseFormatException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AiIntegratedSpringBootServiceApplicationTests {

   static WireMockServer wireMockServer;

    @Autowired
    ChatService chatService;

    @BeforeAll
    static void setup() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void tearDown() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
        chatService.getMemory().clear();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry){
        registry.add("openrouter.api.url", () -> wireMockServer.baseUrl() + "/chat/completions");
        registry.add("openrouter.api.key", () ->  "test-key");
        registry.add("openrouter.model", () -> "openai/gpt-4o-mini");
        registry.add("openrouter.base-url", () -> "http://localhost:" + wireMockServer.port());
    }

    @Test
    @DisplayName("Verifies that the ChatService correctly calls the AI API and returns the response")
    void shouldReturnAiResponseWhenValidRequestIsSent() {

        wireMockServer.stubFor(post(urlEqualTo("/chat/completions"))
                .withHeader("Authorization", equalTo("Bearer test-key"))
                .withHeader("Content-Type", containing("application/json"))
                .willReturn(okJson("""
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "Hello from mocked OpenRouter!"
                  }
                }
              ]
            }
            """)));

        ChatService.ApiChatRequest request = new ChatService.ApiChatRequest(
                "pirate",
                "Hello",
                "test-session"
        );

        String response = chatService.chat(request);

        assertThat(response).contains("Hello from mocked OpenRouter!");
    }

    @Test
    @DisplayName("Should use default session when sessionId is null")
    void shouldUseDefaultSessionWhenSessionIdIsNull() {

        wireMockServer.stubFor(post(urlEqualTo("/chat/completions"))
                .withHeader("Authorization", equalTo("Bearer test-key"))
                .withHeader("Content-Type", containing("application/json"))
                .willReturn(okJson("""
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "Hello from mocked OpenRouter!"
                  }
                }
              ]
            }
            """)));

        ChatService.ApiChatRequest request = new ChatService.ApiChatRequest(
                "comedian",
                "Hello",
                null
        );

        String response = chatService.chat(request);

        assertNotNull(response);
        assertThat(chatService.getMemory().containsKey("default-session")).isTrue();
    }

    @Test
    @DisplayName("Should use default session when sessionId is blank")
    void shouldUseDefaultSessionWhenSessionIdIsBlank() {

        wireMockServer.stubFor(post(urlEqualTo("/chat/completions"))
                .withHeader("Authorization", equalTo("Bearer test-key"))
                .withHeader("Content-Type", containing("application/json"))
                .willReturn(okJson("""
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "Hello from mocked OpenRouter!"
                  }
                }
              ]
            }
            """)));

        ChatService.ApiChatRequest request = new ChatService.ApiChatRequest(
                "rapper",
                "Hello",
                " "

        );

        String response = chatService.chat(request);

        assertNotNull(response);
        assertThat(chatService.getMemory().containsKey("default-session")).isTrue();
    }

    @Test
    @DisplayName("Should throw EmptyResponseException when OpenRouter returns null choices")
    void shouldThrowEmptyResponseException_WhenChoicesIsNull(){

        wireMockServer.stubFor(post(urlEqualTo("/chat/completions"))
                .withHeader("Authorization", equalTo("Bearer test-key"))
                .withHeader("Content-Type", containing("application/json"))
                .willReturn(okJson("""
            {
              "choices": null
            }
            """)));

        ChatService.ApiChatRequest request = new ChatService.ApiChatRequest(
                "pirate",
                "Hello",
                "test-session"

        );

        assertThatThrownBy( () -> chatService.chat(request))
                .isInstanceOf(EmptyResponseException.class)
                .hasMessageContaining("OpenRouter returned an empty response.");
    }

    @Test
    @DisplayName("Should throw FailedToGetAiResponseException when OpenRouter returns server error")
    void shouldThrowFailedToGetAiResponseException_WhenServerError(){

        wireMockServer.stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(serverError()));

        ChatService.ApiChatRequest request = new ChatService.ApiChatRequest(
                "coder",
                "Hello",
                "test-session"

        );

        assertThatThrownBy( () -> chatService.chat(request))
                .isInstanceOf(FailedToGetAiResponseException.class)
                .hasMessageContaining("Failed to get AI response from OpenRouter.");
    }

    @Test
    @DisplayName("Should throw InvalidResponseFormatException when fails to deserialize OpenRouter response from JSON")
    void shouldThrowInvalidResponseFormatException_whenFailsDeserialization(){

        wireMockServer.stubFor(post(urlEqualTo("/chat/completions"))
                .withHeader("Authorization", equalTo("Bearer test-key"))
                .withHeader("Content-Type", containing("application/json"))
                .willReturn(ok("""
            {
              "choices": [
                {
                  "message": {
                    "role": "assistant",
                    "content": "Hello from mocked OpenRouter!"
                  }
                }
          """)));

        ChatService.ApiChatRequest request = new ChatService.ApiChatRequest(
                "rapper",
                "Hello",
                " "

        );

        assertThatThrownBy( () -> chatService.chat(request))
                .isInstanceOf(InvalidResponseFormatException.class)
                .hasMessageContaining("Invalid response format from OpenRouter.");
    }

}
