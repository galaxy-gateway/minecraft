package com.starbase.ai.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.starbase.ai.StarbaseAIMod;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OpenAIClient {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private final String apiKey;
    private final String model;
    private final double temperature;
    private final int timeoutSeconds;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public OpenAIClient(String apiKey, String model, double temperature, int timeoutSeconds) {
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    public record ChatMessage(String role, String content) {}

    public CompletableFuture<String> chatAsync(List<ChatMessage> messages, int maxTokens) {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("max_tokens", maxTokens);
        body.addProperty("temperature", temperature);

        JsonArray messagesArray = new JsonArray();
        for (ChatMessage msg : messages) {
            JsonObject msgObj = new JsonObject();
            msgObj.addProperty("role", msg.role());
            msgObj.addProperty("content", msg.content());
            messagesArray.add(msgObj);
        }
        body.add("messages", messagesArray);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        StarbaseAIMod.LOGGER.error("OpenAI API error {}: {}",
                                response.statusCode(), response.body());
                        return null;
                    }
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    JsonArray choices = json.getAsJsonArray("choices");
                    if (choices == null || choices.isEmpty()) return null;
                    return choices.get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString();
                })
                .exceptionally(e -> {
                    StarbaseAIMod.LOGGER.error("OpenAI request failed", e);
                    return null;
                });
    }
}
