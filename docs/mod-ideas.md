# Custom Fabric Mod Ideas for Starbase Server

This document outlines potential custom Fabric mods that could enhance the Starbase Minecraft server experience. These ideas are designed to complement the existing setup: Fabric 1.21.1, CC: Tweaked, Handcrafted, and the custom Rust web admin panel.

## Integration Mods

### 1. CC: Tweaked Integration Mod

Extend ComputerCraft with custom peripherals that integrate with server infrastructure:

- **Starbase Terminal** - A peripheral block that connects to the web admin API, letting in-game computers query server status, player count, or trigger backups via Lua scripts
- **Redstone Network Bridge** - Allow computers to control redstone signals across long distances or even dimensions
- **Database Peripheral** - Persistent key-value storage that survives computer reboots

### 2. Web Admin Commands Bridge

A lightweight mod that enhances the Rust web admin panel:

- Exposes a local HTTP endpoint for the admin panel to send commands
- Returns structured JSON responses (player list, TPS, memory usage, world stats)
- Allows the web panel to execute whitelisted commands without RCON
- Real-time event streaming via Server-Sent Events

### 3. Player Activity Tracker

Logs player activity to JSON/SQLite for web admin integration:

- Time played per session and total
- Blocks mined/placed statistics
- Deaths, kills, and achievements
- Location history and travel distance
- Displayable on the web dashboard with charts

## Gameplay Mods

### 4. Custom Starbase Items

Themed utility items for the server:

- **Starbase Communicator** - Handheld item that sends messages to Discord or the web panel
- **Teleport Beacon** - Named location teleporter with configurable cooldowns
- **Portable Backup Trigger** - Op-only item that safely triggers world backup
- **Player Tracker Compass** - Points toward other online players (with consent)

### 5. Lua Script Sharing System

Enhance the CC: Tweaked experience with shared programs:

- Syncs approved Lua programs from a server-side folder
- Players can browse and download scripts to their in-game computers
- Version control for scripts with update notifications
- Managed via the web admin panel

### 6. Starbase Economy System

A simple server economy with programmable integration:

- Virtual currency earned through gameplay activities
- Player-to-player trading with transaction logs
- Shop blocks that can be programmed via CC: Tweaked
- Economy stats viewable on web admin dashboard
- Anti-inflation controls and admin tools

### 7. Custom Achievements & Quests

Server-specific progression system:

- Define custom achievements in JSON/TOML config files
- Quest chains with rewards (items, currency, titles)
- Progress tracking visible in web admin
- Announcement system for achievement completion
- Leaderboards for competitive achievements

### 8. Land Claim & Protection

Lightweight territory management:

- Claim chunks using in-game items or commands
- Permission levels: owner, trusted, visitor
- Integration with CC: Tweaked for programmable access control
- Visual claim boundaries with particle effects
- Web admin interface for managing disputes

### 9. Cross-Dimension Communication

Enhanced player communication tools:

- **Quantum Radio** - Voice/text chat that works across dimensions
- **Mailbox Block** - Async message system for offline players
- **Bulletin Board** - Server-wide announcement system
- **Coordinate Sharing** - Easily share locations with teammates
- All messages logged and viewable in web admin

### 10. Server Events Framework

Automated and manual event system:

- Scheduled events (double XP weekends, mob hunts, build competitions)
- Event zones with modified game rules
- Participation tracking and rewards
- Countdown timers visible in-game
- Event creation and management via web admin
- Integration with CC: Tweaked for programmable event triggers

## Implementation Priority

| Mod | Complexity | Value | Recommended Order |
|-----|------------|-------|-------------------|
| Web Admin Commands Bridge | Low | High | 1st |
| Player Activity Tracker | Low | High | 2nd |
| CC: Tweaked Integration | Medium | High | 3rd |
| Custom Starbase Items | Medium | Medium | 4th |
| Lua Script Sharing | Medium | High | 5th |
| Server Events Framework | High | High | 6th |
| Starbase Economy | High | Medium | 7th |
| Custom Achievements | Medium | Medium | 8th |
| Cross-Dimension Comms | Medium | Low | 9th |
| Land Claim & Protection | High | Medium | 10th |

## Development Resources

- [Fabric Documentation](https://fabricmc.net/wiki/)
- [Fabric API GitHub](https://github.com/FabricMC/fabric)
- [CC: Tweaked API Docs](https://tweaked.cc/)
- [Minecraft Protocol Wiki](https://wiki.vg/Protocol)
