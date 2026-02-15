package com.starbase.ai.behavior;

import com.starbase.ai.bot.FakeBotPlayer;
import com.starbase.ai.movement.BotMovementController;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

public class BotContext {

    private final FakeBotPlayer bot;
    private final BotMovementController movementController;
    private final EnvironmentScanner.Snapshot environment;
    private final MinecraftServer server;

    public BotContext(FakeBotPlayer bot, BotMovementController movementController,
                      EnvironmentScanner.Snapshot environment, MinecraftServer server) {
        this.bot = bot;
        this.movementController = movementController;
        this.environment = environment;
        this.server = server;
    }

    public FakeBotPlayer bot() {
        return bot;
    }

    public BotMovementController movement() {
        return movementController;
    }

    public EnvironmentScanner.Snapshot environment() {
        return environment;
    }

    public MinecraftServer server() {
        return server;
    }

    public ServerWorld world() {
        return bot.getServerWorld();
    }
}
