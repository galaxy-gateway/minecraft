package com.starbase.ai.behavior.task;

import com.starbase.ai.behavior.Behavior;
import com.starbase.ai.behavior.BotContext;
import com.starbase.ai.movement.MovementGoal;
import net.minecraft.server.network.ServerPlayerEntity;

public class FollowPlayerBehavior implements Behavior {

    private static final double FOLLOW_MIN_DIST = 3.0;
    private static final double FOLLOW_MAX_DIST = 32.0;

    private ServerPlayerEntity targetPlayer;
    private boolean active;

    @Override
    public String name() {
        return "FollowPlayer";
    }

    @Override
    public int priority() {
        return 2; // High priority -- player command
    }

    @Override
    public boolean canStart(BotContext ctx) {
        return active && targetPlayer != null && !targetPlayer.isRemoved();
    }

    @Override
    public boolean shouldContinue(BotContext ctx) {
        if (!active || targetPlayer == null || targetPlayer.isRemoved()) return false;
        double dist = ctx.bot().distanceTo(targetPlayer);
        // Stop following if player is way too far
        return dist <= FOLLOW_MAX_DIST;
    }

    @Override
    public void start(BotContext ctx) {
        ctx.movement().setGoal(new MovementGoal.FollowPlayer(targetPlayer, FOLLOW_MIN_DIST, FOLLOW_MAX_DIST));
    }

    @Override
    public void tick(BotContext ctx) {
        // Movement controller handles the following
    }

    @Override
    public void stop(BotContext ctx) {
        ctx.movement().clearGoal();
        active = false;
        targetPlayer = null;
    }

    /**
     * Activate follow mode for a specific player.
     * Call this from chat command handling.
     */
    public void follow(ServerPlayerEntity player) {
        this.targetPlayer = player;
        this.active = true;
    }

    /**
     * Stop following.
     */
    public void stopFollowing() {
        this.active = false;
        this.targetPlayer = null;
    }

    public boolean isFollowing() {
        return active;
    }

    public ServerPlayerEntity getTarget() {
        return targetPlayer;
    }
}
