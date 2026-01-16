# Client Setup

To connect to the Fabric modded server, your client must have the same mods installed.

## Server Address

```
starbase.minecraft.party
```

## Requirements

| Component | Version |
|-----------|---------|
| Minecraft | 1.21.1 |
| Fabric Loader | Latest |
| Fabric API | 0.104.0+1.21.1 |
| CC: Tweaked | 1.21.1-fabric-1.117.0 |

## Installation Steps

### 1. Install Fabric Loader

1. Download the Fabric installer: https://fabricmc.net/use/installer/
2. Run the installer
3. Select **Minecraft 1.21.1**
4. Click "Install"

### 2. Download Required Mods

Download these files:

- **Fabric API**: [fabric-api-0.104.0+1.21.1.jar](https://cdn.modrinth.com/data/P7dR8mSH/versions/qKPgBeHl/fabric-api-0.104.0%2B1.21.1.jar)
- **CC: Tweaked**: [cc-tweaked-1.21.1-fabric-1.117.0.jar](https://cdn.modrinth.com/data/gu7yAYhd/versions/oIbVdmfh/cc-tweaked-1.21.1-fabric-1.117.0.jar)

### 3. Install Mods

Place the downloaded `.jar` files in your Minecraft mods folder:

| OS | Mods Folder Location |
|----|---------------------|
| **macOS** | `~/Library/Application Support/minecraft/mods/` |
| **Windows** | `%APPDATA%\.minecraft\mods\` |
| **Linux** | `~/.minecraft/mods/` |

Create the `mods` folder if it doesn't exist.

### 4. Launch Minecraft

1. Open the Minecraft Launcher
2. Select the **fabric-loader-1.21.1** profile
3. Click Play
4. Connect to `starbase.minecraft.party`

## Troubleshooting

### "Failed to decode packet" / Protocol Error

This means your client mods don't match the server. Ensure you have:
- Exactly **Minecraft 1.21.1** (not 1.21.11 or other versions)
- The correct versions of Fabric API and CC: Tweaked listed above

### "Connection refused" / Can't Connect

- Check that the server is running: ask the server admin to run `make status`
- Verify you're using the correct address: `starbase.minecraft.party`

### Mods Folder Doesn't Exist

Create it manually:
```bash
# macOS/Linux
mkdir -p ~/.minecraft/mods

# Or for macOS specifically
mkdir -p ~/Library/Application\ Support/minecraft/mods
```

## Verifying Installation

When Minecraft launches with Fabric, you should see:
- "Fabric" indicator on the main menu (bottom left)
- Mods button showing Fabric API and CC: Tweaked loaded

## CC: Tweaked Basics

Once connected, you can craft and use ComputerCraft items:
- **Computer** - Programmable computer (craft with stone + glass pane + redstone)
- **Turtle** - Mobile robot that can mine and build
- **Monitor** - Display screen for computers
- **Disk Drive** - Save and load programs

See [op-cheatsheet.md](op-cheatsheet.md) for commands, or visit https://tweaked.cc/ for full documentation.
