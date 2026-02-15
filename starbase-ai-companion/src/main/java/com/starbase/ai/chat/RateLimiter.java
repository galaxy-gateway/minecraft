package com.starbase.ai.chat;

import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {
    private final long cooldownMs;
    private final ConcurrentHashMap<String, Long> lastMessageTime = new ConcurrentHashMap<>();

    public RateLimiter(int cooldownSeconds) {
        this.cooldownMs = cooldownSeconds * 1000L;
    }

    public boolean isAllowed(String playerName) {
        long now = System.currentTimeMillis();
        Long last = lastMessageTime.get(playerName.toLowerCase());
        if (last != null && (now - last) < cooldownMs) {
            return false;
        }
        lastMessageTime.put(playerName.toLowerCase(), now);
        return true;
    }
}
