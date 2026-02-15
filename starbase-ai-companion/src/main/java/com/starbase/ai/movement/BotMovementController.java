package com.starbase.ai.movement;

import com.starbase.ai.StarbaseAIMod;
import com.starbase.ai.bot.FakeBotPlayer;
import com.starbase.ai.config.BotConfig;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Random;

public class BotMovementController {

    private static final double GRAVITY = 0.08;
    private static final double WATER_GRAVITY = 0.02;
    private static final double JUMP_VELOCITY = 0.42;
    private static final double SWIM_UP_VELOCITY = 0.04;
    private static final double WATER_SPEED_MULTIPLIER = 0.5;
    private static final double TERMINAL_VELOCITY = -3.0;
    private static final double WATER_TERMINAL_VELOCITY = -0.4;

    private final BotPathfinder pathfinder;
    private final BotConfig config;
    private final Random random = new Random();

    // Current movement state
    private MovementGoal currentGoal;
    private List<BlockPos> currentPath;
    private int pathIndex;
    private BlockPos lastPathTarget;
    private int ticksSinceRepath;

    // Vertical movement
    private double yVelocity;

    // Humanization state
    private int pauseTicksRemaining;
    private int walkingTicks;
    private double angleDeviation;
    private int deviationChangeTicks;

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
        this.yVelocity = 0;
    }

    public void clearGoal() {
        this.currentGoal = null;
        this.currentPath = null;
        this.pathIndex = 0;
        this.walkingTicks = 0;
        this.pauseTicksRemaining = 0;
        this.yVelocity = 0;
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
     * Called after super.tick() to apply all bot movement (horizontal + vertical).
     * FakeBotPlayer.travel() is overridden to skip vanilla movement when this controller
     * is active, so we have full control here.
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
            // Still apply gravity/swimming while paused
            applyVerticalMovement(bot, false, false);
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
        if (currentPath == null) return true;
        if (pathIndex >= currentPath.size()) return true;
        if (ticksSinceRepath >= config.pathRecalcTicks) return true;
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

        // Try to open fence gates / doors when close to waypoint
        if (horizontalDist < 1.5) {
            ServerWorld world = bot.getServerWorld();
            MovementUtil.tryOpen(world, waypoint);
            MovementUtil.tryOpen(world, waypoint.up()); // upper half of doors
        }

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

        boolean inWater = bot.isTouchingWater();
        boolean needsJump = waypoint.getY() > bot.getBlockY() && !inWater;
        boolean needsSwimUp = waypoint.getY() >= bot.getBlockY() && inWater;

        // Apply vertical movement (gravity, jumping, swimming)
        applyVerticalMovement(bot, needsJump, needsSwimUp);

        // Calculate speed
        double distToGoal = getDistToGoalTarget(bot);
        boolean shouldSprint = distToGoal > 10.0 && walkingTicks > 5 && !inWater;
        double speed = shouldSprint ? config.sprintSpeed : config.walkSpeed;

        // Slow down in water
        if (inWater) {
            speed *= WATER_SPEED_MULTIPLIER;
        }

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

        // Apply full 3D movement with collision detection
        bot.move(MovementType.SELF, new Vec3d(moveX, yVelocity, moveZ));

        // Reset y velocity if we landed
        if (bot.isOnGround() && yVelocity < 0) {
            yVelocity = 0;
        }

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

    private void applyVerticalMovement(FakeBotPlayer bot, boolean needsJump, boolean needsSwimUp) {
        boolean inWater = bot.isTouchingWater();

        if (inWater) {
            // Swimming: gentle gravity and swim-up force
            if (needsSwimUp) {
                yVelocity = SWIM_UP_VELOCITY;
            } else {
                yVelocity -= WATER_GRAVITY;
            }
            yVelocity = Math.max(yVelocity, WATER_TERMINAL_VELOCITY);
            // Water drag
            yVelocity *= 0.8;
        } else if (needsJump && bot.isOnGround()) {
            // Jump from ground
            yVelocity = JUMP_VELOCITY;
        } else if (!bot.isOnGround()) {
            // Airborne: apply gravity
            yVelocity -= GRAVITY;
            yVelocity = Math.max(yVelocity, TERMINAL_VELOCITY);
        } else {
            // On ground, not jumping
            yVelocity = 0;
        }
    }

    private double getDistToGoalTarget(FakeBotPlayer bot) {
        if (currentPath == null || currentPath.isEmpty()) return 0;
        BlockPos lastWaypoint = currentPath.get(currentPath.size() - 1);
        return Math.sqrt(bot.getPos().squaredDistanceTo(
                lastWaypoint.getX() + 0.5, lastWaypoint.getY(), lastWaypoint.getZ() + 0.5));
    }

    private void onPathComplete() {
        walkingTicks = 0;
        if (currentGoal instanceof MovementGoal.GoToPosition
                || currentGoal instanceof MovementGoal.Flee) {
            clearGoal();
        }
    }

    private void updateAngleDeviation() {
        deviationChangeTicks--;
        if (deviationChangeTicks <= 0) {
            angleDeviation = (random.nextDouble() * 6.0 - 3.0);
            deviationChangeTicks = 20 + random.nextInt(40);
        }
    }

    private void broadcastHeadYaw(FakeBotPlayer bot) {
        bot.getServerWorld().getChunkManager().sendToOtherNearbyPlayers(bot,
                new EntitySetHeadYawS2CPacket(bot, (byte) (bot.headYaw * 256.0f / 360.0f)));
    }
}
