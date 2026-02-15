package com.starbase.ai.behavior;

import com.starbase.ai.StarbaseAIMod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BotBehaviorManager {

    private final List<Behavior> behaviors = new ArrayList<>();
    private Behavior activeBehavior;

    public void register(Behavior behavior) {
        behaviors.add(behavior);
        // Keep sorted by priority (lowest number = highest priority)
        behaviors.sort(Comparator.comparingInt(Behavior::priority));
    }

    public void tick(BotContext ctx) {
        // Check if a higher-priority behavior should preempt the current one
        for (Behavior behavior : behaviors) {
            if (behavior == activeBehavior) break;

            if (behavior.canStart(ctx)) {
                // Higher priority behavior wants to start
                if (activeBehavior != null) {
                    StarbaseAIMod.LOGGER.debug("Behavior {} preempted by {}",
                            activeBehavior.name(), behavior.name());
                    activeBehavior.stop(ctx);
                }
                activeBehavior = behavior;
                activeBehavior.start(ctx);
                activeBehavior.tick(ctx);
                return;
            }
        }

        // Continue active behavior or find a new one
        if (activeBehavior != null) {
            if (activeBehavior.shouldContinue(ctx)) {
                activeBehavior.tick(ctx);
                return;
            } else {
                StarbaseAIMod.LOGGER.debug("Behavior {} finished", activeBehavior.name());
                activeBehavior.stop(ctx);
                activeBehavior = null;
            }
        }

        // Find the highest-priority behavior that can start
        for (Behavior behavior : behaviors) {
            if (behavior.canStart(ctx)) {
                activeBehavior = behavior;
                StarbaseAIMod.LOGGER.debug("Starting behavior {}", behavior.name());
                activeBehavior.start(ctx);
                activeBehavior.tick(ctx);
                return;
            }
        }
    }

    public Behavior getActiveBehavior() {
        return activeBehavior;
    }

    public void stop(BotContext ctx) {
        if (activeBehavior != null) {
            activeBehavior.stop(ctx);
            activeBehavior = null;
        }
    }
}
