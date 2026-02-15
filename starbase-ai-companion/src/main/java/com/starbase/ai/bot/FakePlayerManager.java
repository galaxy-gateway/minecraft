package com.starbase.ai.bot;

import com.starbase.ai.StarbaseAIMod;
import com.starbase.ai.ai.OpenAIClient;
import com.starbase.ai.behavior.BotBehaviorManager;
import com.starbase.ai.behavior.EnvironmentScanner;
import com.starbase.ai.behavior.idle.ApproachPlayerBehavior;
import com.starbase.ai.behavior.idle.IdleLookAroundBehavior;
import com.starbase.ai.behavior.idle.IdleWanderBehavior;
import com.starbase.ai.behavior.survival.AvoidDangerBehavior;
import com.starbase.ai.behavior.survival.AvoidDrowningBehavior;
import com.starbase.ai.behavior.survival.FleeHostileBehavior;
import com.starbase.ai.behavior.survival.NightShelterBehavior;
import com.starbase.ai.behavior.task.FollowPlayerBehavior;
import com.starbase.ai.behavior.task.GatherResourceBehavior;
import com.starbase.ai.chat.SpontaneousChat;
import com.starbase.ai.config.BehaviorConfig;
import com.starbase.ai.config.BotConfig;
import com.starbase.ai.movement.BotMovementController;
import com.starbase.ai.movement.BotPathfinder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.Set;

public class FakePlayerManager {

    private final OpenAIClient openAIClient;
    private FakeBotPlayer botPlayer;
    private FollowPlayerBehavior followBehavior;
    private GatherResourceBehavior gatherBehavior;
    private int respawnTickCounter = -1;

    public FakePlayerManager(OpenAIClient openAIClient) {
        this.openAIClient = openAIClient;
    }

    public void spawn(MinecraftServer server) {
        BotConfig config = StarbaseAIMod.CONFIG.bot();
        BehaviorConfig behaviorConfig = StarbaseAIMod.CONFIG.behavior();
        ServerWorld world = server.getOverworld();

        var profile = FakeBotPlayer.createProfile(config.botName);
        botPlayer = new FakeBotPlayer(server, world, profile);

        FakeClientConnection fakeConnection = new FakeClientConnection();
        var clientData = ConnectedClientData.createDefault(profile, false);

        server.getPlayerManager().onPlayerConnect(fakeConnection, botPlayer, clientData);

        // teleport(ServerWorld, double, double, double, Set<PositionFlag>, float, float)
        botPlayer.teleport(world, config.spawnX, config.spawnY, config.spawnZ,
                Set.of(), 0f, 0f);
        botPlayer.setHealth(20.0f);
        botPlayer.showAllSkinLayers();

        // Set up movement system
        BotPathfinder pathfinder = new BotPathfinder(config.pathMaxRange);
        BotMovementController movementController = new BotMovementController(pathfinder, config);

        // Set up environment scanner
        EnvironmentScanner environmentScanner = new EnvironmentScanner();

        // Set up behavior system
        BotBehaviorManager behaviorManager = new BotBehaviorManager();

        // Register survival behaviors (priority 1)
        if (behaviorConfig.enableFleeHostile) behaviorManager.register(new FleeHostileBehavior());
        if (behaviorConfig.enableAvoidDanger) behaviorManager.register(new AvoidDangerBehavior());
        if (behaviorConfig.enableAvoidDrowning) behaviorManager.register(new AvoidDrowningBehavior());
        if (behaviorConfig.enableNightShelter) behaviorManager.register(new NightShelterBehavior());

        // Register task behaviors (priority 2)
        followBehavior = new FollowPlayerBehavior();
        behaviorManager.register(followBehavior);

        gatherBehavior = new GatherResourceBehavior();
        behaviorManager.register(gatherBehavior);

        // Register social behaviors (priority 3)
        if (behaviorConfig.enableApproachPlayer) behaviorManager.register(new ApproachPlayerBehavior());

        // Register idle behaviors (priority 4)
        if (behaviorConfig.enableIdleWander) behaviorManager.register(new IdleWanderBehavior());
        if (behaviorConfig.enableIdleLookAround) behaviorManager.register(new IdleLookAroundBehavior());

        // Set up spontaneous chat
        SpontaneousChat spontaneousChat = new SpontaneousChat(
                openAIClient, StarbaseAIMod.CONFIG.personality());

        // Wire everything into the bot
        botPlayer.initSystems(movementController, behaviorManager, environmentScanner, spontaneousChat);

        respawnTickCounter = -1;

        StarbaseAIMod.LOGGER.info("Spawned bot player: {} at ({}, {}, {})",
                config.botName, config.spawnX, config.spawnY, config.spawnZ);
    }

    public void despawn(MinecraftServer server) {
        if (botPlayer != null) {
            botPlayer.networkHandler.disconnect(Text.literal("Server stopping"));
            botPlayer = null;
            followBehavior = null;
            gatherBehavior = null;
            respawnTickCounter = -1;
            StarbaseAIMod.LOGGER.info("Despawned bot player");
        }
    }

    public void tick(MinecraftServer server) {
        if (botPlayer == null) return;

        if (botPlayer.isDead()) {
            if (respawnTickCounter < 0) {
                respawnTickCounter = 0;
                StarbaseAIMod.LOGGER.info("Bot player died! Respawning in {} ticks...",
                        StarbaseAIMod.CONFIG.bot().respawnDelayTicks);
            }

            respawnTickCounter++;
            if (respawnTickCounter >= StarbaseAIMod.CONFIG.bot().respawnDelayTicks) {
                respawn(server);
            }
        }
    }

    private void respawn(MinecraftServer server) {
        StarbaseAIMod.LOGGER.info("Respawning bot player...");
        botPlayer.networkHandler.disconnect(Text.literal("Respawning"));
        botPlayer = null;
        followBehavior = null;
        gatherBehavior = null;
        respawnTickCounter = -1;
        spawn(server);
    }

    public FakeBotPlayer getBotPlayer() {
        return botPlayer;
    }

    public FollowPlayerBehavior getFollowBehavior() {
        return followBehavior;
    }

    public GatherResourceBehavior getGatherBehavior() {
        return gatherBehavior;
    }
}
