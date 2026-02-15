package com.starbase.ai.config;

public class BotConfig {
    public String botName = "ARIA";
    public double spawnX = 0;
    public double spawnY = 64;
    public double spawnZ = 0;
    public boolean lookAtNearestPlayer = true;
    public int respawnDelayTicks = 60; // 3 seconds at 20 TPS

    // Movement
    public double walkSpeed = 0.2;
    public double sprintSpeed = 0.26;
    public double followDistance = 3.0;
    public int pathRecalcTicks = 20;
    public int pathMaxRange = 64;
}
