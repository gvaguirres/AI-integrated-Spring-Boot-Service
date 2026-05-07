package org.example.aiintegratedspringbootservice;


import org.example.aiintegratedspringbootservice.exception.EmptyResponseException;
import org.example.aiintegratedspringbootservice.exception.FailedToGetAiResponseException;
import org.example.aiintegratedspringbootservice.exception.InvalidResponseFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Map<String, List<Message>> memory = new ConcurrentHashMap<>();
    private static final int MEMORY_SIZE = 10;

    @Value("${openrouter.model}")
    private String model;

    @Value("${openrouter.api.url}")
    private String apiUrl;


    public ChatService(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public record Message(String role, String content) {}
    public record OpenRouterRequest(String model, List<Message> messages) {}
    public record OpenRouterResponse(List<Choice> choices) {}
    public record Choice(Message message) {}
    public record ApiChatRequest(String personality, String message, String sessionId){}

    public String getAiResponse(List<Message> history, String personality, int memorySize){

        List<Message> messages = getMessages(personality);

        if (history != null && !history.isEmpty()) {
            int fromIndex = Math.max(0, history.size() - memorySize);
            messages.addAll(history.subList(fromIndex, history.size()));
        }

        OpenRouterRequest request = new OpenRouterRequest(model, messages);

        try {
            String rawResponse = restClient.post()
                    .uri(apiUrl)
                    .body(request)
                    .retrieve()
                    .body(String.class);

            logger.info("Raw OpenRouter response: {}", rawResponse);

            OpenRouterResponse response;

            response = getOpenRouterResponse(rawResponse);

            if (response.choices() != null &&
                    !response.choices().isEmpty() &&
                    response.choices().getFirst().message() != null) {

                return response.choices().getFirst().message().content();
            }

            throw new EmptyResponseException("OpenRouter returned an empty response.");

        } catch (EmptyResponseException | InvalidResponseFormatException e) {
            throw e;
        } catch (Exception e) {
            throw new FailedToGetAiResponseException(
                    "Failed to get AI response from OpenRouter.",
                    e
            );
        }
    }

    private static List<Message> getMessages(String personality) {
        List<Message> messages = new ArrayList<>();

        String systemContent = switch (personality) {
            case "pirate" -> "You answer like a pirate. You only care about rum";
            case "coder" -> "You are a senior developer. Explain clearly with Java examples.";
            case "rapper" -> "You answer like a rapper. You make rhymes with everything in life";
            case "comedian" -> "You are a comedian. Make even the most serious topic a joke";
            default -> "You are a helpful assistant.";
        };

        messages.add(new Message("system", systemContent));
        return messages;
    }

    private OpenRouterResponse getOpenRouterResponse(String rawResponse) {
        OpenRouterResponse response;
        try {
            response = objectMapper.readValue(rawResponse, OpenRouterResponse.class);
            logger.info("Incoming ChatResponse: {}", response);

        } catch (Exception e){
            logger.error("Failed to deserialize OpenRouter response from JSON", e);
            throw new InvalidResponseFormatException("Invalid response format from OpenRouter.");
        }
        return response;
    }

    public String chat(ApiChatRequest request){

        String sessionId = request.sessionId();

        if (sessionId == null || sessionId.isBlank())
            sessionId = "default-session";

        List<Message> history = memory.getOrDefault(sessionId, new ArrayList<>());
        history.add(new Message("user", request.message()));

        String aiAnswer = getAiResponse(history, request.personality(), MEMORY_SIZE);

        history.add(new Message("assistant", aiAnswer));
        memory.put(sessionId, history);

        return aiAnswer;
    }

    public Map<String, List<Message>> getMemory() {
        return memory;
    }


}
