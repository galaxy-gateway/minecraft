#!/bin/bash
# Minecraft World Backup Script
# Creates timestamped backups with rotation

set -e

# Configuration
SERVER_DIR="/home/caavere/Projects/minecraft"
BACKUP_DIR="${SERVER_DIR}/backups"
WORLD_NAME="world"
MAX_BACKUPS=7
TMUX_SESSION="minecraft"

# Create backup directory if not exists
mkdir -p "$BACKUP_DIR"

# Timestamp for backup name
TIMESTAMP=$(date +%Y-%m-%d_%H-%M-%S)
BACKUP_FILE="${BACKUP_DIR}/${WORLD_NAME}_${TIMESTAMP}.tar.gz"

# Check if server is running
is_server_running() {
    tmux has-session -t "$TMUX_SESSION" 2>/dev/null
}

# Send command to server console
send_command() {
    tmux send-keys -t "$TMUX_SESSION" "$1" Enter
}

# If server running, disable auto-save
if is_server_running; then
    echo "Server is running, disabling auto-save..."
    send_command "save-off"
    send_command "save-all"
    sleep 5  # Wait for save to complete
fi

# Create backup
echo "Creating backup: $BACKUP_FILE"
tar -czf "$BACKUP_FILE" -C "$SERVER_DIR" "$WORLD_NAME"

# Re-enable auto-save
if is_server_running; then
    echo "Re-enabling auto-save..."
    send_command "save-on"
fi

# Rotation: remove old backups
echo "Rotating backups (keeping last $MAX_BACKUPS)..."
cd "$BACKUP_DIR"
ls -t ${WORLD_NAME}_*.tar.gz 2>/dev/null | tail -n +$((MAX_BACKUPS + 1)) | xargs -r rm -f

echo "Backup complete!"
ls -lh "$BACKUP_FILE"

# Show current backups
echo ""
echo "Current backups:"
ls -lht "$BACKUP_DIR"/*.tar.gz 2>/dev/null | head -n $MAX_BACKUPS
