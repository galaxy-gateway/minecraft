# Minecraft Server

Minecraft 1.21.11 server with systemd management, playit.gg tunnel for external access, and optional Fabric mod support.

**Server Address:** `starbase.minecraft.party`

## Quick Start

```bash
make help      # Show available commands
make start     # Start the server and tunnel
make stop      # Stop the server and tunnel
make status    # Check server and tunnel status
make console   # Attach to server console
```

## Fabric Mod Loader Setup

Fabric is a lightweight mod loader for Minecraft that allows you to run server-side mods.

### 1. Download the Fabric Installer

```bash
wget https://maven.fabricmc.net/net/fabricmc/fabric-installer/1.1.1/fabric-installer-1.1.1.jar
```

### 2. Install Fabric Server

Run the installer to set up Fabric for your Minecraft version:

```bash
java -jar fabric-installer-1.1.1.jar server -mcversion 1.21.1 -downloadMinecraft
```

This will:
- Download the Fabric server launcher (`fabric-server-launch.jar`)
- Download required Fabric libraries to the `libraries/` directory
- Create `server.jar` pointing to the Fabric launcher

**Options:**
- `-mcversion <version>` - Specify Minecraft version (e.g., `1.21.1`, `1.20.4`)
- `-downloadMinecraft` - Automatically download the vanilla server jar
- `-dir <path>` - Install to a specific directory (default: current directory)

### 3. Install the Fabric API

Most mods require the Fabric API. Download it from:
- https://modrinth.com/mod/fabric-api
- https://www.curseforge.com/minecraft/mc-mods/fabric-api

Place the downloaded `.jar` file in the `mods/` directory:

```bash
mkdir -p mods
# Move your downloaded fabric-api jar to the mods folder
mv fabric-api-*.jar mods/
```

### 4. Adding Mods

1. Download Fabric-compatible mods from [Modrinth](https://modrinth.com) or [CurseForge](https://www.curseforge.com)
2. Ensure the mod version matches your Minecraft version
3. Place the `.jar` files in the `mods/` directory

```bash
# Example structure
mods/
├── fabric-api-0.100.0+1.21.1.jar
├── lithium-mc1.21.1-0.12.0.jar
└── another-mod.jar
```

### 5. Start the Server

Use the existing management commands:

```bash
make start    # Start with Fabric and mods loaded
make logs     # View server logs to verify mods loaded
```

## Popular Server-Side Mods

| Mod | Description |
|-----|-------------|
| [Lithium](https://modrinth.com/mod/lithium) | Performance optimization |
| [Chunky](https://modrinth.com/mod/chunky) | Pre-generate chunks |
| [Ledger](https://modrinth.com/mod/ledger) | Block logging/rollback |
| [FabricProxy-Lite](https://modrinth.com/mod/fabricproxy-lite) | Velocity proxy support |

## Server Management

### Makefile Commands

| Command | Description |
|---------|-------------|
| `make start` | Start Minecraft server and playit tunnel |
| `make stop` | Stop Minecraft server and playit tunnel |
| `make restart` | Restart server and tunnel |
| `make status` | Show server, tunnel, and process status |
| `make backup` | Run backup script |
| `make logs` | Tail the latest log file |
| `make console` | Attach to tmux console (Ctrl+B, D to detach) |
| `make install` | Install systemd services |
| `make uninstall` | Remove systemd services |
| `make playit-download` | Download playit.gg binary |

### Directory Structure

```
minecraft/
├── server.jar              # Fabric server launcher (after Fabric install)
├── fabric-server-launch.jar
├── playit                  # playit.gg tunnel binary
├── mods/                   # Fabric mods go here
├── config/                 # Mod configuration files
├── world/                  # World data
├── logs/                   # Server logs
├── libraries/              # Fabric and Minecraft libraries
├── start.sh                # Server start script
├── backup.sh               # Backup script
├── minecraft.service       # Systemd service file
├── playit.service          # Playit tunnel service file
└── Makefile                # Management commands
```

## External Access (playit.gg)

External players connect via `starbase.minecraft.party`, which tunnels through playit.gg.

### Initial Setup

1. Download playit:
   ```bash
   make playit-download
   ```

2. Run playit and follow the claim URL to link your account:
   ```bash
   ./playit
   ```

3. Create a Minecraft tunnel in the playit.gg dashboard

4. Install the services:
   ```bash
   make install
   ```

The playit tunnel starts automatically with the Minecraft server.

## Troubleshooting

### Mods not loading
- Verify mod versions match your Minecraft version
- Check `logs/latest.log` for errors
- Ensure Fabric API is installed if required by the mod

### Server won't start after adding Fabric
- Run `java -jar fabric-server-launch.jar` directly to see detailed errors
- Check that all libraries downloaded correctly

### Mod conflicts
- Remove mods one at a time to identify conflicts
- Check mod documentation for known incompatibilities
