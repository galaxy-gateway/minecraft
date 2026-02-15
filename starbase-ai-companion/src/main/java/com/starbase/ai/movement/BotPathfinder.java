package com.starbase.ai.movement;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class BotPathfinder {

    private static final int MAX_ITERATIONS = 1000;
    private static final int[] DX = {1, -1, 0, 0};
    private static final int[] DZ = {0, 0, 1, -1};

    private final int maxRange;

    public BotPathfinder(int maxRange) {
        this.maxRange = maxRange;
    }

    public List<BlockPos> findPath(ServerWorld world, BlockPos from, BlockPos to) {
        // Snap start to walkable ground level
        from = findGround(world, from);
        to = findGround(world, to);
        if (from == null || to == null) return List.of();

        // If target is too far, aim for a closer point along the direction
        if (from.getManhattanDistance(to) > maxRange) {
            double dx = to.getX() - from.getX();
            double dz = to.getZ() - from.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0) {
                double scale = (maxRange * 0.75) / dist;
                to = new BlockPos(
                        from.getX() + (int) (dx * scale),
                        to.getY(),
                        from.getZ() + (int) (dz * scale)
                );
                to = findGround(world, to);
                if (to == null) return List.of();
            }
        }

        // A* search
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<BlockPos, Node> allNodes = new HashMap<>();

        Node startNode = new Node(from, 0, heuristic(from, to), null);
        openSet.add(startNode);
        allNodes.put(from, startNode);

        int iterations = 0;
        Node bestNode = startNode;

        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;
            Node current = openSet.poll();

            if (current.pos.equals(to)) {
                return reconstructPath(current);
            }

            // Track the closest node to the target for partial paths
            if (heuristic(current.pos, to) < heuristic(bestNode.pos, to)) {
                bestNode = current;
            }

            // Explore neighbors in 4 cardinal directions
            for (int i = 0; i < 4; i++) {
                int nx = current.pos.getX() + DX[i];
                int nz = current.pos.getZ() + DZ[i];
                int ny = current.pos.getY();

                BlockPos neighborPos = null;
                double moveCost = 1.0;

                // Try same level
                BlockPos sameLevel = new BlockPos(nx, ny, nz);
                if (MovementUtil.isWalkable(world, sameLevel)) {
                    neighborPos = sameLevel;
                }

                // Try step up (1 block)
                if (neighborPos == null) {
                    BlockPos stepUp = new BlockPos(nx, ny + 1, nz);
                    if (MovementUtil.isWalkable(world, stepUp)
                            && MovementUtil.isPassable(world, current.pos.up().up())) {
                        // Need head clearance above current pos to jump
                        neighborPos = stepUp;
                        moveCost = 1.5; // Penalize jumping
                    }
                }

                // Try step down (1 block)
                if (neighborPos == null) {
                    BlockPos stepDown = new BlockPos(nx, ny - 1, nz);
                    if (MovementUtil.isWalkable(world, stepDown)
                            && MovementUtil.isPassable(world, sameLevel)) {
                        neighborPos = stepDown;
                        moveCost = 1.2; // Slight penalty for stepping down
                    }
                }

                if (neighborPos == null) continue;

                // Skip dangerous blocks
                if (MovementUtil.isDangerous(world, neighborPos)
                        || MovementUtil.isDangerous(world, neighborPos.down())) {
                    continue;
                }

                double newG = current.gCost + moveCost;
                Node existing = allNodes.get(neighborPos);
                if (existing != null && newG >= existing.gCost) continue;

                Node neighborNode = new Node(neighborPos, newG, newG + heuristic(neighborPos, to), current);
                allNodes.put(neighborPos, neighborNode);
                openSet.add(neighborNode);
            }
        }

        // Return partial path to closest point found
        if (!bestNode.pos.equals(from)) {
            return reconstructPath(bestNode);
        }
        return List.of();
    }

    private BlockPos findGround(ServerWorld world, BlockPos pos) {
        // Search up and down a few blocks to find a walkable position
        for (int dy = 0; dy <= 5; dy++) {
            BlockPos up = pos.up(dy);
            if (MovementUtil.isWalkable(world, up)) return up;
            if (dy > 0) {
                BlockPos down = pos.down(dy);
                if (MovementUtil.isWalkable(world, down)) return down;
            }
        }
        return null;
    }

    private double heuristic(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private List<BlockPos> reconstructPath(Node end) {
        List<BlockPos> path = new ArrayList<>();
        Node current = end;
        while (current != null) {
            path.add(current.pos);
            current = current.parent;
        }
        Collections.reverse(path);
        // Remove the starting position (bot is already there)
        if (!path.isEmpty()) path.remove(0);
        return path;
    }

    private static class Node implements Comparable<Node> {
        final BlockPos pos;
        final double gCost;
        final double fCost;
        final Node parent;

        Node(BlockPos pos, double gCost, double fCost, Node parent) {
            this.pos = pos;
            this.gCost = gCost;
            this.fCost = fCost;
            this.parent = parent;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.fCost, other.fCost);
        }
    }
}
