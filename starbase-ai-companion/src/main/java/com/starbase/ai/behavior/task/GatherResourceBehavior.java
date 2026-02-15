package com.starbase.ai.behavior.task;

import com.starbase.ai.StarbaseAIMod;
import com.starbase.ai.action.BotBlockBreaker;
import com.starbase.ai.behavior.Behavior;
import com.starbase.ai.behavior.BotContext;
import com.starbase.ai.chat.ChatResponder;
import com.starbase.ai.movement.MovementGoal;
import com.starbase.ai.movement.MovementUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class GatherResourceBehavior implements Behavior {

    private static final int SCAN_RADIUS = 32;
    private static final int SCAN_Y_RANGE = 8;
    private static final double MINING_REACH = 4.0;
    private static final int RESCAN_INTERVAL = 60; // ticks between rescans when walking

    private enum State { IDLE, SCANNING, WALKING, MINING, DONE }

    private final BotBlockBreaker blockBreaker = new BotBlockBreaker();

    // Task parameters
    private Block targetBlock;
    private int targetCount;
    private String requesterName;
    private boolean active;

    // Task state
    private State state = State.IDLE;
    private BlockPos currentMineTarget;
    private int initialItemCount;
    private int ticksSinceRescan;
    private int noBlockFoundTicks;

    // Block name aliases for user-friendly commands
    private static final Map<String, String> ALIASES = new HashMap<>();
    static {
        ALIASES.put("wood", "oak_log");
        ALIASES.put("log", "oak_log");
        ALIASES.put("logs", "oak_log");
        ALIASES.put("cobble", "cobblestone");
        ALIASES.put("grass", "grass_block");
        ALIASES.put("planks", "oak_planks");
        ALIASES.put("rock", "stone");
        ALIASES.put("rocks", "stone");
        ALIASES.put("gravel", "gravel");
    }

    @Override
    public String name() {
        return "GatherResource";
    }

    @Override
    public int priority() {
        return 2; // Same as follow -- player-initiated task
    }

    @Override
    public boolean canStart(BotContext ctx) {
        return active && targetBlock != null;
    }

    @Override
    public boolean shouldContinue(BotContext ctx) {
        if (!active || targetBlock == null) return false;
        if (state == State.DONE) return false;
        return getCollectedCount(ctx) < targetCount;
    }

    @Override
    public void start(BotContext ctx) {
        state = State.SCANNING;
        initialItemCount = countItemsOfBlock(ctx, targetBlock);
        ticksSinceRescan = 0;
        noBlockFoundTicks = 0;
        StarbaseAIMod.LOGGER.info("GatherResource started: {} x{}", getBlockName(targetBlock), targetCount);
    }

    @Override
    public void tick(BotContext ctx) {
        int collected = getCollectedCount(ctx);
        if (collected >= targetCount) {
            reportDone(ctx, collected);
            state = State.DONE;
            return;
        }

        switch (state) {
            case SCANNING -> tickScanning(ctx);
            case WALKING -> tickWalking(ctx);
            case MINING -> tickMining(ctx);
            default -> {}
        }
    }

    @Override
    public void stop(BotContext ctx) {
        blockBreaker.cancel(ctx.bot());
        ctx.movement().clearGoal();
        if (state != State.DONE && active) {
            int collected = getCollectedCount(ctx);
            if (collected > 0) {
                reportProgress(ctx, collected, "Stopping gather.");
            }
        }
        state = State.IDLE;
        active = false;
        targetBlock = null;
        currentMineTarget = null;
    }

    // --- State machine ticks ---

    private void tickScanning(BotContext ctx) {
        BlockPos nearest = findNearestBlock(ctx.world(), ctx.bot().getBlockPos(), targetBlock);
        if (nearest == null) {
            noBlockFoundTicks += 20;
            if (noBlockFoundTicks > 100) {
                reportCantFind(ctx);
                state = State.DONE;
            }
            return;
        }

        noBlockFoundTicks = 0;
        currentMineTarget = nearest;

        double dist = Math.sqrt(ctx.bot().getBlockPos().getSquaredDistance(nearest));
        if (dist <= MINING_REACH) {
            // Close enough to mine directly
            startMining(ctx);
        } else {
            // Need to walk there first
            ctx.movement().setGoal(new MovementGoal.GoToPosition(nearest));
            state = State.WALKING;
            ticksSinceRescan = 0;
        }
    }

    private void tickWalking(BotContext ctx) {
        if (currentMineTarget == null) {
            state = State.SCANNING;
            return;
        }

        // Check if target block was already broken
        if (ctx.world().getBlockState(currentMineTarget).isAir()) {
            ctx.movement().clearGoal();
            state = State.SCANNING;
            return;
        }

        double dist = Math.sqrt(ctx.bot().getBlockPos().getSquaredDistance(currentMineTarget));
        if (dist <= MINING_REACH) {
            ctx.movement().clearGoal();
            startMining(ctx);
            return;
        }

        // Periodically rescan for closer blocks
        ticksSinceRescan++;
        if (ticksSinceRescan > RESCAN_INTERVAL) {
            ticksSinceRescan = 0;
            BlockPos closer = findNearestBlock(ctx.world(), ctx.bot().getBlockPos(), targetBlock);
            if (closer != null && !closer.equals(currentMineTarget)) {
                double closerDist = Math.sqrt(ctx.bot().getBlockPos().getSquaredDistance(closer));
                if (closerDist < dist - 3.0) {
                    currentMineTarget = closer;
                    ctx.movement().setGoal(new MovementGoal.GoToPosition(closer));
                }
            }
        }

        // If movement controller finished its path but we're still not close enough
        if (ctx.movement().hasReachedGoal()) {
            // Try to get closer or scan for a new target
            state = State.SCANNING;
        }
    }

    private void tickMining(BotContext ctx) {
        if (currentMineTarget == null || !blockBreaker.isBreaking()) {
            state = State.SCANNING;
            return;
        }

        boolean broken = blockBreaker.tick(ctx.bot());
        if (broken) {
            currentMineTarget = null;
            // Brief pause then scan for next block
            state = State.SCANNING;

            // Progress report every 5 blocks
            int collected = getCollectedCount(ctx);
            if (collected > 0 && collected % 5 == 0 && collected < targetCount) {
                reportProgress(ctx, collected, null);
            }
        }
    }

    private void startMining(BotContext ctx) {
        if (currentMineTarget == null) {
            state = State.SCANNING;
            return;
        }

        boolean started = blockBreaker.startBreaking(ctx.bot(), currentMineTarget);
        if (started) {
            state = State.MINING;
        } else {
            // Block is unbreakable or already gone
            currentMineTarget = null;
            state = State.SCANNING;
        }
    }

    // --- Public API for ChatListener ---

    /**
     * Start gathering a resource.
     * @param block the block type to mine
     * @param count how many to collect
     * @param playerName who requested it (for chat responses)
     */
    public void gather(Block block, int count, String playerName) {
        this.targetBlock = block;
        this.targetCount = count;
        this.requesterName = playerName;
        this.active = true;
        this.state = State.IDLE; // Will transition to SCANNING on start()
        this.noBlockFoundTicks = 0;
    }

    public void stopGathering() {
        this.active = false;
    }

    public boolean isGathering() {
        return active && state != State.IDLE && state != State.DONE;
    }

    // --- Block scanning ---

    private BlockPos findNearestBlock(ServerWorld world, BlockPos center, Block block) {
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dy = -SCAN_Y_RANGE; dy <= SCAN_Y_RANGE; dy++) {
                for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    BlockState state = world.getBlockState(pos);
                    if (state.isOf(block)) {
                        // Prefer blocks we can actually reach (adjacent to air)
                        if (!hasAccessibleFace(world, pos)) continue;
                        double dist = center.getSquaredDistance(pos);
                        if (dist < nearestDist) {
                            nearestDist = dist;
                            nearest = pos;
                        }
                    }
                }
            }
        }
        return nearest;
    }

    private boolean hasAccessibleFace(ServerWorld world, BlockPos pos) {
        return MovementUtil.isPassable(world, pos.up())
                || MovementUtil.isPassable(world, pos.down())
                || MovementUtil.isPassable(world, pos.north())
                || MovementUtil.isPassable(world, pos.south())
                || MovementUtil.isPassable(world, pos.east())
                || MovementUtil.isPassable(world, pos.west());
    }

    // --- Inventory counting ---

    private int getCollectedCount(BotContext ctx) {
        return countItemsOfBlock(ctx, targetBlock) - initialItemCount;
    }

    private int countItemsOfBlock(BotContext ctx, Block block) {
        Item targetItem = block.asItem();
        int count = 0;
        for (int i = 0; i < ctx.bot().getInventory().size(); i++) {
            ItemStack stack = ctx.bot().getInventory().getStack(i);
            if (stack.getItem() == targetItem) {
                count += stack.getCount();
            }
        }
        return count;
    }

    // --- Chat reporting ---

    private void reportDone(BotContext ctx, int collected) {
        String blockName = getBlockName(targetBlock);
        String msg = "Done! I collected " + collected + " " + blockName + ".";
        sendChat(ctx, msg);
    }

    private void reportProgress(BotContext ctx, int collected, String extra) {
        String blockName = getBlockName(targetBlock);
        String msg = "Got " + collected + "/" + targetCount + " " + blockName + " so far.";
        if (extra != null) msg += " " + extra;
        sendChat(ctx, msg);
    }

    private void reportCantFind(BotContext ctx) {
        String blockName = getBlockName(targetBlock);
        sendChat(ctx, "I can't find any more " + blockName + " nearby.");
    }

    private void sendChat(BotContext ctx, String message) {
        String botName = StarbaseAIMod.CONFIG.bot().botName;
        ctx.server().execute(() -> ChatResponder.sendBotMessage(ctx.server(), botName, message));
    }

    // --- Block name resolution ---

    /**
     * Resolve a user-provided block name to a Block instance.
     * Handles spaces→underscores, aliases, and registry lookup.
     */
    public static Block resolveBlockName(String name) {
        String cleaned = name.toLowerCase().trim().replace(" ", "_");

        // Check aliases first
        if (ALIASES.containsKey(cleaned)) {
            cleaned = ALIASES.get(cleaned);
        }

        // Try minecraft namespace
        Identifier id = Identifier.of("minecraft", cleaned);
        Block block = Registries.BLOCK.get(id);

        // Registries.BLOCK.get() returns AIR for unknown blocks
        if (block != net.minecraft.block.Blocks.AIR) {
            return block;
        }

        // Try without namespace (might already include it)
        try {
            Identifier fullId = Identifier.of(cleaned);
            block = Registries.BLOCK.get(fullId);
            if (block != net.minecraft.block.Blocks.AIR) {
                return block;
            }
        } catch (Exception ignored) {}

        return null;
    }

    private static String getBlockName(Block block) {
        Identifier id = Registries.BLOCK.getId(block);
        return id.getPath().replace("_", " ");
    }
}
