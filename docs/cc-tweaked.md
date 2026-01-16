# CC: Tweaked Installation

CC: Tweaked is a ComputerCraft fork that adds programmable computers, turtles, and more to Minecraft.

## Requirements

- Fabric Loader
- Fabric API (required dependency)

## Compatibility

| Minecraft Version | CC: Tweaked Version | Status |
|-------------------|---------------------|--------|
| 1.21.11           | Not available       | Waiting for update |
| 1.21.8            | 1.117.0             | Supported |
| 1.21.1            | 1.117.0             | **Current (this server)** |
| 1.20.1            | 1.113.1             | Supported |

**Note:** This server runs Minecraft **1.21.1** with CC: Tweaked 1.117.0.

## Installation

1. Download Fabric API for your Minecraft version:
   ```bash
   cd /home/caavere/Projects/minecraft/mods
   curl -LO "https://cdn.modrinth.com/data/P7dR8mSH/versions/[version]/fabric-api-[version].jar"
   ```

2. Download CC: Tweaked from Modrinth:
   ```bash
   curl -LO "https://cdn.modrinth.com/data/gu7yAYhd/versions/[version]/cc-tweaked-[version].jar"
   ```

3. Restart the server:
   ```bash
   make restart
   ```

## Download Sources

- **Modrinth (preferred):** https://modrinth.com/mod/cc-tweaked/versions
- **Documentation:** https://tweaked.cc/
- **GitHub:** https://github.com/cc-tweaked/CC-Tweaked

## Options for 1.21.11

1. **Wait** - CC: Tweaked may release a 1.21.11 compatible version soon
2. **Try 1.21.8 version** - May work across minor version differences
3. **Downgrade server** - Use `make fabric-install` with MC_VERSION=1.21.8

## Client Setup

Clients connecting to the server **must have the same mods installed**. See [client-setup.md](client-setup.md) for detailed instructions.

## Troubleshooting

If the mod fails to load, check the logs:
```bash
make logs
```

Common issues:
- Missing Fabric API dependency
- Version mismatch between Minecraft and mod
- Incompatible mod combinations
- **Client missing mods** - Clients must install Fabric + same mods as server (see [client-setup.md](client-setup.md))
