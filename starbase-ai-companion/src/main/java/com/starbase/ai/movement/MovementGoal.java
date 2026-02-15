package com.starbase.ai.movement;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public sealed interface MovementGoal {

    record FollowPlayer(ServerPlayerEntity target, double minDist, double maxDist) implements MovementGoal {
        public FollowPlayer(ServerPlayerEntity target) {
            this(target, 3.0, 32.0);
        }
    }

    record GoToPosition(BlockPos target) implements MovementGoal {}

    record Wander(BlockPos center, int radius) implements MovementGoal {
        public Wander(BlockPos center) {
            this(center, 20);
        }
    }

    record Flee(BlockPos dangerSource, double safeDistance) implements MovementGoal {
        public Flee(BlockPos dangerSource) {
            this(dangerSource, 16.0);
        }
    }

    record StayHere() implements MovementGoal {}
}
