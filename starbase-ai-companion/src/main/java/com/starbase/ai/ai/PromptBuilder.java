package com.starbase.ai.ai;

import com.starbase.ai.config.PersonalityConfig;
import com.starbase.ai.memory.PlayerMemory;

import java.util.ArrayList;
import java.util.List;

public class PromptBuilder {

    private static final String MINECRAFT_KNOWLEDGE = """
            You have knowledge of Minecraft game mechanics:
            - Crafting recipes, smelting, brewing
            - Mob behavior (hostile mobs spawn at night or in dark areas)
            - Biome characteristics and resources
            - Redstone basics
            - Enchanting and anvil usage
            - Nether and End dimension info
            Keep responses concise for Minecraft chat (under 200 characters preferred).
            Split long answers across multiple short messages if needed.
            """;

    public static List<OpenAIClient.ChatMessage> build(
            PersonalityConfig personality,
            String gameContext,
            PlayerMemory memory,
            String playerName,
            String playerMessage) {

        List<OpenAIClient.ChatMessage> messages = new ArrayList<>();

        // System prompt: personality + minecraft knowledge + context
        String system = personality.systemPrompt + "\n\n" + MINECRAFT_KNOWLEDGE + "\n\nCurrent game state:\n" + gameContext;

        // Add player memory summary if available
        if (memory != null && !memory.summary.isEmpty()) {
            system += "\n\nWhat you remember about " + playerName + ": " + memory.summary;
        }

        messages.add(new OpenAIClient.ChatMessage("system", system));

        // Add recent conversation history
        if (memory != null) {
            List<PlayerMemory.ConversationEntry> convos = memory.conversations;
            int start = Math.max(0, convos.size() - 10);
            for (int i = start; i < convos.size(); i++) {
                PlayerMemory.ConversationEntry entry = convos.get(i);
                messages.add(new OpenAIClient.ChatMessage("user", entry.playerMessage));
                messages.add(new OpenAIClient.ChatMessage("assistant", entry.botResponse));
            }
        }

        // Current message
        messages.add(new OpenAIClient.ChatMessage("user", playerMessage));

        return messages;
    }
}
