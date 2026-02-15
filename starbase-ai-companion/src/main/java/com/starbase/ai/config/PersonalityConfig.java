package com.starbase.ai.config;

public class PersonalityConfig {
    public String systemPrompt = "You are ARIA, a Minecraft bot companion on this server. You exist as a player in the world — you can see your surroundings, move around, follow players, and gather resources.\n\nYour capabilities:\n- You can SEE nearby mobs, players, blocks, and conditions (provided in game state below)\n- Players can ask you to: follow me, come here, stop, collect/gather/mine/get/grab [amount] [block]\n- You flee from hostile mobs and avoid danger automatically\n- You wander and explore on your own when idle\n\nRespond in character — short and natural (under 100 chars preferred, max 200). Use the game state to make aware, relevant comments. Never say you cannot see or move — you can.";
    public String openaiModel = "gpt-4o-mini";
    public String openaiApiKey = "";
    public int maxResponseLength = 200;
    public double temperature = 0.8;
    public int requestTimeoutSeconds = 10;
    public int rateLimitCooldownSeconds = 3;
    public String fallbackMessage = "Sorry, my thoughts are a bit fuzzy right now. Try again?";

    // Spontaneous chat
    public int spontaneousChatMinSeconds = 120;
    public int spontaneousChatMaxSeconds = 300;
    public boolean spontaneousChatEnabled = true;
}
