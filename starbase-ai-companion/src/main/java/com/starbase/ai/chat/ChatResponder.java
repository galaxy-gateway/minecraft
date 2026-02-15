package com.starbase.ai.chat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

public class ChatResponder {

    public static void sendBotMessage(MinecraftServer server, String botName, String message) {
        int maxLen = 200;
        if (message.length() <= maxLen) {
            broadcast(server, botName, message);
            return;
        }

        int start = 0;
        while (start < message.length()) {
            int end = Math.min(start + maxLen, message.length());
            if (end < message.length()) {
                int sentenceBreak = message.lastIndexOf(". ", end);
                if (sentenceBreak > start) {
                    end = sentenceBreak + 1;
                } else {
                    int wordBreak = message.lastIndexOf(' ', end);
                    if (wordBreak > start) {
                        end = wordBreak;
                    }
                }
            }
            broadcast(server, botName, message.substring(start, end).trim());
            start = end;
        }
    }

    private static void broadcast(MinecraftServer server, String botName, String text) {
        server.getPlayerManager().broadcast(
                Text.literal("<" + botName + "> " + text), false);
    }
}
