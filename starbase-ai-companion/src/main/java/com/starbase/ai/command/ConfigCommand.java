package com.starbase.ai.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.starbase.ai.StarbaseAIMod;
import com.starbase.ai.config.BehaviorConfig;
import com.starbase.ai.config.BotConfig;
import com.starbase.ai.config.ModConfig;
import com.starbase.ai.config.PersonalityConfig;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ConfigCommand {

    private record ConfigEntry(String type, Supplier<Object> getter, Consumer<Object> setter) {}

    private static final Map<String, Map<String, ConfigEntry>> CATEGORIES = new LinkedHashMap<>();
    private static final Map<String, String> CATEGORY_FILES = Map.of(
            "bot", "bot.json",
            "personality", "personality.json",
            "behavior", "behavior.json"
    );

    private static void buildEntries() {
        CATEGORIES.clear();

        BotConfig bot = StarbaseAIMod.CONFIG.bot();
        Map<String, ConfigEntry> botEntries = new LinkedHashMap<>();
        botEntries.put("botName", new ConfigEntry("string", () -> bot.botName, v -> bot.botName = (String) v));
        botEntries.put("spawnX", new ConfigEntry("double", () -> bot.spawnX, v -> bot.spawnX = (double) v));
        botEntries.put("spawnY", new ConfigEntry("double", () -> bot.spawnY, v -> bot.spawnY = (double) v));
        botEntries.put("spawnZ", new ConfigEntry("double", () -> bot.spawnZ, v -> bot.spawnZ = (double) v));
        botEntries.put("lookAtNearestPlayer", new ConfigEntry("boolean", () -> bot.lookAtNearestPlayer, v -> bot.lookAtNearestPlayer = (boolean) v));
        botEntries.put("respawnDelayTicks", new ConfigEntry("int", () -> bot.respawnDelayTicks, v -> bot.respawnDelayTicks = (int) v));
        botEntries.put("walkSpeed", new ConfigEntry("double", () -> bot.walkSpeed, v -> bot.walkSpeed = (double) v));
        botEntries.put("sprintSpeed", new ConfigEntry("double", () -> bot.sprintSpeed, v -> bot.sprintSpeed = (double) v));
        botEntries.put("followDistance", new ConfigEntry("double", () -> bot.followDistance, v -> bot.followDistance = (double) v));
        botEntries.put("pathRecalcTicks", new ConfigEntry("int", () -> bot.pathRecalcTicks, v -> bot.pathRecalcTicks = (int) v));
        botEntries.put("pathMaxRange", new ConfigEntry("int", () -> bot.pathMaxRange, v -> bot.pathMaxRange = (int) v));
        CATEGORIES.put("bot", botEntries);

        PersonalityConfig personality = StarbaseAIMod.CONFIG.personality();
        Map<String, ConfigEntry> personalityEntries = new LinkedHashMap<>();
        personalityEntries.put("openaiModel", new ConfigEntry("string", () -> personality.openaiModel, v -> personality.openaiModel = (String) v));
        personalityEntries.put("maxResponseLength", new ConfigEntry("int", () -> personality.maxResponseLength, v -> personality.maxResponseLength = (int) v));
        personalityEntries.put("temperature", new ConfigEntry("double", () -> personality.temperature, v -> personality.temperature = (double) v));
        personalityEntries.put("requestTimeoutSeconds", new ConfigEntry("int", () -> personality.requestTimeoutSeconds, v -> personality.requestTimeoutSeconds = (int) v));
        personalityEntries.put("rateLimitCooldownSeconds", new ConfigEntry("int", () -> personality.rateLimitCooldownSeconds, v -> personality.rateLimitCooldownSeconds = (int) v));
        personalityEntries.put("fallbackMessage", new ConfigEntry("string", () -> personality.fallbackMessage, v -> personality.fallbackMessage = (String) v));
        personalityEntries.put("spontaneousChatMinSeconds", new ConfigEntry("int", () -> personality.spontaneousChatMinSeconds, v -> personality.spontaneousChatMinSeconds = (int) v));
        personalityEntries.put("spontaneousChatMaxSeconds", new ConfigEntry("int", () -> personality.spontaneousChatMaxSeconds, v -> personality.spontaneousChatMaxSeconds = (int) v));
        personalityEntries.put("spontaneousChatEnabled", new ConfigEntry("boolean", () -> personality.spontaneousChatEnabled, v -> personality.spontaneousChatEnabled = (boolean) v));
        CATEGORIES.put("personality", personalityEntries);

        BehaviorConfig behavior = StarbaseAIMod.CONFIG.behavior();
        Map<String, ConfigEntry> behaviorEntries = new LinkedHashMap<>();
        behaviorEntries.put("enableIdleWander", new ConfigEntry("boolean", () -> behavior.enableIdleWander, v -> behavior.enableIdleWander = (boolean) v));
        behaviorEntries.put("enableIdleLookAround", new ConfigEntry("boolean", () -> behavior.enableIdleLookAround, v -> behavior.enableIdleLookAround = (boolean) v));
        behaviorEntries.put("enableApproachPlayer", new ConfigEntry("boolean", () -> behavior.enableApproachPlayer, v -> behavior.enableApproachPlayer = (boolean) v));
        behaviorEntries.put("enableFleeHostile", new ConfigEntry("boolean", () -> behavior.enableFleeHostile, v -> behavior.enableFleeHostile = (boolean) v));
        behaviorEntries.put("enableNightShelter", new ConfigEntry("boolean", () -> behavior.enableNightShelter, v -> behavior.enableNightShelter = (boolean) v));
        behaviorEntries.put("enableAvoidDanger", new ConfigEntry("boolean", () -> behavior.enableAvoidDanger, v -> behavior.enableAvoidDanger = (boolean) v));
        behaviorEntries.put("enableAvoidDrowning", new ConfigEntry("boolean", () -> behavior.enableAvoidDrowning, v -> behavior.enableAvoidDrowning = (boolean) v));
        CATEGORIES.put("behavior", behaviorEntries);
    }

    private static Object parseValue(String type, String raw) {
        return switch (type) {
            case "string" -> raw;
            case "int" -> Integer.parseInt(raw);
            case "double" -> Double.parseDouble(raw);
            case "boolean" -> {
                if (raw.equalsIgnoreCase("true")) yield true;
                if (raw.equalsIgnoreCase("false")) yield false;
                throw new IllegalArgumentException("Expected true or false");
            }
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }

    private static final SuggestionProvider<ServerCommandSource> SUGGEST_CATEGORIES = (context, builder) -> {
        CATEGORIES.keySet().stream()
                .filter(s -> s.startsWith(builder.getRemaining().toLowerCase()))
                .forEach(builder::suggest);
        return builder.buildFuture();
    };

    private static final SuggestionProvider<ServerCommandSource> SUGGEST_KEYS = (context, builder) -> {
        String category = StringArgumentType.getString(context, "category");
        Map<String, ConfigEntry> entries = CATEGORIES.get(category);
        if (entries != null) {
            entries.keySet().stream()
                    .filter(s -> s.toLowerCase().startsWith(builder.getRemaining().toLowerCase()))
                    .forEach(builder::suggest);
        }
        return builder.buildFuture();
    };

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            buildEntries();
            registerCommands(dispatcher);
        });
    }

    private static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("aria")
                .requires(source -> source.hasPermissionLevel(2))
                .then(literal("config")
                        // /aria config — list categories
                        .executes(ConfigCommand::listCategories)
                        // /aria config reload
                        .then(literal("reload")
                                .executes(ConfigCommand::reloadConfig))
                        // /aria config <category> ...
                        .then(argument("category", StringArgumentType.word())
                                .suggests(SUGGEST_CATEGORIES)
                                // /aria config <category> — show all settings
                                .executes(ConfigCommand::showCategory)
                                .then(argument("key", StringArgumentType.word())
                                        .suggests(SUGGEST_KEYS)
                                        // /aria config <category> <key> — show single
                                        .executes(ConfigCommand::showKey)
                                        .then(argument("value", StringArgumentType.greedyString())
                                                // /aria config <category> <key> <value> — set
                                                .executes(ConfigCommand::setValue))))));
    }

    private static int listCategories(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() ->
                Text.literal("=== ARIA Config ===").formatted(Formatting.GOLD), false);
        for (var entry : CATEGORIES.entrySet()) {
            ctx.getSource().sendFeedback(() ->
                    Text.literal("  " + entry.getKey()).formatted(Formatting.YELLOW)
                            .append(Text.literal(" (" + entry.getValue().size() + " settings)").formatted(Formatting.GRAY)),
                    false);
        }
        ctx.getSource().sendFeedback(() ->
                Text.literal("Use /aria config <category> to view settings").formatted(Formatting.GRAY), false);
        return 1;
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> ctx) {
        StarbaseAIMod.CONFIG.reload();
        buildEntries();
        ctx.getSource().sendFeedback(() ->
                Text.literal("Config reloaded from disk.").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int showCategory(CommandContext<ServerCommandSource> ctx) {
        String category = StringArgumentType.getString(ctx, "category");
        Map<String, ConfigEntry> entries = CATEGORIES.get(category);
        if (entries == null) {
            ctx.getSource().sendError(Text.literal("Unknown category: " + category).formatted(Formatting.RED));
            return 0;
        }

        ctx.getSource().sendFeedback(() ->
                Text.literal("=== " + category + " ===").formatted(Formatting.GOLD), false);
        for (var entry : entries.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue().getter().get();
            ctx.getSource().sendFeedback(() ->
                    Text.literal("  " + key + ": ").formatted(Formatting.YELLOW)
                            .append(Text.literal(String.valueOf(value)).formatted(Formatting.WHITE)),
                    false);
        }
        return 1;
    }

    private static int showKey(CommandContext<ServerCommandSource> ctx) {
        String category = StringArgumentType.getString(ctx, "category");
        String key = StringArgumentType.getString(ctx, "key");

        Map<String, ConfigEntry> entries = CATEGORIES.get(category);
        if (entries == null) {
            ctx.getSource().sendError(Text.literal("Unknown category: " + category).formatted(Formatting.RED));
            return 0;
        }

        ConfigEntry entry = entries.get(key);
        if (entry == null) {
            ctx.getSource().sendError(Text.literal("Unknown key: " + key).formatted(Formatting.RED));
            return 0;
        }

        Object value = entry.getter().get();
        ctx.getSource().sendFeedback(() ->
                Text.literal(category + "." + key).formatted(Formatting.YELLOW)
                        .append(Text.literal(" = ").formatted(Formatting.GRAY))
                        .append(Text.literal(String.valueOf(value)).formatted(Formatting.WHITE))
                        .append(Text.literal(" (" + entry.type() + ")").formatted(Formatting.DARK_GRAY)),
                false);
        return 1;
    }

    private static int setValue(CommandContext<ServerCommandSource> ctx) {
        String category = StringArgumentType.getString(ctx, "category");
        String key = StringArgumentType.getString(ctx, "key");
        String raw = StringArgumentType.getString(ctx, "value");

        Map<String, ConfigEntry> entries = CATEGORIES.get(category);
        if (entries == null) {
            ctx.getSource().sendError(Text.literal("Unknown category: " + category).formatted(Formatting.RED));
            return 0;
        }

        ConfigEntry entry = entries.get(key);
        if (entry == null) {
            ctx.getSource().sendError(Text.literal("Unknown key: " + key).formatted(Formatting.RED));
            return 0;
        }

        Object parsed;
        try {
            parsed = parseValue(entry.type(), raw);
        } catch (Exception e) {
            ctx.getSource().sendError(
                    Text.literal("Invalid value for " + entry.type() + ": " + raw).formatted(Formatting.RED));
            return 0;
        }

        entry.setter().accept(parsed);

        // Save to disk
        String filename = CATEGORY_FILES.get(category);
        ModConfig config = StarbaseAIMod.CONFIG;
        try {
            Object configObj = switch (category) {
                case "bot" -> config.bot();
                case "personality" -> config.personality();
                case "behavior" -> config.behavior();
                default -> throw new IllegalStateException("Unknown category: " + category);
            };
            config.saveConfig(filename, configObj);
        } catch (IOException e) {
            ctx.getSource().sendError(Text.literal("Failed to save config: " + e.getMessage()).formatted(Formatting.RED));
            StarbaseAIMod.LOGGER.error("Failed to save config file {}", filename, e);
            return 0;
        }

        ctx.getSource().sendFeedback(() ->
                Text.literal("Set ").formatted(Formatting.GREEN)
                        .append(Text.literal(category + "." + key).formatted(Formatting.YELLOW))
                        .append(Text.literal(" = ").formatted(Formatting.GREEN))
                        .append(Text.literal(String.valueOf(parsed)).formatted(Formatting.WHITE)),
                true);
        return 1;
    }
}
