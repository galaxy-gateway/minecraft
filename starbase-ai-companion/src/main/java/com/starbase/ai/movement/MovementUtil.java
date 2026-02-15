package com.starbase.ai.movement;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.fluid.Fluids;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;

public final class MovementUtil {

    private MovementUtil() {}

    /**
     * Block has no collision shape (air, grass, flowers, etc.)
     */
    public static boolean isPassable(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getCollisionShape(world, pos).isEmpty();
    }

    /**
     * Block has a collision shape (solid ground, walls, etc.)
     */
    public static boolean isSolid(ServerWorld world, BlockPos pos) {
        return !isPassable(world, pos);
    }

    /**
     * Block is an openable barrier (fence gate or door), regardless of current state.
     */
    public static boolean isOpenable(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof FenceGateBlock
                || state.getBlock() instanceof DoorBlock;
    }

    /**
     * Block is passable for pathfinding: either truly passable or openable.
     * The bot will open gates/doors when it reaches them.
     */
    public static boolean isPathPassable(ServerWorld world, BlockPos pos) {
        return isPassable(world, pos) || isOpenable(world, pos);
    }

    /**
     * Block contains water fluid.
     */
    public static boolean isWater(ServerWorld world, BlockPos pos) {
        return world.getFluidState(pos).isOf(Fluids.WATER)
                || world.getFluidState(pos).isOf(Fluids.FLOWING_WATER);
    }

    /**
     * Position is walkable: passable at feet level, passable at head level,
     * and solid block below (ground).
     */
    public static boolean isWalkable(ServerWorld world, BlockPos feetPos) {
        return isPassable(world, feetPos)
                && isPassable(world, feetPos.up())
                && isSolid(world, feetPos.down());
    }

    /**
     * Position is walkable for pathfinding (includes openable blocks like gates/doors).
     */
    public static boolean isWalkableForPath(ServerWorld world, BlockPos feetPos) {
        return isPathPassable(world, feetPos)
                && isPathPassable(world, feetPos.up())
                && isSolid(world, feetPos.down());
    }

    /**
     * Position is swimmable: water at feet level and passable above (can surface).
     */
    public static boolean isSwimmable(ServerWorld world, BlockPos feetPos) {
        if (!isWater(world, feetPos)) return false;
        return isPassable(world, feetPos.up());
    }

    /**
     * Position is traversable by the bot (walkable on land or swimmable in water).
     */
    public static boolean isTraversable(ServerWorld world, BlockPos feetPos) {
        return isWalkable(world, feetPos) || isSwimmable(world, feetPos);
    }

    /**
     * Position is traversable for pathfinding (includes openable blocks).
     */
    public static boolean isTraversableForPath(ServerWorld world, BlockPos feetPos) {
        return isWalkableForPath(world, feetPos) || isSwimmable(world, feetPos);
    }

    /**
     * Block is a hazard (lava, fire, cactus, magma, berry bush).
     */
    public static boolean isDangerous(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isOf(Blocks.LAVA)
                || state.isOf(Blocks.FIRE)
                || state.isOf(Blocks.SOUL_FIRE)
                || state.isOf(Blocks.CACTUS)
                || state.isOf(Blocks.MAGMA_BLOCK)
                || state.isOf(Blocks.SWEET_BERRY_BUSH)
                || state.isOf(Blocks.POWDER_SNOW)
                || world.getFluidState(pos).isOf(Fluids.LAVA)
                || world.getFluidState(pos).isOf(Fluids.FLOWING_LAVA);
    }

    /**
     * Position is safe to walk on (walkable and no hazards at feet level).
     */
    public static boolean isSafeToWalk(ServerWorld world, BlockPos feetPos) {
        return isWalkable(world, feetPos) && !isDangerous(world, feetPos) && !isDangerous(world, feetPos.down());
    }

    /**
     * Try to open a fence gate or door at the given position.
     * Returns true if a block was opened.
     */
    public static boolean tryOpen(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof FenceGateBlock && state.contains(Properties.OPEN) && !state.get(Properties.OPEN)) {
            world.setBlockState(pos, state.with(Properties.OPEN, true), 3);
            return true;
        }
        if (state.getBlock() instanceof DoorBlock && state.contains(Properties.OPEN) && !state.get(Properties.OPEN)) {
            world.setBlockState(pos, state.with(Properties.OPEN, true), 3);
            return true;
        }
        return false;
    }
}
