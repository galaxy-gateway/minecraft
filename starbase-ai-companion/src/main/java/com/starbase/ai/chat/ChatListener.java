package com.starbase.ai.chat;

import com.starbase.ai.StarbaseAIMod;
import com.starbase.ai.ai.GameContextCollector;
import com.starbase.ai.ai.OpenAIClient;
import com.starbase.ai.ai.PromptBuilder;
import com.starbase.ai.behavior.Behavior;
import com.starbase.ai.behavior.BotBehaviorManager;
import com.starbase.ai.behavior.EnvironmentScanner;
import com.starbase.ai.behavior.task.FollowPlayerBehavior;
import com.starbase.ai.behavior.task.GatherResourceBehavior;
import com.starbase.ai.bot.FakeBotPlayer;
import com.starbase.ai.bot.FakePlayerManager;
import com.starbase.ai.config.PersonalityConfig;
import com.starbase.ai.memory.MemoryStore;
import com.starbase.ai.memory.MemorySummarizer;
import com.starbase.ai.memory.PlayerMemory;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.block.Block;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.biome.Biome;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

            // Handle simple commands before AI processing
            if (handleCommand(playerMessage, sender, server, botName)) {
                return;
            }

            // Rate limit check
            if (!rateLimiter.isAllowed(playerName)) {
                server.execute(() -> ChatResponder.sendBotMessage(
                        server, botName, "Give me a moment..."));
                return;
            }

            // Gather context
            FakeBotPlayer bot = fakePlayerManager.getBotPlayer();
            EnvironmentScanner.Snapshot env = bot != null ? bot.getEnvironmentSnapshot() : null;
            String activity = getActivityDescription(bot);
            String gameContext = GameContextCollector.collect(sender, bot, env, activity);
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

    // Matches: "collect 10 sand", "mine 5 stone", "gather oak log", "get some dirt", "grab 10 sand"
    private static final Pattern GATHER_PATTERN = Pattern.compile(
            "(?:collect|gather|mine|get|grab|find|dig)\\s+(?:(\\d+)\\s+)?(.+)",
            Pattern.CASE_INSENSITIVE);

    private static String stripNaturalLanguage(String message) {
        return message
                .replaceFirst("^(?:can you|could you|would you|please|pls|go)\\s+", "")
                .replaceFirst("\\s+(?:for me|for us|please|pls|thanks|thx|now)\\??$", "")
                .replaceFirst("[?.!]+$", "")
                .trim();
    }

    private boolean handleCommand(String message, ServerPlayerEntity sender,
                                  MinecraftServer server, String botName) {
        String lower = message.toLowerCase().trim();
        String stripped = stripNaturalLanguage(lower);

        // Follow command
        if (stripped.equals("follow me") || stripped.equals("come here")
                || stripped.equals("come") || stripped.equals("come to me")) {
            FollowPlayerBehavior followBehavior = fakePlayerManager.getFollowBehavior();
            if (followBehavior != null) {
                followBehavior.follow(sender);
                server.execute(() -> ChatResponder.sendBotMessage(
                        server, botName, "On my way, " + sender.getName().getString() + "!"));
            }
            return true;
        }

        // Stop command (stops following OR gathering)
        if (stripped.equals("stop") || stripped.equals("stay") || stripped.equals("stay here")
                || stripped.equals("stop following") || stripped.equals("stop gathering")
                || stripped.equals("stop mining") || stripped.equals("stop collecting")) {
            FollowPlayerBehavior followBehavior = fakePlayerManager.getFollowBehavior();
            if (followBehavior != null && followBehavior.isFollowing()) {
                followBehavior.stopFollowing();
            }
            GatherResourceBehavior gatherBehavior = fakePlayerManager.getGatherBehavior();
            if (gatherBehavior != null && gatherBehavior.isGathering()) {
                gatherBehavior.stopGathering();
            }
            server.execute(() -> ChatResponder.sendBotMessage(
                    server, botName, "Alright, I'll stop."));
            return true;
        }

        // Gather/collect/mine command
        Matcher gatherMatch = GATHER_PATTERN.matcher(stripped);
        if (gatherMatch.matches()) {
            return handleGatherCommand(gatherMatch, sender, server, botName);
        }

        return false;
    }

    private String getActivityDescription(FakeBotPlayer bot) {
        if (bot == null) return "Offline";
        BotBehaviorManager behaviorManager = bot.getBehaviorManager();
        if (behaviorManager == null) return "Idle";
        Behavior active = behaviorManager.getActiveBehavior();
        if (active == null) return "Idle";

        if (active instanceof FollowPlayerBehavior follow) {
            ServerPlayerEntity target = follow.getTarget();
            String name = target != null ? target.getName().getString() : "player";
            return "Following " + name;
        }
        if (active instanceof GatherResourceBehavior gather) {
            if (gather.isGathering()) {
                return "Gathering resources";
            }
        }
        return active.name();
    }

    private boolean handleGatherCommand(Matcher match, ServerPlayerEntity sender,
                                        MinecraftServer server, String botName) {
        String countStr = match.group(1);
        String blockName = match.group(2).trim();

        // Strip trailing "s" for plural (e.g. "sands" -> "sand")
        if (blockName.endsWith("s") && !blockName.endsWith("ss")
                && !blockName.endsWith("grass") && !blockName.endsWith("glass")) {
            blockName = blockName.substring(0, blockName.length() - 1);
        }

        // Strip "some" / "a few" prefixes
        blockName = blockName.replaceFirst("^(?:some|a few|a bunch of)\\s+", "");

        int count = countStr != null ? Integer.parseInt(countStr) : 10;
        count = Math.max(1, Math.min(64, count)); // Clamp 1-64

        Block block = GatherResourceBehavior.resolveBlockName(blockName);
        if (block == null) {
            String finalBlockName = blockName;
            server.execute(() -> ChatResponder.sendBotMessage(
                    server, botName, "I don't know what '" + finalBlockName + "' is."));
            return true;
        }

        GatherResourceBehavior gatherBehavior = fakePlayerManager.getGatherBehavior();
        if (gatherBehavior == null) {
            server.execute(() -> ChatResponder.sendBotMessage(
                    server, botName, "I can't do that right now."));
            return true;
        }

        String playerName = sender.getName().getString();
        gatherBehavior.gather(block, count, playerName);

        int finalCount = count;
        String displayName = blockName.replace("_", " ");
        server.execute(() -> ChatResponder.sendBotMessage(
                server, botName, "Sure! I'll collect " + finalCount + " " + displayName + "."));
        return true;
    }
}
