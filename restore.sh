#!/bin/bash
# Minecraft World Restore Script
# Restores world from a named or timestamped backup

set -e

# Configuration
SERVER_DIR="/home/caavere/Projects/minecraft"
BACKUP_DIR="${SERVER_DIR}/backups"
WORLD_NAME="world"
TMUX_SESSION="minecraft"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if backup name provided
if [ -z "$1" ]; then
    echo "Usage: $0 <backup_name>"
    echo ""
    echo "Available backups:"
    ls -lht "$BACKUP_DIR"/*.tar.gz 2>/dev/null | awk '{print "  " $NF}' | sed "s|$BACKUP_DIR/||g" | sed 's/.tar.gz//g'
    exit 1
fi

# Find the backup file
BACKUP_NAME="$1"
BACKUP_FILE="${BACKUP_DIR}/${WORLD_NAME}_${BACKUP_NAME}.tar.gz"

# If not found, try without world_ prefix
if [ ! -f "$BACKUP_FILE" ]; then
    BACKUP_FILE="${BACKUP_DIR}/${BACKUP_NAME}.tar.gz"
fi

# If still not found, try exact match
if [ ! -f "$BACKUP_FILE" ]; then
    BACKUP_FILE="${BACKUP_DIR}/${BACKUP_NAME}"
fi

if [ ! -f "$BACKUP_FILE" ]; then
    echo -e "${RED}Error: Backup not found: $BACKUP_NAME${NC}"
    echo ""
    echo "Available backups:"
    ls -lht "$BACKUP_DIR"/*.tar.gz 2>/dev/null | awk '{print "  " $9}' | xargs -I{} basename {} .tar.gz | sed "s/${WORLD_NAME}_//g"
    exit 1
fi

echo -e "${YELLOW}Found backup: $BACKUP_FILE${NC}"
ls -lh "$BACKUP_FILE"
echo ""

# Check if server is running
is_server_running() {
    tmux has-session -t "$TMUX_SESSION" 2>/dev/null
}

if is_server_running; then
    echo -e "${RED}WARNING: Server is currently running!${NC}"
    echo "You must stop the server before restoring."
    echo ""
    echo "Run: make stop"
    exit 1
fi

# Confirm restore
echo -e "${RED}WARNING: This will REPLACE the current world!${NC}"
echo "Current world will be backed up as '${WORLD_NAME}_pre-restore.tar.gz'"
echo ""
read -p "Are you sure you want to restore? [y/N] " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Restore cancelled."
    exit 1
fi

# Backup current world before restore
echo ""
echo "Backing up current world before restore..."
PRE_RESTORE_BACKUP="${BACKUP_DIR}/${WORLD_NAME}_pre-restore.tar.gz"
if [ -d "${SERVER_DIR}/${WORLD_NAME}" ]; then
    tar -czf "$PRE_RESTORE_BACKUP" -C "$SERVER_DIR" "$WORLD_NAME"
    echo "Current world backed up to: $PRE_RESTORE_BACKUP"
fi

# Remove current world
echo "Removing current world..."
rm -rf "${SERVER_DIR}/${WORLD_NAME}"

# Extract backup
echo "Restoring from backup..."
tar -xzf "$BACKUP_FILE" -C "$SERVER_DIR"

echo ""
echo -e "${GREEN}Restore complete!${NC}"
echo ""
echo "Start the server with: make start"
