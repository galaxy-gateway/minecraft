package com.starbase.ai.behavior.idle;

import com.starbase.ai.behavior.Behavior;
import com.starbase.ai.behavior.BotContext;
import com.starbase.ai.movement.MovementGoal;

import java.util.Random;

public class IdleWanderBehavior implements Behavior {

    private static final int MIN_WAIT_TICKS = 600;  // 30 seconds
    private static final int MAX_WAIT_TICKS = 1200;  // 60 seconds
    private static final int WANDER_TIMEOUT = 200;   // 10 seconds max per wander

    private final Random random = new Random();
    private int waitTicksRemaining;
    private int wanderTicksRemaining;

    public IdleWanderBehavior() {
        this.waitTicksRemaining = MIN_WAIT_TICKS + random.nextInt(MAX_WAIT_TICKS - MIN_WAIT_TICKS);
    }

    @Override
    public String name() {
        return "IdleWander";
    }

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public boolean canStart(BotContext ctx) {
        // Only wander if nothing else is happening and enough time has passed
        if (waitTicksRemaining > 0) {
            waitTicksRemaining--;
            return false;
        }
        return !ctx.movement().isMoving();
    }

    @Override
    public boolean shouldContinue(BotContext ctx) {
        wanderTicksRemaining--;
        return wanderTicksRemaining > 0 && ctx.movement().isMoving() && !ctx.movement().hasReachedGoal();
    }

    @Override
    public void start(BotContext ctx) {
        wanderTicksRemaining = WANDER_TIMEOUT;
        ctx.movement().setGoal(new MovementGoal.Wander(ctx.bot().getBlockPos(), 20));
    }

    @Override
    public void tick(BotContext ctx) {
        // Movement controller handles the actual walking
    }

    @Override
    public void stop(BotContext ctx) {
        ctx.movement().clearGoal();
        // Reset wait timer for next wander
        waitTicksRemaining = MIN_WAIT_TICKS + random.nextInt(MAX_WAIT_TICKS - MIN_WAIT_TICKS);
    }
}
