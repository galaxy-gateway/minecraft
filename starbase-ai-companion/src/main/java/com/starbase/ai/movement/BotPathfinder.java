package com.starbase.ai.movement;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class BotPathfinder {

    private static final int MAX_ITERATIONS = 4000;

    // 8 directions: 4 cardinal + 4 diagonal
    private static final int[] DX = {1, -1, 0, 0, 1, -1, 1, -1};
    private static final int[] DZ = {0, 0, 1, -1, 1, 1, -1, -1};
    private static final double[] BASE_COST = {1.0, 1.0, 1.0, 1.0, 1.414, 1.414, 1.414, 1.414};

    private final int maxRange;

    public BotPathfinder(int maxRange) {
        this.maxRange = maxRange;
    }

    public List<BlockPos> findPath(ServerWorld world, BlockPos from, BlockPos to) {
        // Snap start to traversable ground level
        from = findTraversable(world, from);
        to = findTraversable(world, to);
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
                to = findTraversable(world, to);
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

            boolean currentInWater = MovementUtil.isSwimmable(world, current.pos);

            // Explore neighbors in 8 directions (4 cardinal + 4 diagonal)
            for (int i = 0; i < 8; i++) {
                int nx = current.pos.getX() + DX[i];
                int nz = current.pos.getZ() + DZ[i];
                int ny = current.pos.getY();
                boolean isDiagonal = i >= 4;

                // For diagonal movement, check both adjacent cardinal blocks
                // to prevent corner-cutting through walls
                if (isDiagonal) {
                    BlockPos adj1 = new BlockPos(current.pos.getX() + DX[i], ny, current.pos.getZ());
                    BlockPos adj2 = new BlockPos(current.pos.getX(), ny, current.pos.getZ() + DZ[i]);
                    if (!MovementUtil.isPathPassable(world, adj1) || !MovementUtil.isPathPassable(world, adj2)) {
                        continue;
                    }
                }

                BlockPos neighborPos = null;
                double moveCost = BASE_COST[i];

                // Try same level (walkable -- includes openable gates/doors)
                BlockPos sameLevel = new BlockPos(nx, ny, nz);
                if (MovementUtil.isWalkableForPath(world, sameLevel)) {
                    neighborPos = sameLevel;
                    // Add cost penalty if path goes through an openable block
                    if (MovementUtil.isOpenable(world, sameLevel)) {
                        moveCost += 0.5;
                    }
                }

                // Try step up (1 block) -- only from land, only cardinal
                if (neighborPos == null && !currentInWater && !isDiagonal) {
                    BlockPos stepUp = new BlockPos(nx, ny + 1, nz);
                    if (MovementUtil.isWalkableForPath(world, stepUp)
                            && MovementUtil.isPathPassable(world, current.pos.up().up())) {
                        neighborPos = stepUp;
                        moveCost = 1.5;
                    }
                }

                // Try step down (1 block) -- only cardinal
                if (neighborPos == null && !isDiagonal) {
                    BlockPos stepDown = new BlockPos(nx, ny - 1, nz);
                    if (MovementUtil.isWalkableForPath(world, stepDown)
                            && MovementUtil.isPathPassable(world, sameLevel)) {
                        neighborPos = stepDown;
                        moveCost = 1.2;
                    }
                }

                // Try swimming (same level water)
                if (neighborPos == null && MovementUtil.isSwimmable(world, sameLevel)) {
                    neighborPos = sameLevel;
                    moveCost = BASE_COST[i] * 2.0;
                }

                // Try entering water (step down into water) -- only cardinal
                if (neighborPos == null && !isDiagonal) {
                    BlockPos waterDown = new BlockPos(nx, ny - 1, nz);
                    if (MovementUtil.isSwimmable(world, waterDown)
                            && MovementUtil.isPathPassable(world, sameLevel)) {
                        neighborPos = waterDown;
                        moveCost = 2.0;
                    }
                }

                // Try exiting water (step up from water to land) -- only cardinal
                if (neighborPos == null && currentInWater && !isDiagonal) {
                    BlockPos stepUp = new BlockPos(nx, ny + 1, nz);
                    if (MovementUtil.isWalkableForPath(world, stepUp)) {
                        neighborPos = stepUp;
                        moveCost = 1.5;
                    }
                }

                if (neighborPos == null) continue;

                tryAddNeighbor(neighborPos, current, moveCost, to, allNodes, openSet, world);
            }

            // Vertical water movement: swim straight up/down
            if (currentInWater) {
                BlockPos swimUp = current.pos.up();
                if (MovementUtil.isSwimmable(world, swimUp)
                        || MovementUtil.isWalkableForPath(world, swimUp)) {
                    tryAddNeighbor(swimUp, current, 1.5, to, allNodes, openSet, world);
                }

                BlockPos swimDown = current.pos.down();
                if (MovementUtil.isSwimmable(world, swimDown)) {
                    tryAddNeighbor(swimDown, current, 1.2, to, allNodes, openSet, world);
                }
            }
        }

        // Return partial path to closest point found
        if (!bestNode.pos.equals(from)) {
            return reconstructPath(bestNode);
        }
        return List.of();
    }

    private void tryAddNeighbor(BlockPos neighborPos, Node current, double moveCost,
                                BlockPos to, Map<BlockPos, Node> allNodes,
                                PriorityQueue<Node> openSet, ServerWorld world) {
        if (MovementUtil.isDangerous(world, neighborPos)
                || MovementUtil.isDangerous(world, neighborPos.down())) {
            return;
        }

        double newG = current.gCost + moveCost;
        Node existing = allNodes.get(neighborPos);
        if (existing != null && newG >= existing.gCost) return;

        Node neighborNode = new Node(neighborPos, newG, newG + heuristic(neighborPos, to), current);
        allNodes.put(neighborPos, neighborNode);
        openSet.add(neighborNode);
    }

    private BlockPos findTraversable(ServerWorld world, BlockPos pos) {
        for (int dy = 0; dy <= 5; dy++) {
            BlockPos up = pos.up(dy);
            if (MovementUtil.isTraversableForPath(world, up)) return up;
            if (dy > 0) {
                BlockPos down = pos.down(dy);
                if (MovementUtil.isTraversableForPath(world, down)) return down;
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
