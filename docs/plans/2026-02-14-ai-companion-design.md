# Starbase AI Companion - Design Document

A server-side Fabric 1.21.1 mod that spawns an AI-powered bot player which interacts with players through in-game chat using OpenAI.

## Decisions

| Decision | Choice |
|----------|--------|
| Bot role | Companion/Helper - follows context, answers questions, assists players |
| Interaction | In-game chat only via `@BotName` prefix |
| Framework | Java Fabric Mod (server-side) |
| MVP scope | Chat-only (no movement or block actions) |
| Memory | Persistent across server restarts (JSON per player) |
| Personality | Configurable via file + built-in Minecraft knowledge |

## Architecture

```
+---------------------------------------------+
|           Minecraft Fabric Server            |
|                                              |
|  +----------+   +------------------------+  |
|  | Chat     |-->| AICompanionMod         |  |
|  | Listener |   |                        |  |
|  +----------+   |  - Parse @bot messages |  |
|                 |  - Gather game context  |  |
|  +----------+   |  - Build prompt        |  |
|  | Fake     |   |  - Send to OpenAI      |  |
|  | Player   |<--+  - Post response       |  |
|  | Entity   |   |  - Save to memory      |  |
|  +----------+   +----------+-------------+  |
|                            |                 |
|  +----------+   +----------+-----------+     |
|  | Config   |   | Memory Store         |     |
|  | (JSON)   |   | (JSON per player)    |     |
|  +----------+   +----------------------+     |
+---------------------------------------------+
                      |
                      | HTTPS (async, off-thread)
                      v
              +---------------+
              |  OpenAI API   |
              +---------------+
```

OpenAI calls run off the server thread via CompletableFuture. Responses are scheduled back onto the main thread to send chat messages safely.

## Fake Player Entity

- Create a ServerPlayerEntity with a fake GameProfile (offline UUID v3 + bot name)
- Appears in tab list with custom skin
- Physical presence at configured spawn location
- Head-tracks nearest player (looks at them) for awareness feel
- Spawns on server start, removed on server stop
- Offline UUID avoids conflicts with online-mode=true auth

### Bot Config (config/starbase-ai/bot.json)

```json
{
  "botName": "ARIA",
  "skinUrl": "https://mc-heads.net/avatar/ARIA",
  "spawnX": 0,
  "spawnY": 64,
  "spawnZ": 0,
  "lookAtNearestPlayer": true
}
```

## Chat System

### Flow

1. Player types: `@ARIA what biome is this?`
2. Fabric ServerMessageEvents.CHAT_MESSAGE fires
3. Mod checks if message starts with @BotName (case-insensitive)
4. Gathers game context
5. Builds OpenAI request: system prompt + context + memory + player message
6. Sends HTTP request on background thread via CompletableFuture.supplyAsync()
7. On response, schedules chat message back on server thread
8. Bot says response in chat as `<ARIA> response here`
9. Player message and bot response appended to memory file

### Game Context (injected into each prompt)

- Time of day (day/night, tick count)
- Weather (clear/rain/thunder)
- Player biome and coordinates
- Nearby players and distances
- Bot location and health
- Server TPS

### Rate Limiting

- Configurable cooldown per player (default: 3 seconds)
- Messages during cooldown get: `<ARIA> Give me a moment...`

### Error Handling

- OpenAI timeout: 10 seconds default
- On failure: configurable fallback message
- HTTP client: java.net.http.HttpClient (built-in since Java 17)

## Memory System

### Storage (config/starbase-ai/memory/)

```
config/starbase-ai/memory/
  +-- steve.json
  +-- alex.json
  +-- global.json       # shared world facts
```

### Player Memory Format

```json
{
  "playerName": "Steve",
  "firstSeen": "2026-02-14T10:30:00Z",
  "summary": "Steve is an experienced player who likes building redstone contraptions...",
  "conversations": [
    {
      "timestamp": "2026-02-14T10:30:00Z",
      "playerMessage": "what biome is this?",
      "botResponse": "You are in a plains biome!",
      "context": { "biome": "plains", "timeOfDay": "day" }
    }
  ]
}
```

### Memory Management

- Keep last 50 conversations per player in file
- Include most recent 10 as chat history in prompt
- AI-generated summary of older interactions (one paragraph)
- Summary regenerated every 20 new messages via background call
- Retains long-term impressions without excessive token usage

## Personality System

### Config (config/starbase-ai/personality.json)

```json
{
  "systemPrompt": "You are ARIA, the AI companion of the Starbase server. You are friendly, curious, and love exploring. You speak concisely (under 100 characters when possible, never more than 200) because Minecraft chat is small.",
  "openaiModel": "gpt-4o-mini",
  "openaiApiKey": "sk-...",
  "maxResponseLength": 200,
  "temperature": 0.8,
  "requestTimeoutSeconds": 10,
  "rateLimitCooldownSeconds": 3,
  "fallbackMessage": "Sorry, my thoughts are a bit fuzzy right now. Try again?"
}
```

- System prompt is admin-editable for custom personality
- Minecraft knowledge appendix auto-injected (crafting, mobs, biomes)
- Response length enforced in prompt AND by truncation
- Long responses split across multiple chat messages with short delay (Minecraft chat 256-char limit)

## Project Structure

```
starbase-ai-companion/
+-- build.gradle
+-- settings.gradle
+-- gradle.properties
+-- src/main/
    +-- java/com/starbase/ai/
    |   +-- StarbaseAIMod.java          # Entry point, registers events
    |   +-- bot/
    |   |   +-- FakePlayerManager.java  # Spawn/despawn/manage bot entity
    |   |   +-- BotSkinUtil.java        # Skin loading for fake profile
    |   +-- chat/
    |   |   +-- ChatListener.java       # Intercepts @bot messages
    |   |   +-- ChatResponder.java      # Sends bot replies in-game
    |   |   +-- RateLimiter.java        # Per-player cooldowns
    |   +-- ai/
    |   |   +-- OpenAIClient.java       # Async HTTP to OpenAI
    |   |   +-- PromptBuilder.java      # Assembles system + context + memory
    |   |   +-- GameContextCollector.java
    |   +-- memory/
    |   |   +-- MemoryStore.java        # Read/write player JSON files
    |   |   +-- MemorySummarizer.java   # Compresses old convos into summaries
    |   +-- config/
    |       +-- ModConfig.java          # Loads bot.json + personality.json
    +-- resources/
        +-- fabric.mod.json
```

### Dependencies

- fabric-api: server events (chat, lifecycle, tick)
- java.net.http: built-in HTTP client for OpenAI
- com.google.gson: bundled with Minecraft for JSON

### Build & Deploy

1. ./gradlew build -> jar in build/libs/
2. Copy jar to server mods/ folder
3. Place config files in config/starbase-ai/
4. Restart server

## Future Enhancements (Post-MVP)

- Movement: follow players, pathfind to locations
- Actions: mining, placing blocks, combat
- Multiple bot support
- Web admin panel integration
- CC:Tweaked peripheral for Lua-based bot control
