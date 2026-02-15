package com.starbase.ai.bot;

import com.mojang.authlib.GameProfile;
import com.starbase.ai.StarbaseAIMod;
import com.starbase.ai.behavior.BotBehaviorManager;
import com.starbase.ai.behavior.BotContext;
import com.starbase.ai.behavior.EnvironmentScanner;
import com.starbase.ai.chat.SpontaneousChat;
import com.starbase.ai.movement.BotMovementController;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Uuids;

public class FakeBotPlayer extends ServerPlayerEntity {

    private BotMovementController movementController;
    private BotBehaviorManager behaviorManager;
    private EnvironmentScanner environmentScanner;
    private SpontaneousChat spontaneousChat;

    public FakeBotPlayer(MinecraftServer server, ServerWorld world, GameProfile profile) {
        super(server, world, profile, SyncedClientOptions.createDefault());
    }

    public void initSystems(BotMovementController movementController,
                            BotBehaviorManager behaviorManager,
                            EnvironmentScanner environmentScanner,
                            SpontaneousChat spontaneousChat) {
        this.movementController = movementController;
        this.behaviorManager = behaviorManager;
        this.environmentScanner = environmentScanner;
        this.spontaneousChat = spontaneousChat;
    }

    public void showAllSkinLayers() {
        this.getDataTracker().set(PLAYER_MODEL_PARTS, (byte) 0x7f);
    }

    public static GameProfile createProfile(String botName) {
        return new GameProfile(Uuids.getOfflinePlayerUuid(botName), botName);
    }

    public BotMovementController getMovementController() {
        return movementController;
    }

    public BotBehaviorManager getBehaviorManager() {
        return behaviorManager;
    }

    @Override
    public void tick() {
        // 1. Pre-tick movement (set up jumps before vanilla processes physics)
        if (movementController != null) {
            movementController.preTick(this);
        }

        // 2. Vanilla tick (gravity, damage, status effects, etc.)
        super.tick();

        // 3. Environment scan (every 20 ticks)
        if (environmentScanner != null) {
            environmentScanner.maybeScan(this);
        }

        // 4. Behavior tick (priority-based behavior selection)
        if (behaviorManager != null && environmentScanner != null) {
            EnvironmentScanner.Snapshot snapshot = environmentScanner.getSnapshot();
            if (snapshot != null) {
                BotContext ctx = new BotContext(this, movementController, snapshot, this.getServer());
                behaviorManager.tick(ctx);
            }
        }

        // 5. Movement tick (apply horizontal movement after vanilla physics)
        if (movementController != null) {
            movementController.postTick(this);
        }

        // 6. Spontaneous chat
        if (spontaneousChat != null && environmentScanner != null) {
            EnvironmentScanner.Snapshot snapshot = environmentScanner.getSnapshot();
            if (snapshot != null) {
                spontaneousChat.maybeTick(this, snapshot, this.getServer());
            }
        }

        // 7. Look management (context-dependent)
        updateLook();

        // 8. Position sync
        syncPosition();
    }

    private void updateLook() {
        // When moving, the movement controller handles yaw
        if (movementController != null && movementController.isMoving()) {
            return;
        }
        // When idle, look at the nearest player
        if (StarbaseAIMod.CONFIG.bot().lookAtNearestPlayer) {
            lookAtNearestPlayer();
        }
    }

    private void syncPosition() {
        boolean isMoving = movementController != null && movementController.isMoving();
        // Sync every tick when moving, every 10 ticks when idle
        if (isMoving || this.getServer().getTicks() % 10 == 0) {
            this.networkHandler.syncWithPlayerPosition();
            this.getServerWorld().getChunkManager().updatePosition(this);
        }
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
