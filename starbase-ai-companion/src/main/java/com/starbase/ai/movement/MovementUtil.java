package com.starbase.ai.movement;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
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
     * Position is walkable: passable at feet level, passable at head level,
     * and solid block below (ground).
     */
    public static boolean isWalkable(ServerWorld world, BlockPos feetPos) {
        return isPassable(world, feetPos)
                && isPassable(world, feetPos.up())
                && isSolid(world, feetPos.down());
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
                || !world.getFluidState(pos).isEmpty() && world.getFluidState(pos).isStill();
    }

    /**
     * Position is safe to walk on (walkable and no hazards nearby at feet level).
     */
    public static boolean isSafeToWalk(ServerWorld world, BlockPos feetPos) {
        return isWalkable(world, feetPos) && !isDangerous(world, feetPos) && !isDangerous(world, feetPos.down());
    }
}
