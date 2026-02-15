# AI Companion Mod Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a Fabric 1.21.1 server-side mod that spawns an AI-powered fake player which responds to chat via OpenAI with persistent memory.

**Architecture:** Fake player entity (Carpet-style) + Fabric chat event listener + async OpenAI HTTP calls + JSON-based config and memory. All OpenAI calls happen off the server thread via CompletableFuture; responses are scheduled back to the main thread.

**Tech Stack:** Java 21, Fabric Loom 1.8, Fabric API 0.116.8+1.21.1, Yarn 1.21.1+build.3, java.net.http.HttpClient, com.google.gson (bundled with MC)

**Reference design:** `docs/plans/2026-02-14-ai-companion-design.md`

---

### Task 1: Project Scaffolding

**Files:**
- Create: `starbase-ai-companion/settings.gradle`
- Create: `starbase-ai-companion/build.gradle`
- Create: `starbase-ai-companion/gradle.properties`
- Create: `starbase-ai-companion/src/main/resources/fabric.mod.json`
- Create: `starbase-ai-companion/src/main/resources/starbase-ai.accesswidener`
- Create: `starbase-ai-companion/src/main/java/com/starbase/ai/StarbaseAIMod.java`

**Step 1: Create settings.gradle**

```groovy
// starbase-ai-companion/settings.gradle
pluginManagement {
    repositories {
        maven {
            name = 'Fabric'
            url = 'https://maven.fabricmc.net/'
        }
        gradlePluginPortal()
    }
}
```

**Step 2: Create gradle.properties**

```properties
# starbase-ai-companion/gradle.properties
org.gradle.jvmargs=-Xmx1G
org.gradle.parallel=true

# Fabric Properties
minecraft_version=1.21.1
yarn_mappings=1.21.1+build.3
loader_version=0.16.9
loom_version=1.8

# Mod Properties
mod_version=0.1.0
maven_group=com.starbase
archives_base_name=starbase-ai-companion

# Dependencies
fabric_version=0.116.8+1.21.1
```

**Step 3: Create build.gradle**

```groovy
// starbase-ai-companion/build.gradle
plugins {
    id 'fabric-loom' version "${loom_version}"
}

version = project.mod_version
group = project.maven_group

base {
    archivesName = project.archives_base_name
}

loom {
    accessWidenerPath = file("src/main/resources/starbase-ai.accesswidener")
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
}

processResources {
    inputs.property "version", project.version
    filesMatching("fabric.mod.json") {
        expand "version": project.version
    }
}

tasks.withType(JavaCompile).configureEach {
    it.options.release = 21
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
```

**Step 4: Create fabric.mod.json**

```json
{
    "schemaVersion": 1,
    "id": "starbase-ai",
    "version": "${version}",
    "name": "Starbase AI Companion",
    "description": "AI-powered bot player that chats with players via OpenAI",
    "authors": ["Starbase"],
    "license": "MIT",
    "environment": "server",
    "entrypoints": {
        "server": ["com.starbase.ai.StarbaseAIMod"]
    },
    "accessWidener": "starbase-ai.accesswidener",
    "depends": {
        "fabricloader": ">=0.16.0",
        "minecraft": "~1.21.1",
        "java": ">=21",
        "fabric-api": "*"
    }
}
```

**Step 5: Create access widener**

```
// starbase-ai-companion/src/main/resources/starbase-ai.accesswidener
accessWidener v2 named
accessible field net/minecraft/network/ClientConnection channel Lio/netty/channel/Channel;
mutable field net/minecraft/network/ClientConnection channel Lio/netty/channel/Channel;
```

**Step 6: Create mod entry point stub**

```java
// starbase-ai-companion/src/main/java/com/starbase/ai/StarbaseAIMod.java
package com.starbase.ai;

import net.fabricmc.api.DedicatedServerModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StarbaseAIMod implements DedicatedServerModInitializer {
    public static final String MOD_ID = "starbase-ai";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeServer() {
        LOGGER.info("Starbase AI Companion loading...");
    }
}
```

**Step 7: Install Gradle wrapper and verify build**

```bash
cd starbase-ai-companion
gradle wrapper --gradle-version 8.10
./gradlew build
```

Expected: BUILD SUCCESSFUL, jar in `build/libs/starbase-ai-companion-0.1.0.jar`

**Step 8: Commit**

```
feat(ai-companion): scaffold Fabric mod project
```

---

### Task 2: Config System

**Files:**
- Create: `starbase-ai-companion/src/main/java/com/starbase/ai/config/ModConfig.java`
- Create: `starbase-ai-companion/src/main/java/com/starbase/ai/config/BotConfig.java`
- Create: `starbase-ai-companion/src/main/java/com/starbase/ai/config/PersonalityConfig.java`

**Step 1: Create BotConfig data class**

```java
package com.starbase.ai.config;

public class BotConfig {
    public String botName = "ARIA";
    public double spawnX = 0;
    public double spawnY = 64;
    public double spawnZ = 0;
    public boolean lookAtNearestPlayer = true;
}
```

**Step 2: Create PersonalityConfig data class**

```java
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
```

**Step 3: Create ModConfig loader**

```java
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

    public void load() {
        try {
            Files.createDirectories(CONFIG_DIR);
            botConfig = loadOrCreate("bot.json", BotConfig.class);
            personalityConfig = loadOrCreate("personality.json", PersonalityConfig.class);

            if (personalityConfig.openaiApiKey.isEmpty()) {
                StarbaseAIMod.LOGGER.warn("OpenAI API key not set in config/starbase-ai/personality.json!");
            }
        } catch (IOException e) {
            StarbaseAIMod.LOGGER.error("Failed to load config", e);
            botConfig = new BotConfig();
            personalityConfig = new PersonalityConfig();
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

    public BotConfig bot() { return botConfig; }
    public PersonalityConfig personality() { return personalityConfig; }
    public Path configDir() { return CONFIG_DIR; }
}
```

**Step 4: Wire config into mod entry point**

Update `StarbaseAIMod.java`:

```java
package com.starbase.ai;

import com.starbase.ai.config.ModConfig;
import net.fabricmc.api.DedicatedServerModInitializer;
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
        LOGGER.info("Bot name: {}", CONFIG.bot().botName);
    }
}
```

**Step 5: Build and verify**

```bash
cd starbase-ai-companion && ./gradlew build
```

Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```
feat(ai-companion): add JSON config system for bot and personality settings
```

---

### Task 3: OpenAI Client

**Files:**
- Create: `starbase-ai-companion/src/main/java/com/starbase/ai/ai/OpenAIClient.java`

**Step 1: Implement async OpenAI client**

```java
package com.starbase.ai.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.starbase.ai.StarbaseAIMod;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class OpenAIClient {
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private final String apiKey;
    private final String model;
    private final double temperature;
    private final int timeoutSeconds;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public OpenAIClient(String apiKey, String model, double temperature, int timeoutSeconds) {
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
        this.timeoutSeconds = timeoutSeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    public record ChatMessage(String role, String content) {}

    public CompletableFuture<String> chatAsync(List<ChatMessage> messages, int maxTokens) {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("max_tokens", maxTokens);
        body.addProperty("temperature", temperature);

        JsonArray messagesArray = new JsonArray();
        for (ChatMessage msg : messages) {
            JsonObject msgObj = new JsonObject();
            msgObj.addProperty("role", msg.role());
            msgObj.addProperty("content", msg.content());
            messagesArray.add(msgObj);
        }
        body.add("messages", messagesArray);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        StarbaseAIMod.LOGGER.error("OpenAI API error {}: {}",
                                response.statusCode(), response.body());
                        return null;
                    }
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    JsonArray choices = json.getAsJsonArray("choices");
                    if (choices == null || choices.isEmpty()) return null;
                    return choices.get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString();
                })
                .exceptionally(e -> {
                    StarbaseAIMod.LOGGER.error("OpenAI request failed", e);
                    return null;
                });
    }
}
```

**Step 2: Build and verify**

```bash
cd starbase-ai-companion && ./gradlew build
```

**Step 3: Commit**

```
feat(ai-companion): add async OpenAI chat completions client
```

---

### Task 4: Memory Store

**Files:**
- Create: `starbase-ai-companion/src/main/java/com/starbase/ai/memory/PlayerMemory.java`
- Create: `starbase-ai-companion/src/main/java/com/starbase/ai/memory/MemoryStore.java`

**Step 1: Create PlayerMemory data class**

```java
package com.starbase.ai.memory;

import java.util.ArrayList;
import java.util.List;

public class PlayerMemory {
    public String playerName;
    public String firstSeen;
    public String summary = "";
    public List<ConversationEntry> conversations = new ArrayList<>();
    public int messagesSinceSummary = 0;

    public static class ConversationEntry {
        public String timestamp;
        public String playerMessage;
        public String botResponse;
        public String biome;
        public String timeOfDay;

        public ConversationEntry(String timestamp, String playerMessage, String botResponse,
                                  String biome, String timeOfDay) {
            this.timestamp = timestamp;
            this.playerMessage = playerMessage;
            this.botResponse = botResponse;
            this.biome = biome;
            this.timeOfDay = timeOfDay;
        }
    }
}
```

**Step 2: Create MemoryStore**

```java
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

    private void save(String playerName, PlayerMemory mem) {
        try {
            Path file = memoryDir.resolve(playerName.toLowerCase() + ".json");
            Files.writeString(file, GSON.toJson(mem));
        } catch (IOException e) {
            StarbaseAIMod.LOGGER.error("Failed to save memory for {}", playerName, e);
        }
    }
}
```

**Step 3: Build and verify**

```bash
cd starbase-ai-companion && ./gradlew build
```

**Step 4: Commit**

```
feat(ai-companion): add persistent per-player memory store
```

---

### Task 5: Fake Player Entity

**Files:**
- Create: `starbase-ai-companion/src/main/java/com/starbase/ai/bot/FakeBotPlayer.java`
- Create: `starbase-ai-companion/src/main/java/com/starbase/ai/bot/FakeClientConnection.java`
- Create: `starbase-ai-companion/src/main/java/com/starbase/ai/bot/FakePlayerManager.java`

**Step 1: Create FakeClientConnection**

```java
package com.starbase.ai.bot;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;

public class FakeClientConnection extends ClientConnection {

    public FakeClientConnection() {
        super(NetworkSide.SERVERBOUND);
        // EmbeddedChannel makes isOpen() return true
        this.channel = new EmbeddedChannel();
    }

    @Override
    public void handleDisconnection() {
        // No-op: no real client
    }
}
```

Note: The `this.channel = ...` line requires the access widener from Task 1.

**Step 2: Create FakeBotPlayer**

```java
package com.starbase.ai.bot;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Uuids;

public class FakeBotPlayer extends ServerPlayerEntity {

    public FakeBotPlayer(MinecraftServer server, ServerWorld world, GameProfile profile) {
        super(server, world, profile, server.createClientInformation(profile));
    }

    public static GameProfile createProfile(String botName) {
        return new GameProfile(Uuids.getOfflinePlayerUuid(botName), botName);
    }

    @Override
    public void tick() {
        // Reset position tracking every 10 ticks to avoid "moved too quickly" warnings
        if (this.getServer().getTicks() % 10 == 0) {
            this.networkHandler.syncWithPlayerPosition();
            this.getServerWorld().getChunkManager().updatePosition(this);
        }
        super.tick();
        lookAtNearestPlayer();
    }

    private void lookAtNearestPlayer() {
        ServerWorld world = this.getServerWorld();
        ServerPlayerEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player == this || player instanceof FakeBotPlayer) continue;
            double dist = this.squaredDistanceTo(player);
            if (dist < nearestDist && dist < 48.0 * 48.0) {
                nearestDist = dist;
                nearest = player;
            }
        }

        if (nearest == null) return;

        double dx = nearest.getX() - this.getX();
        double dy = nearest.getEyeY() - this.getEyeY();
        double dz = nearest.getZ() - this.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontalDist)));

        this.setYaw(lerpAngle(this.getYaw(), targetYaw, 0.3f));
        this.setPitch(lerpAngle(this.getPitch(), targetPitch, 0.3f));
        this.headYaw = this.getYaw();

        this.getServerWorld().getChunkManager().sendToOtherNearbyPlayers(this,
                new EntitySetHeadYawS2CPacket(this, (byte) (this.headYaw * 256.0f / 360.0f)));
    }

    private static float lerpAngle(float current, float target, float factor) {
        float diff = target - current;
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        return current + diff * factor;
    }
}
```

Note: The `server.createClientInformation(profile)` call may not exist. If it doesn't compile, replace with `net.minecraft.server.network.ServerPlayerEntity.ClientInformation.createDefault()` or the appropriate Yarn-mapped equivalent. This will need to be verified during build.

**Step 3: Create FakePlayerManager**

```java
package com.starbase.ai.bot;

import com.starbase.ai.StarbaseAIMod;
import com.starbase.ai.config.BotConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public class FakePlayerManager {
    private FakeBotPlayer botPlayer;

    public void spawn(MinecraftServer server) {
        BotConfig config = StarbaseAIMod.CONFIG.bot();
        ServerWorld world = server.getOverworld();

        var profile = FakeBotPlayer.createProfile(config.botName);
        botPlayer = new FakeBotPlayer(server, world, profile);

        FakeClientConnection fakeConnection = new FakeClientConnection();
        var clientData = ConnectedClientData.createDefault(profile, false);

        server.getPlayerManager().onPlayerConnect(fakeConnection, botPlayer, clientData);

        botPlayer.teleport(world, config.spawnX, config.spawnY, config.spawnZ,
                java.util.Set.of(), 0f, 0f, true);
        botPlayer.setHealth(20.0f);
        botPlayer.unsetRemoved();
        botPlayer.getDataTracker().set(PlayerEntity.PLAYER_MODEL_PARTS, (byte) 0x7f);

        StarbaseAIMod.LOGGER.info("Spawned bot player: {} at ({}, {}, {})",
                config.botName, config.spawnX, config.spawnY, config.spawnZ);
    }

    public void despawn(MinecraftServer server) {
        if (botPlayer != null) {
            botPlayer.networkHandler.disconnect(Text.literal("Server stopping"));
            botPlayer = null;
            StarbaseAIMod.LOGGER.info("Despawned bot player");
        }
    }

    public FakeBotPlayer getBotPlayer() {
        return botPlayer;
    }
}
```

**Step 4: Build and verify**

```bash
cd starbase-ai-companion && ./gradlew build
```

This task is the most likely to have compilation issues due to Yarn mapping differences. Fix any method signature mismatches using the Yarn javadocs for 1.21.1.

**Step 5: Commit**

```
feat(ai-companion): add fake player entity with head tracking
```

---

### Task 6: Game Context Collector

**Files:**
- Create: `starbase-ai-companion/src/main/java/com/starbase/ai/ai/GameContextCollector.java`

**Step 1: Implement context collector**

```java
package com.starbase.ai.ai;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;

public class GameContextCollector {

    public static String collect(ServerPlayerEntity player, ServerPlayerEntity bot) {
        ServerWorld world = player.getServerWorld();
        StringBuilder ctx = new StringBuilder();

        // Time
        long time = world.getTimeOfDay() % 24000;
        String timeStr = time < 12000 ? "Day" : "Night";
        ctx.append("Time: ").append(timeStr).append(" (").append(time).append(" ticks)\n");

        // Weather
        String weather = world.isThundering() ? "Thunder" : world.isRaining() ? "Rain" : "Clear";
        ctx.append("Weather: ").append(weather).append("\n");

        // Player info
        RegistryEntry<Biome> biome = world.getBiome(player.getBlockPos());
        String biomeName = biome.getKey().map(k -> k.getValue().getPath()).orElse("unknown");
        ctx.append(String.format("Player \"%s\" at (%.0f, %.0f, %.0f) in %s biome\n",
                player.getName().getString(),
                player.getX(), player.getY(), player.getZ(),
                biomeName));

        // Nearby players
        List<String> nearby = new ArrayList<>();
        for (ServerPlayerEntity other : world.getPlayers()) {
            if (other == player || other == bot) continue;
            double dist = player.distanceTo(other);
            if (dist < 100) {
                nearby.add(String.format("%s (%.0f blocks away)", other.getName().getString(), dist));
            }
        }
        if (!nearby.isEmpty()) {
            ctx.append("Nearby players: ").append(String.join(", ", nearby)).append("\n");
        }

        // Bot info
        if (bot != null) {
            ctx.append(String.format("Bot at (%.0f, %.0f, %.0f), health: %.0f/20\n",
                    bot.getX(), bot.getY(), bot.getZ(), bot.getHealth()));
        }

        return ctx.toString();
    }
}
```

**Step 2: Build and verify**

```bash
cd starbase-ai-companion && ./gradlew build
```

**Step 3: Commit**

```
feat(ai-companion): add game context collector for OpenAI prompts
```

---

### Task 7: Prompt Builder

**Files:**
- Create: `starbase-ai-companion/src/main/java/com/starbase/ai/ai/PromptBuilder.java`

**Step 1: Implement prompt builder**

```java
package com.starbase.ai.ai;

import com.starbase.ai.config.PersonalityConfig;
import com.starbase.ai.memory.PlayerMemory;

import java.util.ArrayList;
import java.util.List;

public class PromptBuilder {

    private static final String MINECRAFT_KNOWLEDGE = """
            You have knowledge of Minecraft game mechanics:
            - Crafting recipes, smelting, brewing
            - Mob behavior (hostile mobs spawn at night or in dark areas)
            - Biome characteristics and resources
            - Redstone basics
            - Enchanting and anvil usage
            - Nether and End dimension info
            Keep responses concise for Minecraft chat (under 200 characters preferred).
            Split long answers across multiple short messages if needed.
            """;

    public static List<OpenAIClient.ChatMessage> build(
            PersonalityConfig personality,
            String gameContext,
            PlayerMemory memory,
            String playerName,
            String playerMessage) {

        List<OpenAIClient.ChatMessage> messages = new ArrayList<>();

        // System prompt: personality + minecraft knowledge + context
        String system = personality.systemPrompt + "\n\n" + MINECRAFT_KNOWLEDGE + "\n\nCurrent game state:\n" + gameContext;

        // Add player memory summary if available
        if (memory != null && !memory.summary.isEmpty()) {
            system += "\n\nWhat you remember about " + playerName + ": " + memory.summary;
        }

        messages.add(new OpenAIClient.ChatMessage("system", system));

        // Add recent conversation history
        if (memory != null) {
            List<PlayerMemory.ConversationEntry> convos = memory.conversations;
            int start = Math.max(0, convos.size() - 10);
            for (int i = start; i < convos.size(); i++) {
                PlayerMemory.ConversationEntry entry = convos.get(i);
                messages.add(new OpenAIClient.ChatMessage("user", entry.playerMessage));
                messages.add(new OpenAIClient.ChatMessage("assistant", entry.botResponse));
            }
        }

        // Current message
        messages.add(new OpenAIClient.ChatMessage("user", playerMessage));

        return messages;
    }
}
```

**Step 2: Build and verify**

```bash
cd starbase-ai-companion && ./gradlew build
```

**Step 3: Commit**

```
feat(ai-companion): add prompt builder with memory and game context
```

---

### Task 8: Chat Listener, Responder, and Rate Limiter

**Files:**
- Create: `starbase-ai-companion/src/main/java/com/starbase/ai/chat/RateLimiter.java`
- Create: `starbase-ai-companion/src/main/java/com/starbase/ai/chat/ChatResponder.java`
- Create: `starbase-ai-companion/src/main/java/com/starbase/ai/chat/ChatListener.java`

**Step 1: Create RateLimiter**

```java
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
```

**Step 2: Create ChatResponder**

```java
package com.starbase.ai.chat;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

public class ChatResponder {

    public static void sendBotMessage(MinecraftServer server, String botName, String message) {
        // Split long messages at 200 chars to stay readable in MC chat
        int maxLen = 200;
        if (message.length() <= maxLen) {
            broadcast(server, botName, message);
            return;
        }

        // Split on sentence boundaries or word boundaries
        int start = 0;
        while (start < message.length()) {
            int end = Math.min(start + maxLen, message.length());
            if (end < message.length()) {
                // Try to break at a sentence boundary
                int sentenceBreak = message.lastIndexOf(". ", end);
                if (sentenceBreak > start) {
                    end = sentenceBreak + 1;
                } else {
                    // Break at word boundary
                    int wordBreak = message.lastIndexOf(' ', end);
                    if (wordBreak > start) {
                        end = wordBreak;
                    }
                }
            }
            broadcast(server, botName, message.substring(start, end).trim());
            start = end;
        }
    }

    private static void broadcast(MinecraftServer server, String botName, String text) {
        server.getPlayerManager().broadcast(
                Text.literal("<" + botName + "> " + text), false);
    }
}
```

**Step 3: Create ChatListener**

```java
package com.starbase.ai.chat;

import com.starbase.ai.StarbaseAIMod;
import com.starbase.ai.ai.GameContextCollector;
import com.starbase.ai.ai.OpenAIClient;
import com.starbase.ai.ai.PromptBuilder;
import com.starbase.ai.bot.FakeBotPlayer;
import com.starbase.ai.bot.FakePlayerManager;
import com.starbase.ai.config.PersonalityConfig;
import com.starbase.ai.memory.MemoryStore;
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
    private final RateLimiter rateLimiter;

    public ChatListener(FakePlayerManager fakePlayerManager, OpenAIClient openAIClient,
                         MemoryStore memoryStore, RateLimiter rateLimiter) {
        this.fakePlayerManager = fakePlayerManager;
        this.openAIClient = openAIClient;
        this.memoryStore = memoryStore;
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
                    }));
        });
    }
}
```

**Step 4: Build and verify**

```bash
cd starbase-ai-companion && ./gradlew build
```

**Step 5: Commit**

```
feat(ai-companion): add chat listener, responder, and rate limiter
```

---

### Task 9: Wire Everything Together

**Files:**
- Modify: `starbase-ai-companion/src/main/java/com/starbase/ai/StarbaseAIMod.java`

**Step 1: Update mod entry point to initialize all systems**

```java
package com.starbase.ai;

import com.starbase.ai.ai.OpenAIClient;
import com.starbase.ai.bot.FakePlayerManager;
import com.starbase.ai.chat.ChatListener;
import com.starbase.ai.chat.RateLimiter;
import com.starbase.ai.config.ModConfig;
import com.starbase.ai.config.PersonalityConfig;
import com.starbase.ai.memory.MemoryStore;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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

        // Register chat listener
        ChatListener chatListener = new ChatListener(
                fakePlayerManager, openAIClient, memoryStore, rateLimiter);
        chatListener.register();

        // Spawn bot when server starts
        ServerLifecycleEvents.SERVER_STARTED.register(fakePlayerManager::spawn);

        // Despawn bot when server stops
        ServerLifecycleEvents.SERVER_STOPPING.register(fakePlayerManager::despawn);

        LOGGER.info("Starbase AI Companion loaded! Bot name: {}", CONFIG.bot().botName);
    }
}
```

**Step 2: Build and verify**

```bash
cd starbase-ai-companion && ./gradlew build
```

Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```
feat(ai-companion): wire all systems together in mod entry point
```

---

### Task 10: Memory Summarizer

**Files:**
- Create: `starbase-ai-companion/src/main/java/com/starbase/ai/memory/MemorySummarizer.java`
- Modify: `starbase-ai-companion/src/main/java/com/starbase/ai/chat/ChatListener.java` (trigger summarization)

**Step 1: Create MemorySummarizer**

```java
package com.starbase.ai.memory;

import com.starbase.ai.StarbaseAIMod;
import com.starbase.ai.ai.OpenAIClient;

import java.util.ArrayList;
import java.util.List;

public class MemorySummarizer {
    private static final int SUMMARIZE_EVERY = 20;
    private final OpenAIClient openAIClient;
    private final MemoryStore memoryStore;

    public MemorySummarizer(OpenAIClient openAIClient, MemoryStore memoryStore) {
        this.openAIClient = openAIClient;
        this.memoryStore = memoryStore;
    }

    public void summarizeIfNeeded(String playerName) {
        PlayerMemory memory = memoryStore.getMemory(playerName);
        if (memory.messagesSinceSummary < SUMMARIZE_EVERY) return;

        // Build a summary prompt from all conversations
        StringBuilder convoText = new StringBuilder();
        for (PlayerMemory.ConversationEntry entry : memory.conversations) {
            convoText.append("Player: ").append(entry.playerMessage).append("\n");
            convoText.append("Bot: ").append(entry.botResponse).append("\n\n");
        }

        List<OpenAIClient.ChatMessage> messages = new ArrayList<>();
        messages.add(new OpenAIClient.ChatMessage("system",
                "Summarize the following conversations with " + playerName +
                " into a single paragraph. Focus on: what the player likes to do, " +
                "what they asked about, their play style, and any notable events. " +
                "Keep it under 200 words."));
        messages.add(new OpenAIClient.ChatMessage("user", convoText.toString()));

        openAIClient.chatAsync(messages, 300)
                .thenAccept(summary -> {
                    if (summary != null) {
                        memory.summary = summary;
                        memory.messagesSinceSummary = 0;
                        // Re-save memory
                        memoryStore.addConversation(playerName, "", "", "", "");
                        // Remove the empty entry we just added
                        if (!memory.conversations.isEmpty()) {
                            memory.conversations.remove(memory.conversations.size() - 1);
                        }
                        StarbaseAIMod.LOGGER.info("Updated memory summary for {}", playerName);
                    }
                });
    }
}
```

Note: The save-via-addConversation approach is a bit hacky. A cleaner fix is to expose a `save()` method on MemoryStore. Refactor during this step:

Add to MemoryStore:
```java
public void save(String playerName) {
    PlayerMemory mem = cache.get(playerName.toLowerCase());
    if (mem != null) {
        save(playerName, mem);
    }
}
```

Then MemorySummarizer becomes:
```java
memoryStore.save(playerName);
```

**Step 2: Trigger summarization from ChatListener**

In `ChatListener.java`, after saving the conversation in the `thenAccept` callback, add:

```java
// After memoryStore.addConversation(...)
memorySummarizer.summarizeIfNeeded(playerName);
```

Pass `MemorySummarizer` into the ChatListener constructor.

**Step 3: Build and verify**

```bash
cd starbase-ai-companion && ./gradlew build
```

**Step 4: Commit**

```
feat(ai-companion): add automatic memory summarization
```

---

### Task 11: Integration Test on Server

**Step 1: Build the final jar**

```bash
cd starbase-ai-companion && ./gradlew build
```

**Step 2: Deploy to server**

```bash
cp starbase-ai-companion/build/libs/starbase-ai-companion-0.1.0.jar mods/
```

**Step 3: Create config with test API key**

Create `config/starbase-ai/personality.json` with a real OpenAI API key.

**Step 4: Start the server**

```bash
make start-fabric
```

**Step 5: Verify in-game**

1. Check server logs for "Starbase AI Companion loaded!"
2. Connect to server, verify bot appears in tab list
3. Verify bot entity is visible at spawn
4. Type `@ARIA hello` in chat
5. Verify bot responds with an AI-generated message
6. Type `@ARIA what biome am I in?` to test context
7. Check `config/starbase-ai/memory/` for player memory file
8. Rapid-fire messages to test rate limiting

**Step 6: Commit**

```
feat(ai-companion): complete MVP with integration testing
```

---

## Troubleshooting Notes

**Compilation issues with Yarn mappings:** The fake player code is adapted from Carpet (Mojang mappings). If methods don't resolve:
- Check `ServerPlayerEntity` constructor signature in Yarn 1.21.1+build.3 javadocs
- Check `ConnectedClientData.createDefault()` parameter types
- Check `PlayerManager.onPlayerConnect()` parameter types
- The access widener field name for `ClientConnection.channel` may use an intermediary name

**Key mapping reference (Mojang -> Yarn):**
| Mojang | Yarn |
|--------|------|
| `ServerPlayer` | `ServerPlayerEntity` |
| `ServerLevel` | `ServerWorld` |
| `Connection` | `ClientConnection` |
| `ServerGamePacketListenerImpl` | `ServerPlayNetworkHandler` |
| `CommonListenerCookie` | `ConnectedClientData` |
| `placeNewPlayer()` | `onPlayerConnect()` |

**Bot not appearing:** Check that `online-mode=true` is not rejecting the offline UUID. The offline UUID generation via `Uuids.getOfflinePlayerUuid()` should work, but if not, try a hardcoded UUID.

**OpenAI not responding:** Check API key, check server can reach `api.openai.com` (firewall/DNS), check logs for HTTP error codes.
