package com.starbase.ai.behavior.idle;

import com.starbase.ai.behavior.Behavior;
import com.starbase.ai.behavior.BotContext;
import com.starbase.ai.behavior.EnvironmentScanner;
import com.starbase.ai.movement.MovementGoal;

public class ApproachPlayerBehavior implements Behavior {

    private static final double APPROACH_RANGE = 16.0;
    private static final double STOP_DISTANCE = 4.0;
    private static final int TIMEOUT_TICKS = 200; // 10 seconds max

    private int ticksRemaining;

    @Override
    public String name() {
        return "ApproachPlayer";
    }

    @Override
    public int priority() {
        return 3; // Medium priority -- above idle but below tasks
    }

    @Override
    public boolean canStart(BotContext ctx) {
        if (ctx.movement().isMoving()) return false;
        EnvironmentScanner.Snapshot env = ctx.environment();
        if (env == null) return false;

        EnvironmentScanner.PlayerInfo nearest = env.closestPlayer();
        // Approach if a player is nearby but not too close
        return nearest != null
                && nearest.distance() > STOP_DISTANCE
                && nearest.distance() <= APPROACH_RANGE;
    }

    @Override
    public boolean shouldContinue(BotContext ctx) {
        ticksRemaining--;
        if (ticksRemaining <= 0) return false;
        if (!ctx.movement().isMoving()) return false;

        EnvironmentScanner.Snapshot env = ctx.environment();
        if (env == null) return false;
        EnvironmentScanner.PlayerInfo nearest = env.closestPlayer();
        // Stop if we're close enough or player went away
        return nearest != null && nearest.distance() > STOP_DISTANCE;
    }

    @Override
    public void start(BotContext ctx) {
        ticksRemaining = TIMEOUT_TICKS;
        EnvironmentScanner.PlayerInfo nearest = ctx.environment().closestPlayer();
        if (nearest != null) {
            ctx.movement().setGoal(new MovementGoal.FollowPlayer(nearest.player(), STOP_DISTANCE, APPROACH_RANGE));
        }
    }

    @Override
    public void tick(BotContext ctx) {
        // Movement controller handles walking
    }

    @Override
    public void stop(BotContext ctx) {
        ctx.movement().clearGoal();
    }
}
