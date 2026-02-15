package com.starbase.ai.behavior.survival;

import com.starbase.ai.behavior.Behavior;
import com.starbase.ai.behavior.BotContext;
import com.starbase.ai.behavior.EnvironmentScanner;
import com.starbase.ai.movement.MovementGoal;

public class FleeHostileBehavior implements Behavior {

    private static final double FLEE_TRIGGER_RANGE = 8.0;
    private static final double SAFE_DISTANCE = 16.0;
    private static final int MAX_FLEE_TICKS = 200; // 10 seconds max

    private int fleeTicksRemaining;

    @Override
    public String name() {
        return "FleeHostile";
    }

    @Override
    public int priority() {
        return 1; // Critical -- survival
    }

    @Override
    public boolean canStart(BotContext ctx) {
        EnvironmentScanner.Snapshot env = ctx.environment();
        if (env == null) return false;
        return env.hasHostilesNearby(FLEE_TRIGGER_RANGE);
    }

    @Override
    public boolean shouldContinue(BotContext ctx) {
        fleeTicksRemaining--;
        if (fleeTicksRemaining <= 0) return false;

        EnvironmentScanner.Snapshot env = ctx.environment();
        if (env == null) return false;
        // Keep fleeing while hostiles are within safe distance
        return env.hasHostilesNearby(SAFE_DISTANCE);
    }

    @Override
    public void start(BotContext ctx) {
        fleeTicksRemaining = MAX_FLEE_TICKS;
        EnvironmentScanner.MobInfo closest = ctx.environment().closestHostile();
        if (closest != null) {
            ctx.movement().setGoal(new MovementGoal.Flee(closest.pos(), SAFE_DISTANCE));
        }
    }

    @Override
    public void tick(BotContext ctx) {
        // Update flee direction if the hostile is moving
        EnvironmentScanner.MobInfo closest = ctx.environment().closestHostile();
        if (closest != null && closest.distance() < FLEE_TRIGGER_RANGE) {
            // Re-flee from the new position of the hostile
            ctx.movement().setGoal(new MovementGoal.Flee(closest.pos(), SAFE_DISTANCE));
        }
    }

    @Override
    public void stop(BotContext ctx) {
        ctx.movement().clearGoal();
    }
}
