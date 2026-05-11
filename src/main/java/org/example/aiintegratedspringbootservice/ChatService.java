package org.example.aiintegratedspringbootservice;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.example.aiintegratedspringbootservice.exception.EmptyResponseException;
import org.example.aiintegratedspringbootservice.exception.FailedToGetAiResponseException;
import org.example.aiintegratedspringbootservice.exception.InvalidResponseFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Cache<String, List<Message>> cache;
    private static final int MEMORY_SIZE = 10;

    @Value("${openrouter.model}")
    private String model;

    @Value("${openrouter.api.url}")
    private String apiUrl;

    public ChatService(
            RestClient restClient,
            ObjectMapper objectMapper,
            @Value("${chat.session.ttl-minutes:30}") long ttlMinutes,
            @Value("${chat.session.max-size:1000}") long maxSize) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(ttlMinutes))
                .maximumSize(maxSize)
                .build();
    }

    public record Message(String role, String content) {}
    public record OpenRouterRequest(String model, List<Message> messages) {}
    public record OpenRouterResponse(List<Choice> choices) {}
    public record Choice(Message message) {}
    public record ApiChatRequest(String personality, String message, String sessionId) {}

    public String getAiResponse(List<Message> history, String personality, int memorySize) {

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

            OpenRouterResponse response = getOpenRouterResponse(rawResponse);

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
            case "pirate"   -> "You answer like a pirate. You only care about rum";
            case "coder"    -> "You are a senior developer. Explain clearly with Java examples.";
            case "rapper"   -> "You answer like a rapper. You are a helpful rapper. You make rhymes with everything in life";
            case "comedian" -> "You are a comedian. Make even the most serious topic a joke";
            default         -> "You are a helpful assistant.";
        };

        messages.add(new Message("system", systemContent));
        return messages;
    }

    private OpenRouterResponse getOpenRouterResponse(String rawResponse) {
        try {
            OpenRouterResponse response = objectMapper.readValue(rawResponse, OpenRouterResponse.class);
            logger.info("Incoming ChatResponse: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("Failed to deserialize OpenRouter response from JSON", e);
            throw new InvalidResponseFormatException("Invalid response format from OpenRouter.");
        }
    }

    public String chat(ApiChatRequest request) {
        String sessionId = (request.sessionId() == null || request.sessionId().isBlank())
                ? "default-session"
                : request.sessionId();

        final String[] aiAnswerHolder = new String[1];

        cache.asMap().compute(sessionId, (key, history) -> {
            if (history == null) history = new ArrayList<>();
            synchronized (history) {
                // Build a defensive snapshot with the pending user message.
                // The cached list is NOT mutated yet, so a failure leaves it clean.
                List<Message> requestSnapshot = new ArrayList<>(history);
                requestSnapshot.add(new Message("user", request.message()));

                // AI call runs inside compute → strict per-session serialization.
                // If this throws, compute propagates the exception and the cache
                // entry remains unchanged (no orphan user message).
                aiAnswerHolder[0] = getAiResponse(requestSnapshot, request.personality(), MEMORY_SIZE);

                // Commit both turns atomically only on success.
                history.add(new Message("user", request.message()));
                history.add(new Message("assistant", aiAnswerHolder[0]));
            }
            return history;
        });

        return aiAnswerHolder[0];
    }

    public Map<String, List<Message>> getMemory() {
        return cache.asMap();
    }
}
