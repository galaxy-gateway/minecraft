package com.starbase.ai.behavior;

public interface Behavior {

    /** Display name for logging. */
    String name();

    /** Priority: 1=critical (survival), 2=high (tasks), 3=medium, 4=low (idle). */
    int priority();

    /** Whether this behavior can start given the current context. */
    boolean canStart(BotContext ctx);

    /** Whether this behavior should continue running. */
    boolean shouldContinue(BotContext ctx);

    /** Called when this behavior becomes the active one. */
    void start(BotContext ctx);

    /** Called every tick while this behavior is active. */
    void tick(BotContext ctx);

    /** Called when this behavior is preempted or finishes. */
    void stop(BotContext ctx);
}
