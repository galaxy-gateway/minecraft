package com.starbase.ai.behavior;

import com.starbase.ai.bot.FakeBotPlayer;
import com.starbase.ai.movement.MovementUtil;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.List;

public class EnvironmentScanner {

    private static final int SCAN_INTERVAL = 20; // ticks
    private static final double HOSTILE_SCAN_RANGE = 16.0;
    private static final double PASSIVE_SCAN_RANGE = 16.0;
    private static final int BLOCK_SCAN_RANGE = 8;

    private int tickCounter;
    private Snapshot lastSnapshot;

    public void maybeScan(FakeBotPlayer bot) {
        tickCounter++;
        if (tickCounter >= SCAN_INTERVAL || lastSnapshot == null) {
            tickCounter = 0;
            lastSnapshot = scan(bot);
        }
    }

    public Snapshot getSnapshot() {
        return lastSnapshot;
    }

    private Snapshot scan(FakeBotPlayer bot) {
        ServerWorld world = bot.getServerWorld();
        BlockPos botPos = bot.getBlockPos();

        // Scan hostile mobs
        List<MobInfo> hostiles = new ArrayList<>();
        Box hostileBox = bot.getBoundingBox().expand(HOSTILE_SCAN_RANGE);
        for (Entity entity : world.getOtherEntities(bot, hostileBox)) {
            if (entity instanceof HostileEntity hostile) {
                double dist = bot.distanceTo(hostile);
                if (dist <= HOSTILE_SCAN_RANGE) {
                    hostiles.add(new MobInfo(
                            hostile.getType().getName().getString(),
                            hostile.getBlockPos(),
                            dist
                    ));
                }
            }
        }

        // Scan passive mobs
        List<MobInfo> passives = new ArrayList<>();
        Box passiveBox = bot.getBoundingBox().expand(PASSIVE_SCAN_RANGE);
        for (Entity entity : world.getOtherEntities(bot, passiveBox)) {
            if (entity instanceof AnimalEntity animal) {
                double dist = bot.distanceTo(animal);
                if (dist <= PASSIVE_SCAN_RANGE) {
                    passives.add(new MobInfo(
                            animal.getType().getName().getString(),
                            animal.getBlockPos(),
                            dist
                    ));
                }
            }
        }

        // Scan nearby players
        List<PlayerInfo> nearbyPlayers = new ArrayList<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player == bot || player instanceof FakeBotPlayer) continue;
            double dist = bot.distanceTo(player);
            if (dist < 48.0) {
                nearbyPlayers.add(new PlayerInfo(
                        player.getName().getString(),
                        player,
                        dist
                ));
            }
        }

        // Check overhead cover
        boolean hasOverheadCover = false;
        for (int y = botPos.getY() + 2; y < botPos.getY() + 10 && y < world.getTopY(); y++) {
            BlockState above = world.getBlockState(new BlockPos(botPos.getX(), y, botPos.getZ()));
            if (!above.getCollisionShape(world, new BlockPos(botPos.getX(), y, botPos.getZ())).isEmpty()) {
                hasOverheadCover = true;
                break;
            }
        }

        // Light level at bot position
        int lightLevel = world.getLightLevel(botPos);

        // Check for nearby hazards
        boolean nearHazard = false;
        BlockPos nearestHazard = null;
        double nearestHazardDist = Double.MAX_VALUE;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos checkPos = botPos.add(dx, dy, dz);
                    if (MovementUtil.isDangerous(world, checkPos)) {
                        nearHazard = true;
                        double dist = botPos.getSquaredDistance(checkPos);
                        if (dist < nearestHazardDist) {
                            nearestHazardDist = dist;
                            nearestHazard = checkPos;
                        }
                    }
                }
            }
        }

        // Notable blocks
        List<BlockInfo> notableBlocks = new ArrayList<>();
        for (int dx = -BLOCK_SCAN_RANGE; dx <= BLOCK_SCAN_RANGE; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -BLOCK_SCAN_RANGE; dz <= BLOCK_SCAN_RANGE; dz++) {
                    BlockPos checkPos = botPos.add(dx, dy, dz);
                    BlockState state = world.getBlockState(checkPos);
                    String notable = getNotableBlockName(state);
                    if (notable != null) {
                        notableBlocks.add(new BlockInfo(notable, checkPos));
                    }
                }
            }
        }

        // Time of day
        long timeOfDay = world.getTimeOfDay() % 24000;
        boolean isNight = timeOfDay >= 13000 && timeOfDay < 23000;

        return new Snapshot(
                hostiles, passives, nearbyPlayers,
                hasOverheadCover, lightLevel,
                nearHazard, nearestHazard,
                notableBlocks, isNight, timeOfDay
        );
    }

    private String getNotableBlockName(BlockState state) {
        if (state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST)) return "Chest";
        if (state.isOf(Blocks.CRAFTING_TABLE)) return "Crafting Table";
        if (state.isOf(Blocks.FURNACE)) return "Furnace";
        if (state.isOf(Blocks.DIAMOND_ORE) || state.isOf(Blocks.DEEPSLATE_DIAMOND_ORE)) return "Diamond Ore";
        if (state.isOf(Blocks.IRON_ORE) || state.isOf(Blocks.DEEPSLATE_IRON_ORE)) return "Iron Ore";
        if (state.isOf(Blocks.GOLD_ORE) || state.isOf(Blocks.DEEPSLATE_GOLD_ORE)) return "Gold Ore";
        if (state.isOf(Blocks.ENCHANTING_TABLE)) return "Enchanting Table";
        if (state.isOf(Blocks.ANVIL)) return "Anvil";
        if (state.getBlock() instanceof BedBlock) return "Bed";
        return null;
    }

    // Data records
    public record MobInfo(String type, BlockPos pos, double distance) {}
    public record PlayerInfo(String name, ServerPlayerEntity player, double distance) {}
    public record BlockInfo(String type, BlockPos pos) {}

    public record Snapshot(
            List<MobInfo> hostileMobs,
            List<MobInfo> passiveMobs,
            List<PlayerInfo> nearbyPlayers,
            boolean hasOverheadCover,
            int lightLevel,
            boolean nearHazard,
            BlockPos nearestHazardPos,
            List<BlockInfo> notableBlocks,
            boolean isNight,
            long timeOfDay
    ) {
        public MobInfo closestHostile() {
            return hostileMobs.stream()
                    .min((a, b) -> Double.compare(a.distance(), b.distance()))
                    .orElse(null);
        }

        public PlayerInfo closestPlayer() {
            return nearbyPlayers.stream()
                    .min((a, b) -> Double.compare(a.distance(), b.distance()))
                    .orElse(null);
        }

        public boolean hasHostilesNearby(double range) {
            return hostileMobs.stream().anyMatch(m -> m.distance() <= range);
        }
    }
}
