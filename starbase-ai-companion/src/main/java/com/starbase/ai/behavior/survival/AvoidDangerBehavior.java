package com.starbase.ai.behavior.survival;

import com.starbase.ai.behavior.Behavior;
import com.starbase.ai.behavior.BotContext;
import com.starbase.ai.behavior.EnvironmentScanner;
import com.starbase.ai.movement.MovementGoal;

public class AvoidDangerBehavior implements Behavior {

    private static final double SAFE_DISTANCE = 5.0;
    private static final int MAX_AVOID_TICKS = 100; // 5 seconds

    private int avoidTicksRemaining;

    @Override
    public String name() {
        return "AvoidDanger";
    }

    @Override
    public int priority() {
        return 1; // Critical -- survival
    }

    @Override
    public boolean canStart(BotContext ctx) {
        EnvironmentScanner.Snapshot env = ctx.environment();
        if (env == null) return false;
        return env.nearHazard() && env.nearestHazardPos() != null;
    }

    @Override
    public boolean shouldContinue(BotContext ctx) {
        avoidTicksRemaining--;
        if (avoidTicksRemaining <= 0) return false;

        EnvironmentScanner.Snapshot env = ctx.environment();
        if (env == null) return false;
        return env.nearHazard();
    }

    @Override
    public void start(BotContext ctx) {
        avoidTicksRemaining = MAX_AVOID_TICKS;
        EnvironmentScanner.Snapshot env = ctx.environment();
        if (env.nearestHazardPos() != null) {
            ctx.movement().setGoal(new MovementGoal.Flee(env.nearestHazardPos(), SAFE_DISTANCE));
        }
    }

    @Override
    public void tick(BotContext ctx) {
        // Update flee direction if hazard position changed
        EnvironmentScanner.Snapshot env = ctx.environment();
        if (env != null && env.nearestHazardPos() != null) {
            double dist = ctx.bot().getBlockPos().getSquaredDistance(env.nearestHazardPos());
            if (dist < 4.0) { // Within 2 blocks, keep fleeing
                ctx.movement().setGoal(new MovementGoal.Flee(env.nearestHazardPos(), SAFE_DISTANCE));
            }
        }
    }

    @Override
    public void stop(BotContext ctx) {
        ctx.movement().clearGoal();
    }
}
