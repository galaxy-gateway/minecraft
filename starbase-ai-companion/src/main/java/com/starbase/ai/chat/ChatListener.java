package com.starbase.ai.chat;

import com.starbase.ai.StarbaseAIMod;
import com.starbase.ai.ai.GameContextCollector;
import com.starbase.ai.ai.OpenAIClient;
import com.starbase.ai.ai.PromptBuilder;
import com.starbase.ai.bot.FakeBotPlayer;
import com.starbase.ai.bot.FakePlayerManager;
import com.starbase.ai.config.PersonalityConfig;
import com.starbase.ai.memory.MemoryStore;
import com.starbase.ai.memory.MemorySummarizer;
import com.starbase.ai.memory.PlayerMemory;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.biome.Biome;

import java.util.List;

public class ChatListener {
    private final FakePlayerManager fakePlayerManager;
    private final OpenAIClient openAIClient;
    private final MemoryStore memoryStore;
    private final MemorySummarizer memorySummarizer;
    private final RateLimiter rateLimiter;

    public ChatListener(FakePlayerManager fakePlayerManager, OpenAIClient openAIClient,
                         MemoryStore memoryStore, MemorySummarizer memorySummarizer,
                         RateLimiter rateLimiter) {
        this.fakePlayerManager = fakePlayerManager;
        this.openAIClient = openAIClient;
        this.memoryStore = memoryStore;
        this.memorySummarizer = memorySummarizer;
        this.rateLimiter = rateLimiter;
    }

    public void register() {
        String botName = StarbaseAIMod.CONFIG.bot().botName;
        String prefix = "@" + botName.toLowerCase();

        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String content = message.getContent().getString();
            if (!content.toLowerCase().startsWith(prefix)) return;

            String playerMessage = content.substring(prefix.length()).trim();
            if (playerMessage.isEmpty()) return;

            String playerName = sender.getName().getString();
            MinecraftServer server = sender.getServer();
            PersonalityConfig personality = StarbaseAIMod.CONFIG.personality();

            // Rate limit check
            if (!rateLimiter.isAllowed(playerName)) {
                server.execute(() -> ChatResponder.sendBotMessage(
                        server, botName, "Give me a moment..."));
                return;
            }

            // Gather context
            FakeBotPlayer bot = fakePlayerManager.getBotPlayer();
            String gameContext = GameContextCollector.collect(sender, bot);
            PlayerMemory memory = memoryStore.getMemory(playerName);

            // Build prompt and call OpenAI
            List<OpenAIClient.ChatMessage> prompt = PromptBuilder.build(
                    personality, gameContext, memory, playerName, playerMessage);

            openAIClient.chatAsync(prompt, personality.maxResponseLength)
                    .thenAccept(response -> server.execute(() -> {
                        String reply = response != null ? response : personality.fallbackMessage;
                        ChatResponder.sendBotMessage(server, botName, reply);

                        // Save to memory
                        RegistryEntry<Biome> biome = sender.getServerWorld().getBiome(sender.getBlockPos());
                        String biomeName = biome.getKey()
                                .map(k -> k.getValue().getPath()).orElse("unknown");
                        long time = sender.getServerWorld().getTimeOfDay() % 24000;
                        String timeOfDay = time < 12000 ? "day" : "night";
                        memoryStore.addConversation(playerName, playerMessage, reply,
                                biomeName, timeOfDay);
                        memorySummarizer.summarizeIfNeeded(playerName);
                    }));
        });
    }
}
