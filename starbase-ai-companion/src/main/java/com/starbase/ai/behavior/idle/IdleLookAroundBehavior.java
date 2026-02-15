package com.starbase.ai.behavior.idle;

import com.starbase.ai.behavior.Behavior;
import com.starbase.ai.behavior.BotContext;
import com.starbase.ai.behavior.EnvironmentScanner;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;

import java.util.Random;

public class IdleLookAroundBehavior implements Behavior {

    private static final int MIN_LOOK_TICKS = 40;   // 2 seconds
    private static final int MAX_LOOK_TICKS = 100;   // 5 seconds

    private final Random random = new Random();
    private int lookTicksRemaining;
    private float targetYaw;
    private float targetPitch;

    @Override
    public String name() {
        return "IdleLookAround";
    }

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public boolean canStart(BotContext ctx) {
        // Can always start as an idle behavior when nothing else is happening
        return !ctx.movement().isMoving();
    }

    @Override
    public boolean shouldContinue(BotContext ctx) {
        return lookTicksRemaining > 0;
    }

    @Override
    public void start(BotContext ctx) {
        lookTicksRemaining = MIN_LOOK_TICKS + random.nextInt(MAX_LOOK_TICKS - MIN_LOOK_TICKS);
        pickLookTarget(ctx);
    }

    @Override
    public void tick(BotContext ctx) {
        lookTicksRemaining--;

        // Smoothly rotate toward target
        float currentYaw = ctx.bot().getYaw();
        float currentPitch = ctx.bot().getPitch();

        float newYaw = lerpAngle(currentYaw, targetYaw, 0.05f);
        float newPitch = lerpAngle(currentPitch, targetPitch, 0.05f);

        ctx.bot().setYaw(newYaw);
        ctx.bot().setPitch(newPitch);
        ctx.bot().headYaw = newYaw;

        // Broadcast head yaw
        ctx.bot().getServerWorld().getChunkManager().sendToOtherNearbyPlayers(ctx.bot(),
                new EntitySetHeadYawS2CPacket(ctx.bot(), (byte) (newYaw * 256.0f / 360.0f)));
    }

    @Override
    public void stop(BotContext ctx) {
        // Nothing to clean up
    }

    private void pickLookTarget(BotContext ctx) {
        EnvironmentScanner.Snapshot env = ctx.environment();
        if (env == null) {
            // Random direction
            targetYaw = ctx.bot().getYaw() + (random.nextFloat() * 120 - 60);
            targetPitch = random.nextFloat() * 30 - 15;
            return;
        }

        // 50% chance to look at nearest player
        EnvironmentScanner.PlayerInfo nearestPlayer = env.closestPlayer();
        if (nearestPlayer != null && random.nextFloat() < 0.5f) {
            double dx = nearestPlayer.player().getX() - ctx.bot().getX();
            double dy = nearestPlayer.player().getEyeY() - ctx.bot().getEyeY();
            double dz = nearestPlayer.player().getZ() - ctx.bot().getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontalDist)));
            return;
        }

        // 30% chance to look at a nearby mob
        if (!env.passiveMobs().isEmpty() && random.nextFloat() < 0.3f) {
            EnvironmentScanner.MobInfo mob = env.passiveMobs().get(random.nextInt(env.passiveMobs().size()));
            double dx = mob.pos().getX() + 0.5 - ctx.bot().getX();
            double dz = mob.pos().getZ() + 0.5 - ctx.bot().getZ();
            targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
            targetPitch = random.nextFloat() * 10 - 5;
            return;
        }

        // Otherwise random direction
        targetYaw = ctx.bot().getYaw() + (random.nextFloat() * 120 - 60);
        targetPitch = random.nextFloat() * 30 - 15;
    }

    private static float lerpAngle(float current, float target, float factor) {
        float diff = target - current;
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        return current + diff * factor;
    }
}
