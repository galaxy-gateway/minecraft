package com.starbase.ai.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.starbase.ai.StarbaseAIMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config", "starbase-ai");

    private BotConfig botConfig;
    private PersonalityConfig personalityConfig;
    private BehaviorConfig behaviorConfig;

    public void load() {
        try {
            Files.createDirectories(CONFIG_DIR);
            botConfig = loadOrCreate("bot.json", BotConfig.class);
            personalityConfig = loadOrCreate("personality.json", PersonalityConfig.class);
            behaviorConfig = loadOrCreate("behavior.json", BehaviorConfig.class);

            // Migrate old default prompt to new capabilities-aware prompt
            if (personalityConfig.systemPrompt != null
                    && personalityConfig.systemPrompt.startsWith("You are ARIA, the AI companion")) {
                PersonalityConfig fresh = new PersonalityConfig();
                personalityConfig.systemPrompt = fresh.systemPrompt;
                saveConfig("personality.json", personalityConfig);
                StarbaseAIMod.LOGGER.info("Migrated personality.json system prompt to include capabilities");
            }

            if (personalityConfig.openaiApiKey.isEmpty()) {
                StarbaseAIMod.LOGGER.warn("OpenAI API key not set in config/starbase-ai/personality.json!");
            }
        } catch (IOException e) {
            StarbaseAIMod.LOGGER.error("Failed to load config", e);
            botConfig = new BotConfig();
            personalityConfig = new PersonalityConfig();
            behaviorConfig = new BehaviorConfig();
        }
    }

    private <T> T loadOrCreate(String filename, Class<T> clazz) throws IOException {
        Path path = CONFIG_DIR.resolve(filename);
        if (Files.exists(path)) {
            String json = Files.readString(path);
            return GSON.fromJson(json, clazz);
        } else {
            T defaults;
            try {
                defaults = clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IOException("Cannot create default config for " + clazz.getName(), e);
            }
            Files.writeString(path, GSON.toJson(defaults));
            StarbaseAIMod.LOGGER.info("Created default config: {}", path);
            return defaults;
        }
    }

    public <T> void saveConfig(String filename, T config) throws IOException {
        Path path = CONFIG_DIR.resolve(filename);
        Files.writeString(path, GSON.toJson(config));
    }

    public void reload() {
        load();
    }

    public BotConfig bot() { return botConfig; }
    public PersonalityConfig personality() { return personalityConfig; }
    public BehaviorConfig behavior() { return behaviorConfig; }
    public Path configDir() { return CONFIG_DIR; }
}
