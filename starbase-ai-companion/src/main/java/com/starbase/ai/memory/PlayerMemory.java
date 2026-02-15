package com.starbase.ai.memory;

import java.util.ArrayList;
import java.util.List;

public class PlayerMemory {
    public String playerName;
    public String firstSeen;
    public String summary = "";
    public List<ConversationEntry> conversations = new ArrayList<>();
    public int messagesSinceSummary = 0;

    public static class ConversationEntry {
        public String timestamp;
        public String playerMessage;
        public String botResponse;
        public String biome;
        public String timeOfDay;

        public ConversationEntry(String timestamp, String playerMessage, String botResponse,
                                  String biome, String timeOfDay) {
            this.timestamp = timestamp;
            this.playerMessage = playerMessage;
            this.botResponse = botResponse;
            this.biome = biome;
            this.timeOfDay = timeOfDay;
        }
    }
}
