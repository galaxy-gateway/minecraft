package com.starbase.ai.memory;

import com.starbase.ai.StarbaseAIMod;
import com.starbase.ai.ai.OpenAIClient;

import java.util.ArrayList;
import java.util.List;

public class MemorySummarizer {
    private static final int SUMMARIZE_EVERY = 20;
    private final OpenAIClient openAIClient;
    private final MemoryStore memoryStore;

    public MemorySummarizer(OpenAIClient openAIClient, MemoryStore memoryStore) {
        this.openAIClient = openAIClient;
        this.memoryStore = memoryStore;
    }

    public void summarizeIfNeeded(String playerName) {
        PlayerMemory memory = memoryStore.getMemory(playerName);
        if (memory.messagesSinceSummary < SUMMARIZE_EVERY) return;

        StringBuilder convoText = new StringBuilder();
        for (PlayerMemory.ConversationEntry entry : memory.conversations) {
            convoText.append("Player: ").append(entry.playerMessage).append("\n");
            convoText.append("Bot: ").append(entry.botResponse).append("\n\n");
        }

        List<OpenAIClient.ChatMessage> messages = new ArrayList<>();
        messages.add(new OpenAIClient.ChatMessage("system",
                "Summarize the following conversations with " + playerName +
                " into a single paragraph. Focus on: what the player likes to do, " +
                "what they asked about, their play style, and any notable events. " +
                "Keep it under 200 words."));
        messages.add(new OpenAIClient.ChatMessage("user", convoText.toString()));

        openAIClient.chatAsync(messages, 300)
                .thenAccept(summary -> {
                    if (summary != null) {
                        memory.summary = summary;
                        memory.messagesSinceSummary = 0;
                        memoryStore.save(playerName);
                        StarbaseAIMod.LOGGER.info("Updated memory summary for {}", playerName);
                    }
                });
    }
}
