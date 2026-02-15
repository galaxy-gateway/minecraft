package com.starbase.ai.bot;

import com.starbase.ai.StarbaseAIMod;
import com.starbase.ai.config.BotConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.Set;

public class FakePlayerManager {
    private FakeBotPlayer botPlayer;
    private int respawnTickCounter = -1;

    public void spawn(MinecraftServer server) {
        BotConfig config = StarbaseAIMod.CONFIG.bot();
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

        respawnTickCounter = -1;

        StarbaseAIMod.LOGGER.info("Spawned bot player: {} at ({}, {}, {})",
                config.botName, config.spawnX, config.spawnY, config.spawnZ);
    }

    public void despawn(MinecraftServer server) {
        if (botPlayer != null) {
            botPlayer.networkHandler.disconnect(Text.literal("Server stopping"));
            botPlayer = null;
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
        respawnTickCounter = -1;
        spawn(server);
    }

    public FakeBotPlayer getBotPlayer() {
        return botPlayer;
    }
}
