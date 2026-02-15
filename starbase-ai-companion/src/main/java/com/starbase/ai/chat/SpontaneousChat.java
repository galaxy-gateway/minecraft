package com.starbase.ai.chat;

import com.starbase.ai.StarbaseAIMod;
import com.starbase.ai.ai.OpenAIClient;
import com.starbase.ai.behavior.EnvironmentScanner;
import com.starbase.ai.bot.FakeBotPlayer;
import com.starbase.ai.config.PersonalityConfig;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SpontaneousChat {

    private static final String SILENCE = "SILENCE";

    private final OpenAIClient openAIClient;
    private final PersonalityConfig personality;
    private final Random random = new Random();

    private int ticksUntilNextChat;
    private boolean waitingForResponse;

    public SpontaneousChat(OpenAIClient openAIClient, PersonalityConfig personality) {
        this.openAIClient = openAIClient;
        this.personality = personality;
        resetTimer();
    }

    public void maybeTick(FakeBotPlayer bot, EnvironmentScanner.Snapshot environment, MinecraftServer server) {
        if (!personality.spontaneousChatEnabled) return;
        if (waitingForResponse) return;
        if (environment == null) return;

        // Only chat if a player is within 32 blocks
        if (environment.nearbyPlayers().isEmpty()) {
            return;
        }
        boolean playerNearby = environment.nearbyPlayers().stream()
                .anyMatch(p -> p.distance() <= 32.0);
        if (!playerNearby) return;

        ticksUntilNextChat--;
        if (ticksUntilNextChat > 0) return;

        // Time to maybe say something
        waitingForResponse = true;
        String context = buildContext(bot, environment);

        List<OpenAIClient.ChatMessage> messages = new ArrayList<>();
        messages.add(new OpenAIClient.ChatMessage("system",
                personality.systemPrompt + "\n\n"
                        + "You are in a Minecraft world. Based on the context below, make a brief, natural observation "
                        + "or comment (under 100 characters). If there is nothing interesting to say, respond with exactly: SILENCE\n\n"
                        + "Context:\n" + context));
        messages.add(new OpenAIClient.ChatMessage("user",
                "What do you want to say right now? Remember: respond with SILENCE if nothing comes to mind."));

        String botName = StarbaseAIMod.CONFIG.bot().botName;

        openAIClient.chatAsync(messages, 100)
                .thenAccept(response -> server.execute(() -> {
                    waitingForResponse = false;
                    resetTimer();

                    if (response == null || response.trim().equalsIgnoreCase(SILENCE)
                            || response.trim().isEmpty()) {
                        return;
                    }

                    // Clean up the response (remove quotes if AI wrapped it)
                    String cleaned = response.trim();
                    if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
                        cleaned = cleaned.substring(1, cleaned.length() - 1);
                    }

                    ChatResponder.sendBotMessage(server, botName, cleaned);
                    StarbaseAIMod.LOGGER.debug("Spontaneous chat: {}", cleaned);
                }));
    }

    private String buildContext(FakeBotPlayer bot, EnvironmentScanner.Snapshot env) {
        StringBuilder ctx = new StringBuilder();

        // Time
        String timeStr = env.isNight() ? "Night" : "Day";
        ctx.append("Time: ").append(timeStr).append(" (").append(env.timeOfDay()).append(" ticks)\n");

        // Location
        ctx.append(String.format("Bot at (%d, %d, %d)\n",
                bot.getBlockX(), bot.getBlockY(), bot.getBlockZ()));

        // Health
        ctx.append(String.format("Health: %.0f/20\n", bot.getHealth()));

        // Nearby players
        if (!env.nearbyPlayers().isEmpty()) {
            ctx.append("Nearby players: ");
            List<String> players = env.nearbyPlayers().stream()
                    .map(p -> String.format("%s (%.0f blocks)", p.name(), p.distance()))
                    .toList();
            ctx.append(String.join(", ", players)).append("\n");
        }

        // Hostiles
        if (!env.hostileMobs().isEmpty()) {
            ctx.append("Hostile mobs nearby: ");
            List<String> mobs = env.hostileMobs().stream()
                    .map(m -> String.format("%s (%.0f blocks)", m.type(), m.distance()))
                    .toList();
            ctx.append(String.join(", ", mobs)).append("\n");
        }

        // Passive mobs
        if (!env.passiveMobs().isEmpty()) {
            ctx.append("Animals nearby: ");
            List<String> mobs = env.passiveMobs().stream()
                    .map(m -> String.format("%s (%.0f blocks)", m.type(), m.distance()))
                    .toList();
            ctx.append(String.join(", ", mobs)).append("\n");
        }

        // Environment
        if (env.hasOverheadCover()) ctx.append("Inside/sheltered\n");
        else ctx.append("Outside/open sky\n");
        ctx.append("Light level: ").append(env.lightLevel()).append("\n");

        // Notable blocks
        if (!env.notableBlocks().isEmpty()) {
            ctx.append("Nearby blocks: ");
            List<String> blocks = env.notableBlocks().stream()
                    .limit(5)
                    .map(EnvironmentScanner.BlockInfo::type)
                    .distinct()
                    .toList();
            ctx.append(String.join(", ", blocks)).append("\n");
        }

        return ctx.toString();
    }

    private void resetTimer() {
        int minTicks = personality.spontaneousChatMinSeconds * 20;
        int maxTicks = personality.spontaneousChatMaxSeconds * 20;
        ticksUntilNextChat = minTicks + random.nextInt(Math.max(1, maxTicks - minTicks));
    }
}
