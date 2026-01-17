# Minecraft Web Admin

A Rust-based web admin panel for managing the Minecraft server.

## Features

- **Dashboard**: Server status, quick start/stop/restart controls
- **Console**: Send commands to the server via tmux
- **Logs**: Real-time log streaming via WebSocket
- **Mods**: Upload, list, and delete mod files
- **Players**: Manage operators, whitelist, and banned players
- **Backups**: Create, restore, and delete world backups

## Installation

### 1. Build the application

```bash
make webadmin-build
```

### 2. Configure sudoers for server control

The web admin needs passwordless sudo access to control the Minecraft service. Create a sudoers file:

```bash
sudo visudo -f /etc/sudoers.d/minecraft-webadmin
```

Add the following (replace `caavere` with your username if different):

```
caavere ALL=(ALL) NOPASSWD: /usr/bin/systemctl start minecraft-fabric
caavere ALL=(ALL) NOPASSWD: /usr/bin/systemctl stop minecraft-fabric
caavere ALL=(ALL) NOPASSWD: /usr/bin/systemctl restart minecraft-fabric
```

### 3. Configure the application

Edit `.env` to customize settings:

```bash
# Bind address (0.0.0.0 for LAN access, 127.0.0.1 for local-only)
BIND_ADDRESS=0.0.0.0:8180

# Admin credentials
ADMIN_USERNAME=admin
ADMIN_PASSWORD_HASH='<your-hash-here>'

# Minecraft server directory
MINECRAFT_DIR=/home/caavere/Projects/minecraft
```

### 4. Generate a password hash

```bash
./target/release/hash-password "your-secure-password"
```

Copy the output and paste it into `.env` as `ADMIN_PASSWORD_HASH`. **Important**: Wrap the hash in single quotes to prevent `$` variable expansion.

### 5. Install and start the service

```bash
make webadmin-install
make webadmin-start
```

## Usage

Access the web admin at: `http://<server-ip>:8180`

### Makefile targets

| Command | Description |
|---------|-------------|
| `make webadmin-build` | Build the release binary |
| `make webadmin-install` | Install systemd service |
| `make webadmin-uninstall` | Remove systemd service |
| `make webadmin-start` | Start the service |
| `make webadmin-stop` | Stop the service |
| `make webadmin-restart` | Restart the service |
| `make webadmin-status` | Show service status |
| `make webadmin-logs` | Tail service logs |

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `BIND_ADDRESS` | IP and port to bind to | `127.0.0.1:8180` |
| `ADMIN_USERNAME` | Admin login username | `admin` |
| `ADMIN_PASSWORD_HASH` | Argon2 password hash | (required) |
| `MINECRAFT_DIR` | Path to Minecraft server | `/home/caavere/Projects/minecraft` |
| `SESSION_SECRET` | Secret for session cookies | (random default) |
| `RUST_LOG` | Logging level | `info` |

## Security Notes

- The web admin binds to `0.0.0.0` by default for LAN access. For internet-facing deployments, use a reverse proxy with HTTPS.
- Change the default password immediately after installation.
- The sudoers configuration only allows specific systemctl commands, not arbitrary sudo access.
