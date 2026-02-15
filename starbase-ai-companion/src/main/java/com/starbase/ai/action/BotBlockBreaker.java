package com.starbase.ai.action;

import com.starbase.ai.bot.FakeBotPlayer;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public class BotBlockBreaker {

    private static final int ARM_SWING_INTERVAL = 5; // ticks between arm swings

    private BlockPos targetBlock;
    private int breakTicksRemaining;
    private int totalBreakTicks;

    /**
     * Start breaking a block. Returns false if the block is unbreakable.
     */
    public boolean startBreaking(FakeBotPlayer bot, BlockPos pos) {
        ServerWorld world = bot.getServerWorld();
        BlockState state = world.getBlockState(pos);

        if (state.isAir()) return false;

        float hardness = state.getHardness(world, pos);
        if (hardness < 0) return false; // Unbreakable (bedrock, etc.)

        // Calculate break time: scale hardness to ticks
        // Soft blocks (sand=0.5): ~10 ticks (0.5s)
        // Medium blocks (stone=1.5): ~30 ticks (1.5s)
        // Hard blocks (obsidian=50): capped at 200 ticks (10s)
        totalBreakTicks = Math.max(4, Math.min(200, (int) (hardness * 20)));
        breakTicksRemaining = totalBreakTicks;
        targetBlock = pos;

        // Start break progress visual
        world.setBlockBreakingInfo(bot.getId(), pos, 0);

        return true;
    }

    /**
     * Tick the block breaking process. Returns true when the block is broken.
     * Call this every tick while mining.
     */
    public boolean tick(FakeBotPlayer bot) {
        if (targetBlock == null) return false;

        ServerWorld world = bot.getServerWorld();
        BlockState state = world.getBlockState(targetBlock);

        // Block was already broken (by someone else or world change)
        if (state.isAir()) {
            cleanup(bot);
            return true;
        }

        // Face the target block
        faceBlock(bot, targetBlock);

        // Swing arm periodically
        if (breakTicksRemaining % ARM_SWING_INTERVAL == 0) {
            bot.swingHand(Hand.MAIN_HAND);
        }

        breakTicksRemaining--;

        // Update break progress visual (stages 0-9)
        int stage = (int) ((1.0 - (double) breakTicksRemaining / totalBreakTicks) * 10);
        stage = Math.min(stage, 9);
        world.setBlockBreakingInfo(bot.getId(), targetBlock, stage);

        if (breakTicksRemaining <= 0) {
            // Break the block
            boolean broken = bot.interactionManager.tryBreakBlock(targetBlock);
            cleanup(bot);
            return broken;
        }

        return false;
    }

    public boolean isBreaking() {
        return targetBlock != null;
    }

    public BlockPos getTarget() {
        return targetBlock;
    }

    public void cancel(FakeBotPlayer bot) {
        if (targetBlock != null) {
            cleanup(bot);
        }
    }

    private void cleanup(FakeBotPlayer bot) {
        if (targetBlock != null) {
            // Clear break progress visual
            bot.getServerWorld().setBlockBreakingInfo(bot.getId(), targetBlock, -1);
        }
        targetBlock = null;
        breakTicksRemaining = 0;
        totalBreakTicks = 0;
    }

    private void faceBlock(FakeBotPlayer bot, BlockPos pos) {
        double dx = pos.getX() + 0.5 - bot.getX();
        double dy = pos.getY() + 0.5 - bot.getEyeY();
        double dz = pos.getZ() + 0.5 - bot.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontalDist)));

        bot.setYaw(yaw);
        bot.setPitch(pitch);
        bot.headYaw = yaw;

        bot.getServerWorld().getChunkManager().sendToOtherNearbyPlayers(bot,
                new EntitySetHeadYawS2CPacket(bot, (byte) (yaw * 256.0f / 360.0f)));
    }
}
