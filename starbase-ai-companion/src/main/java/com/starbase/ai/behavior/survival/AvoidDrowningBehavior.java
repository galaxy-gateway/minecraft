package com.starbase.ai.behavior.survival;

import com.starbase.ai.behavior.Behavior;
import com.starbase.ai.behavior.BotContext;
import com.starbase.ai.bot.FakeBotPlayer;
import com.starbase.ai.movement.MovementGoal;
import net.minecraft.util.math.BlockPos;

public class AvoidDrowningBehavior implements Behavior {

    private static final int AIR_TRIGGER = 200;   // ~10 seconds of air left (max 300)
    private static final int AIR_SAFE = 280;       // keep swimming until safely surfaced
    private static final int SURFACE_SEARCH_HEIGHT = 10;

    @Override
    public String name() {
        return "AvoidDrowning";
    }

    @Override
    public int priority() {
        return 1; // Critical -- survival
    }

    @Override
    public boolean canStart(BotContext ctx) {
        FakeBotPlayer bot = ctx.bot();
        return bot.isSubmergedInWater() && bot.getAir() < AIR_TRIGGER;
    }

    @Override
    public boolean shouldContinue(BotContext ctx) {
        FakeBotPlayer bot = ctx.bot();
        // Keep going until safely surfaced with enough air
        return bot.isSubmergedInWater() || bot.getAir() < AIR_SAFE;
    }

    @Override
    public void start(BotContext ctx) {
        setSurfaceGoal(ctx);
    }

    @Override
    public void tick(BotContext ctx) {
        // Re-target surface if the current goal was cleared
        if (!ctx.movement().isMoving()) {
            setSurfaceGoal(ctx);
        }
    }

    @Override
    public void stop(BotContext ctx) {
        ctx.movement().clearGoal();
    }

    private void setSurfaceGoal(BotContext ctx) {
        FakeBotPlayer bot = ctx.bot();
        BlockPos target = new BlockPos(
                bot.getBlockX(),
                bot.getBlockY() + SURFACE_SEARCH_HEIGHT,
                bot.getBlockZ()
        );
        ctx.movement().setGoal(new MovementGoal.GoToPosition(target));
    }
}
