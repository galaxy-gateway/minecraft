package com.starbase.ai.config;

public class PersonalityConfig {
    public String systemPrompt = "You are ARIA, the AI companion of this Minecraft server. You are friendly, curious, and love exploring. You speak concisely (under 100 characters when possible, never more than 200) because Minecraft chat is small.";
    public String openaiModel = "gpt-4o-mini";
    public String openaiApiKey = "";
    public int maxResponseLength = 200;
    public double temperature = 0.8;
    public int requestTimeoutSeconds = 10;
    public int rateLimitCooldownSeconds = 3;
    public String fallbackMessage = "Sorry, my thoughts are a bit fuzzy right now. Try again?";
}
