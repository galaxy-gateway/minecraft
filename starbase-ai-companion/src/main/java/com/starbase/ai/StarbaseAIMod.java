package com.starbase.ai;

import com.starbase.ai.ai.OpenAIClient;
import com.starbase.ai.bot.FakePlayerManager;
import com.starbase.ai.chat.ChatListener;
import com.starbase.ai.chat.RateLimiter;
import com.starbase.ai.config.ModConfig;
import com.starbase.ai.config.PersonalityConfig;
import com.starbase.ai.memory.MemoryStore;
import com.starbase.ai.memory.MemorySummarizer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StarbaseAIMod implements DedicatedServerModInitializer {
    public static final String MOD_ID = "starbase-ai";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ModConfig CONFIG = new ModConfig();

    @Override
    public void onInitializeServer() {
        LOGGER.info("Starbase AI Companion loading...");
        CONFIG.load();

        PersonalityConfig personality = CONFIG.personality();

        if (personality.openaiApiKey.isEmpty()) {
            LOGGER.error("No OpenAI API key configured! Bot will not respond to messages.");
            LOGGER.error("Set your key in config/starbase-ai/personality.json");
        }

        FakePlayerManager fakePlayerManager = new FakePlayerManager();
        MemoryStore memoryStore = new MemoryStore(CONFIG.configDir());
        OpenAIClient openAIClient = new OpenAIClient(
                personality.openaiApiKey,
                personality.openaiModel,
                personality.temperature,
                personality.requestTimeoutSeconds);
        RateLimiter rateLimiter = new RateLimiter(personality.rateLimitCooldownSeconds);
        MemorySummarizer memorySummarizer = new MemorySummarizer(openAIClient, memoryStore);

        // Register chat listener
        ChatListener chatListener = new ChatListener(
                fakePlayerManager, openAIClient, memoryStore, memorySummarizer, rateLimiter);
        chatListener.register();

        // Spawn bot when server starts
        ServerLifecycleEvents.SERVER_STARTED.register(fakePlayerManager::spawn);

        // Respawn bot if killed
        ServerTickEvents.END_SERVER_TICK.register(fakePlayerManager::tick);

        // Despawn bot when server stops
        ServerLifecycleEvents.SERVER_STOPPING.register(fakePlayerManager::despawn);

        LOGGER.info("Starbase AI Companion loaded! Bot name: {}", CONFIG.bot().botName);
    }
}
