package com.starbase.ai.memory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.starbase.ai.StarbaseAIMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class MemoryStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_CONVERSATIONS = 50;

    private final Path memoryDir;
    private final ConcurrentHashMap<String, PlayerMemory> cache = new ConcurrentHashMap<>();

    public MemoryStore(Path configDir) {
        this.memoryDir = configDir.resolve("memory");
        try {
            Files.createDirectories(memoryDir);
        } catch (IOException e) {
            StarbaseAIMod.LOGGER.error("Failed to create memory directory", e);
        }
    }

    public PlayerMemory getMemory(String playerName) {
        return cache.computeIfAbsent(playerName.toLowerCase(), name -> {
            Path file = memoryDir.resolve(name + ".json");
            if (Files.exists(file)) {
                try {
                    return GSON.fromJson(Files.readString(file), PlayerMemory.class);
                } catch (IOException e) {
                    StarbaseAIMod.LOGGER.error("Failed to load memory for {}", name, e);
                }
            }
            PlayerMemory mem = new PlayerMemory();
            mem.playerName = playerName;
            mem.firstSeen = Instant.now().toString();
            return mem;
        });
    }

    public void addConversation(String playerName, String playerMessage, String botResponse,
                                 String biome, String timeOfDay) {
        PlayerMemory mem = getMemory(playerName);
        mem.conversations.add(new PlayerMemory.ConversationEntry(
                Instant.now().toString(), playerMessage, botResponse, biome, timeOfDay));

        // Trim to max
        while (mem.conversations.size() > MAX_CONVERSATIONS) {
            mem.conversations.remove(0);
        }

        mem.messagesSinceSummary++;
        save(playerName, mem);
    }

    public void save(String playerName) {
        PlayerMemory mem = cache.get(playerName.toLowerCase());
        if (mem != null) {
            save(playerName, mem);
        }
    }

    private void save(String playerName, PlayerMemory mem) {
        try {
            Path file = memoryDir.resolve(playerName.toLowerCase() + ".json");
            Files.writeString(file, GSON.toJson(mem));
        } catch (IOException e) {
            StarbaseAIMod.LOGGER.error("Failed to save memory for {}", playerName, e);
        }
    }
}
