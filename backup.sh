#!/bin/bash
# Minecraft World Backup Script
# Creates timestamped or named backups with rotation

set -e

# Configuration
SERVER_DIR="/home/caavere/Projects/minecraft"
BACKUP_DIR="${SERVER_DIR}/backups"
WORLD_NAME="world"
MAX_BACKUPS=10
TMUX_SESSION="minecraft"

# Get backup name from argument or use timestamp
if [ -n "$1" ]; then
    # Named backup - sanitize the name
    BACKUP_NAME=$(echo "$1" | tr ' ' '_' | tr -cd '[:alnum:]_-')
    BACKUP_FILE="${BACKUP_DIR}/${WORLD_NAME}_${BACKUP_NAME}.tar.gz"
    ROTATE=false
else
    # Timestamped backup
    TIMESTAMP=$(date +%Y-%m-%d_%H-%M-%S)
    BACKUP_FILE="${BACKUP_DIR}/${WORLD_NAME}_${TIMESTAMP}.tar.gz"
    ROTATE=true
fi

# Create backup directory if not exists
mkdir -p "$BACKUP_DIR"

# Check if backup already exists
if [ -f "$BACKUP_FILE" ]; then
    echo "Backup already exists: $BACKUP_FILE"
    read -p "Overwrite? [y/N] " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Backup cancelled."
        exit 1
    fi
fi

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

# Rotation: remove old timestamped backups (not named ones)
if [ "$ROTATE" = true ]; then
    echo "Rotating timestamped backups (keeping last $MAX_BACKUPS)..."
    cd "$BACKUP_DIR"
    # Only rotate backups matching timestamp pattern
    ls -t ${WORLD_NAME}_[0-9][0-9][0-9][0-9]-*.tar.gz 2>/dev/null | tail -n +$((MAX_BACKUPS + 1)) | xargs -r rm -f
fi

echo ""
echo "Backup complete!"
ls -lh "$BACKUP_FILE"

# Show current backups
echo ""
echo "Current backups:"
ls -lht "$BACKUP_DIR"/*.tar.gz 2>/dev/null || echo "No backups found"
