package com.starbase.ai.behavior.survival;

import com.starbase.ai.behavior.Behavior;
import com.starbase.ai.behavior.BotContext;
import com.starbase.ai.behavior.EnvironmentScanner;
import com.starbase.ai.movement.MovementGoal;
import com.starbase.ai.movement.MovementUtil;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

public class NightShelterBehavior implements Behavior {

    private static final int SEARCH_RADIUS = 24;
    private static final int MAX_SEEK_TICKS = 400; // 20 seconds

    private final Random random = new Random();
    private int seekTicksRemaining;
    private boolean foundShelter;

    @Override
    public String name() {
        return "NightShelter";
    }

    @Override
    public int priority() {
        return 1; // Critical -- survival
    }

    @Override
    public boolean canStart(BotContext ctx) {
        EnvironmentScanner.Snapshot env = ctx.environment();
        if (env == null) return false;

        // Trigger: night time + no overhead cover + hostiles nearby
        return env.isNight()
                && !env.hasOverheadCover()
                && env.hasHostilesNearby(16.0);
    }

    @Override
    public boolean shouldContinue(BotContext ctx) {
        seekTicksRemaining--;
        if (seekTicksRemaining <= 0) return false;
        if (foundShelter) return false;

        EnvironmentScanner.Snapshot env = ctx.environment();
        if (env == null) return false;

        // Stop seeking if we found cover
        if (env.hasOverheadCover()) {
            foundShelter = true;
            return false;
        }

        return env.isNight();
    }

    @Override
    public void start(BotContext ctx) {
        seekTicksRemaining = MAX_SEEK_TICKS;
        foundShelter = false;

        // Try to find a sheltered position
        BlockPos shelterPos = findShelter(ctx);
        if (shelterPos != null) {
            ctx.movement().setGoal(new MovementGoal.GoToPosition(shelterPos));
        } else {
            // If no shelter found, just find a well-lit spot or stay put
            BlockPos litPos = findLitArea(ctx);
            if (litPos != null) {
                ctx.movement().setGoal(new MovementGoal.GoToPosition(litPos));
            }
        }
    }

    @Override
    public void tick(BotContext ctx) {
        // If we've reached our goal and it's still night, stay put
        if (ctx.movement().hasReachedGoal()) {
            ctx.movement().setGoal(new MovementGoal.StayHere());
        }
    }

    @Override
    public void stop(BotContext ctx) {
        ctx.movement().clearGoal();
    }

    private BlockPos findShelter(BotContext ctx) {
        ServerWorld world = ctx.world();
        BlockPos botPos = ctx.bot().getBlockPos();

        // Search for a position with overhead cover
        for (int attempt = 0; attempt < 20; attempt++) {
            int dx = random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            int dz = random.nextInt(SEARCH_RADIUS * 2 + 1) - SEARCH_RADIUS;
            BlockPos checkPos = botPos.add(dx, 0, dz);

            // Find ground level
            for (int dy = -3; dy <= 3; dy++) {
                BlockPos groundPos = checkPos.add(0, dy, 0);
                if (MovementUtil.isWalkable(world, groundPos) && hasOverheadCover(world, groundPos)) {
                    return groundPos;
                }
            }
        }
        return null;
    }

    private BlockPos findLitArea(BotContext ctx) {
        ServerWorld world = ctx.world();
        BlockPos botPos = ctx.bot().getBlockPos();

        BlockPos bestPos = null;
        int bestLight = 0;

        for (int attempt = 0; attempt < 15; attempt++) {
            int dx = random.nextInt(16) - 8;
            int dz = random.nextInt(16) - 8;
            BlockPos checkPos = botPos.add(dx, 0, dz);

            for (int dy = -2; dy <= 2; dy++) {
                BlockPos groundPos = checkPos.add(0, dy, 0);
                if (MovementUtil.isWalkable(world, groundPos)) {
                    int light = world.getLightLevel(groundPos);
                    if (light > bestLight) {
                        bestLight = light;
                        bestPos = groundPos;
                    }
                }
            }
        }
        return bestPos;
    }

    private boolean hasOverheadCover(ServerWorld world, BlockPos pos) {
        for (int y = pos.getY() + 2; y < pos.getY() + 8 && y < world.getTopY(); y++) {
            BlockPos above = new BlockPos(pos.getX(), y, pos.getZ());
            if (!world.getBlockState(above).getCollisionShape(world, above).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
