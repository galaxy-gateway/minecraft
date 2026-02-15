package com.starbase.ai.ai;

import com.starbase.ai.behavior.EnvironmentScanner;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameContextCollector {

    public static String collect(ServerPlayerEntity player, ServerPlayerEntity bot) {
        ServerWorld world = player.getServerWorld();
        StringBuilder ctx = new StringBuilder();

        // Time
        long time = world.getTimeOfDay() % 24000;
        String timeStr = time < 12000 ? "Day" : "Night";
        ctx.append("Time: ").append(timeStr).append(" (").append(time).append(" ticks)\n");

        // Weather
        String weather = world.isThundering() ? "Thunder" : world.isRaining() ? "Rain" : "Clear";
        ctx.append("Weather: ").append(weather).append("\n");

        // Player info
        RegistryEntry<Biome> biome = world.getBiome(player.getBlockPos());
        String biomeName = biome.getKey().map(k -> k.getValue().getPath()).orElse("unknown");
        ctx.append(String.format("Player \"%s\" at (%.0f, %.0f, %.0f) in %s biome\n",
                player.getName().getString(),
                player.getX(), player.getY(), player.getZ(),
                biomeName));

        // Nearby players
        List<String> nearby = new ArrayList<>();
        for (ServerPlayerEntity other : world.getPlayers()) {
            if (other == player || other == bot) continue;
            double dist = player.distanceTo(other);
            if (dist < 100) {
                nearby.add(String.format("%s (%.0f blocks away)", other.getName().getString(), dist));
            }
        }
        if (!nearby.isEmpty()) {
            ctx.append("Nearby players: ").append(String.join(", ", nearby)).append("\n");
        }

        // Bot info
        if (bot != null) {
            ctx.append(String.format("Bot at (%.0f, %.0f, %.0f), health: %.0f/20\n",
                    bot.getX(), bot.getY(), bot.getZ(), bot.getHealth()));
        }

        return ctx.toString();
    }

    public static String collect(ServerPlayerEntity player, ServerPlayerEntity bot,
                                 EnvironmentScanner.Snapshot env, String currentActivity) {
        String base = collect(player, bot);
        if (env == null) return base;

        StringBuilder ctx = new StringBuilder(base);

        // Current activity
        if (currentActivity != null && !currentActivity.isEmpty()) {
            ctx.append("Bot activity: ").append(currentActivity).append("\n");
        }

        // Hostile mobs
        if (!env.hostileMobs().isEmpty()) {
            List<String> threats = new ArrayList<>();
            for (EnvironmentScanner.MobInfo mob : env.hostileMobs()) {
                threats.add(String.format("%s (%.0f blocks)", mob.type(), mob.distance()));
            }
            ctx.append("Hostile mobs: ").append(String.join(", ", threats)).append("\n");
        }

        // Passive mobs
        if (!env.passiveMobs().isEmpty()) {
            List<String> animals = new ArrayList<>();
            for (EnvironmentScanner.MobInfo mob : env.passiveMobs()) {
                animals.add(String.format("%s (%.0f blocks)", mob.type(), mob.distance()));
            }
            ctx.append("Animals: ").append(String.join(", ", animals)).append("\n");
        }

        // Notable blocks (deduplicated, limit 5)
        if (!env.notableBlocks().isEmpty()) {
            Set<String> seen = new HashSet<>();
            List<String> blocks = new ArrayList<>();
            for (EnvironmentScanner.BlockInfo block : env.notableBlocks()) {
                if (seen.add(block.type()) && blocks.size() < 5) {
                    blocks.add(block.type());
                }
            }
            ctx.append("Notable blocks nearby: ").append(String.join(", ", blocks)).append("\n");
        }

        // Light and cover
        ctx.append("Light level: ").append(env.lightLevel()).append("\n");
        ctx.append("Location: ").append(env.hasOverheadCover() ? "Inside/underground" : "Outside").append("\n");

        return ctx.toString();
    }
}
