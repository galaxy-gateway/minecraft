package com.starbase.ai.movement;

import com.starbase.ai.StarbaseAIMod;
import com.starbase.ai.bot.FakeBotPlayer;
import com.starbase.ai.config.BotConfig;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Random;

public class BotMovementController {

    private final BotPathfinder pathfinder;
    private final BotConfig config;
    private final Random random = new Random();

    // Current movement state
    private MovementGoal currentGoal;
    private List<BlockPos> currentPath;
    private int pathIndex;
    private BlockPos lastPathTarget;
    private int ticksSinceRepath;

    // Humanization state
    private int pauseTicksRemaining;
    private int walkingTicks;
    private double angleDeviation;
    private int deviationChangeTicks;

    // Jump state
    private boolean jumpRequested;

    public BotMovementController(BotPathfinder pathfinder, BotConfig config) {
        this.pathfinder = pathfinder;
        this.config = config;
    }

    public void setGoal(MovementGoal goal) {
        this.currentGoal = goal;
        this.currentPath = null;
        this.pathIndex = 0;
        this.lastPathTarget = null;
        this.ticksSinceRepath = 0;
        this.walkingTicks = 0;
        this.pauseTicksRemaining = 0;
        this.jumpRequested = false;
    }

    public void clearGoal() {
        this.currentGoal = null;
        this.currentPath = null;
        this.pathIndex = 0;
        this.walkingTicks = 0;
        this.pauseTicksRemaining = 0;
        this.jumpRequested = false;
    }

    public boolean isMoving() {
        return currentGoal != null && !(currentGoal instanceof MovementGoal.StayHere);
    }

    public MovementGoal getGoal() {
        return currentGoal;
    }

    public boolean hasReachedGoal() {
        return currentGoal != null && currentPath != null && pathIndex >= currentPath.size();
    }

    /**
     * Called before super.tick() to set up jump velocity.
     */
    public void preTick(FakeBotPlayer bot) {
        if (jumpRequested && bot.isOnGround()) {
            bot.setVelocity(bot.getVelocity().x, 0.42, bot.getVelocity().z);
            jumpRequested = false;
        }
    }

    /**
     * Called after super.tick() to apply horizontal movement.
     */
    public void postTick(FakeBotPlayer bot) {
        if (currentGoal == null) return;

        if (currentGoal instanceof MovementGoal.StayHere) return;

        ServerWorld world = bot.getServerWorld();
        BlockPos targetPos = resolveTargetPos(bot);

        if (targetPos == null) {
            clearGoal();
            return;
        }

        // Check if we're already close enough (for FollowPlayer)
        if (currentGoal instanceof MovementGoal.FollowPlayer follow) {
            double distToTarget = bot.getPos().distanceTo(follow.target().getPos());
            if (distToTarget <= follow.minDist()) {
                currentPath = null;
                return;
            }
            if (distToTarget > follow.maxDist()) {
                clearGoal();
                return;
            }
        }

        // Repath if needed
        ticksSinceRepath++;
        if (shouldRepath(targetPos)) {
            computePath(world, bot.getBlockPos(), targetPos);
            ticksSinceRepath = 0;
        }

        if (currentPath == null || currentPath.isEmpty() || pathIndex >= currentPath.size()) {
            return;
        }

        // Handle pauses (humanization)
        if (pauseTicksRemaining > 0) {
            pauseTicksRemaining--;
            return;
        }

        moveTowardWaypoint(bot);
    }

    private BlockPos resolveTargetPos(FakeBotPlayer bot) {
        return switch (currentGoal) {
            case MovementGoal.FollowPlayer follow -> follow.target().getBlockPos();
            case MovementGoal.GoToPosition goTo -> goTo.target();
            case MovementGoal.Wander wander -> {
                // Pick a random position within radius if we don't have a path
                if (currentPath == null || pathIndex >= currentPath.size()) {
                    int rx = wander.center().getX() + random.nextInt(wander.radius() * 2 + 1) - wander.radius();
                    int rz = wander.center().getZ() + random.nextInt(wander.radius() * 2 + 1) - wander.radius();
                    yield new BlockPos(rx, wander.center().getY(), rz);
                }
                yield lastPathTarget;
            }
            case MovementGoal.Flee flee -> {
                // Move away from danger
                double dx = bot.getX() - flee.dangerSource().getX();
                double dz = bot.getZ() - flee.dangerSource().getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist < 0.1) {
                    dx = random.nextDouble() - 0.5;
                    dz = random.nextDouble() - 0.5;
                    dist = Math.sqrt(dx * dx + dz * dz);
                }
                double scale = flee.safeDistance() / dist;
                yield new BlockPos(
                        (int) (bot.getX() + dx * scale),
                        bot.getBlockY(),
                        (int) (bot.getZ() + dz * scale)
                );
            }
            case MovementGoal.StayHere ignored -> null;
        };
    }

    private boolean shouldRepath(BlockPos targetPos) {
        // First path computation
        if (currentPath == null) return true;

        // Path completed
        if (pathIndex >= currentPath.size()) return true;

        // Periodic repath
        if (ticksSinceRepath >= config.pathRecalcTicks) return true;

        // Target moved significantly
        if (lastPathTarget != null && lastPathTarget.getManhattanDistance(targetPos) > 3) return true;

        return false;
    }

    private void computePath(ServerWorld world, BlockPos from, BlockPos to) {
        currentPath = pathfinder.findPath(world, from, to);
        pathIndex = 0;
        lastPathTarget = to;

        if (currentPath.isEmpty()) {
            StarbaseAIMod.LOGGER.debug("Pathfinder found no path from {} to {}", from, to);
        }
    }

    private void moveTowardWaypoint(FakeBotPlayer bot) {
        if (pathIndex >= currentPath.size()) return;

        BlockPos waypoint = currentPath.get(pathIndex);
        double targetX = waypoint.getX() + 0.5;
        double targetZ = waypoint.getZ() + 0.5;

        double dx = targetX - bot.getX();
        double dz = targetZ - bot.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        // Check waypoint arrival
        if (horizontalDist < 0.5) {
            pathIndex++;
            walkingTicks++;
            if (pathIndex >= currentPath.size()) {
                onPathComplete();
                return;
            }
            // Process next waypoint in the same tick
            moveTowardWaypoint(bot);
            return;
        }

        // Check if we need to jump (next waypoint is higher)
        if (waypoint.getY() > bot.getBlockY() && bot.isOnGround()) {
            jumpRequested = true;
        }

        // Calculate speed
        double distToGoal = getDistToGoalTarget(bot);
        boolean shouldSprint = distToGoal > 10.0 && walkingTicks > 5;
        double speed = shouldSprint ? config.sprintSpeed : config.walkSpeed;

        // Deceleration near target (humanization)
        int remainingWaypoints = currentPath.size() - pathIndex;
        if (distToGoal < 3.0 && remainingWaypoints <= 3) {
            speed *= Math.max(0.3, distToGoal / 3.0);
        }

        // Apply angle deviation (humanization)
        updateAngleDeviation();
        double angle = Math.atan2(dz, dx);
        angle += Math.toRadians(angleDeviation);

        double moveX = Math.cos(angle) * speed;
        double moveZ = Math.sin(angle) * speed;

        // Apply horizontal movement with collision
        bot.move(MovementType.SELF, new Vec3d(moveX, 0, moveZ));

        // Update facing direction
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        bot.setYaw(yaw);
        bot.headYaw = yaw;
        broadcastHeadYaw(bot);

        walkingTicks++;

        // Random pause on long walks (humanization)
        if (walkingTicks > 40 && random.nextFloat() < 0.01f) {
            pauseTicksRemaining = 10 + random.nextInt(20);
        }

        // Sync position every tick during movement
        bot.networkHandler.syncWithPlayerPosition();
        bot.getServerWorld().getChunkManager().updatePosition(bot);
    }

    private double getDistToGoalTarget(FakeBotPlayer bot) {
        if (currentPath == null || currentPath.isEmpty()) return 0;
        BlockPos lastWaypoint = currentPath.get(currentPath.size() - 1);
        return Math.sqrt(bot.getPos().squaredDistanceTo(
                lastWaypoint.getX() + 0.5, lastWaypoint.getY(), lastWaypoint.getZ() + 0.5));
    }

    private void onPathComplete() {
        walkingTicks = 0;
        // For one-shot goals, clear them when path is done
        if (currentGoal instanceof MovementGoal.GoToPosition
                || currentGoal instanceof MovementGoal.Flee) {
            clearGoal();
        }
        // For Wander, the next tick will pick a new random target
        // For FollowPlayer, repath will compute a new path if needed
    }

    private void updateAngleDeviation() {
        deviationChangeTicks--;
        if (deviationChangeTicks <= 0) {
            angleDeviation = (random.nextDouble() * 6.0 - 3.0); // +/- 3 degrees
            deviationChangeTicks = 20 + random.nextInt(40);
        }
    }

    private void broadcastHeadYaw(FakeBotPlayer bot) {
        bot.getServerWorld().getChunkManager().sendToOtherNearbyPlayers(bot,
                new EntitySetHeadYawS2CPacket(bot, (byte) (bot.headYaw * 256.0f / 360.0f)));
    }
}
