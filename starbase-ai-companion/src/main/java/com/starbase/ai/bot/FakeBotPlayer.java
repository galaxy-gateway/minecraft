package com.starbase.ai.bot;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Uuids;

public class FakeBotPlayer extends ServerPlayerEntity {

    public FakeBotPlayer(MinecraftServer server, ServerWorld world, GameProfile profile) {
        super(server, world, profile, SyncedClientOptions.createDefault());
    }

    /**
     * Set all skin model parts visible (hat, jacket, sleeves, pants).
     * PLAYER_MODEL_PARTS is protected, so we access it from within the subclass.
     */
    public void showAllSkinLayers() {
        this.getDataTracker().set(PLAYER_MODEL_PARTS, (byte) 0x7f);
    }

    public static GameProfile createProfile(String botName) {
        return new GameProfile(Uuids.getOfflinePlayerUuid(botName), botName);
    }

    @Override
    public void tick() {
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
