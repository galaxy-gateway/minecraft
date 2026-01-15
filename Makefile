# Minecraft Server Management
# Server: Minecraft 1.21.1 (Fabric) / Vanilla
# Location: /home/caavere/Projects/minecraft
# External Access: playit.gg tunnel (starbase.minecraft.party)

.PHONY: help start stop restart status \
        server-start server-stop server-restart server-status \
        fabric-start fabric-stop fabric-restart fabric-status \
        playit-start playit-stop playit-restart playit-status playit-logs \
        backup logs console \
        install uninstall server-install server-uninstall fabric-server-install fabric-server-uninstall playit-install playit-uninstall \
        playit-download fabric-download fabric-install

# Configuration
TMUX_SESSION = minecraft
SERVER_SERVICE = minecraft
FABRIC_SERVICE = minecraft-fabric
PLAYIT_SERVICE = playit
PLAYIT_BIN = /home/caavere/Projects/minecraft/playit

# Fabric configuration
MC_VERSION = 1.21.1
FABRIC_INSTALLER_VERSION = 1.1.1
FABRIC_INSTALLER = fabric-installer-$(FABRIC_INSTALLER_VERSION).jar
FABRIC_INSTALLER_URL = https://maven.fabricmc.net/net/fabricmc/fabric-installer/$(FABRIC_INSTALLER_VERSION)/$(FABRIC_INSTALLER)

#===============================================================================
# HELP
#===============================================================================

help:
	@echo "Minecraft Server Management"
	@echo ""
	@echo "Usage: make <target>"
	@echo ""
	@echo "All Services:"
	@echo "  start           - Start server + playit tunnel"
	@echo "  stop            - Stop server + playit tunnel"
	@echo "  restart         - Restart server + playit tunnel"
	@echo "  status          - Show status of all services"
	@echo ""
	@echo "Vanilla Server:"
	@echo "  server-start    - Start vanilla Minecraft server"
	@echo "  server-stop     - Stop vanilla Minecraft server"
	@echo "  server-restart  - Restart vanilla Minecraft server"
	@echo "  server-status   - Show vanilla server status"
	@echo ""
	@echo "Fabric Server (mods):"
	@echo "  fabric-start    - Start Fabric modded server"
	@echo "  fabric-stop     - Stop Fabric modded server"
	@echo "  fabric-restart  - Restart Fabric modded server"
	@echo "  fabric-status   - Show Fabric server status"
	@echo ""
	@echo "Server Tools:"
	@echo "  console         - Attach to server console (Ctrl+B, D to detach)"
	@echo "  logs            - Tail server logs"
	@echo "  backup          - Create world backup"
	@echo ""
	@echo "Playit Tunnel:"
	@echo "  playit-start    - Start playit tunnel only"
	@echo "  playit-stop     - Stop playit tunnel only"
	@echo "  playit-restart  - Restart playit tunnel only"
	@echo "  playit-status   - Show playit tunnel status"
	@echo "  playit-logs     - Tail playit tunnel logs"
	@echo ""
	@echo "Setup:"
	@echo "  install              - Install vanilla + playit services"
	@echo "  uninstall            - Remove all systemd services"
	@echo "  server-install       - Install vanilla Minecraft service"
	@echo "  server-uninstall     - Remove vanilla Minecraft service"
	@echo "  fabric-server-install   - Install Fabric server service"
	@echo "  fabric-server-uninstall - Remove Fabric server service"
	@echo "  playit-install       - Install playit service"
	@echo "  playit-uninstall     - Remove playit service"
	@echo "  playit-download      - Download playit.gg binary"
	@echo "  fabric-download      - Download Fabric installer"
	@echo "  fabric-install       - Install Fabric server (MC $(MC_VERSION))"

#===============================================================================
# ALL SERVICES
#===============================================================================

start: server-start playit-start
	@echo ""
	@$(MAKE) --no-print-directory status

stop: playit-stop server-stop
	@echo ""
	@$(MAKE) --no-print-directory status

restart: stop start

status: server-status playit-status

#===============================================================================
# MINECRAFT SERVER
#===============================================================================

server-start:
	@echo "Starting Minecraft server..."
	@sudo systemctl start $(SERVER_SERVICE)
	@sleep 2
	@systemctl is-active --quiet $(SERVER_SERVICE) && echo "Minecraft server started" || echo "Failed to start server"

server-stop:
	@echo "Stopping Minecraft server..."
	@sudo systemctl stop $(SERVER_SERVICE)
	@sleep 2
	@systemctl is-active --quiet $(SERVER_SERVICE) && echo "Server still running" || echo "Minecraft server stopped"

server-restart:
	@echo "Restarting Minecraft server..."
	@sudo systemctl restart $(SERVER_SERVICE)
	@sleep 3
	@$(MAKE) --no-print-directory server-status

server-status:
	@echo "=== Vanilla Server ==="
	@printf "  Service:  "; systemctl is-active $(SERVER_SERVICE) 2>/dev/null || echo "inactive"
	@printf "  tmux:     "; tmux has-session -t $(TMUX_SESSION) 2>/dev/null && echo "active" || echo "inactive"
	@printf "  Process:  "; pgrep -f "server.jar" >/dev/null 2>&1 && echo "running (PID $$(pgrep -f 'server.jar' | head -1))" || echo "not running"

#===============================================================================
# FABRIC SERVER (MODDED)
#===============================================================================

fabric-start:
	@echo "Starting Fabric server..."
	@sudo systemctl start $(FABRIC_SERVICE)
	@sleep 2
	@systemctl is-active --quiet $(FABRIC_SERVICE) && echo "Fabric server started" || echo "Failed to start Fabric server"

fabric-stop:
	@echo "Stopping Fabric server..."
	@sudo systemctl stop $(FABRIC_SERVICE)
	@sleep 2
	@systemctl is-active --quiet $(FABRIC_SERVICE) && echo "Server still running" || echo "Fabric server stopped"

fabric-restart:
	@echo "Restarting Fabric server..."
	@sudo systemctl restart $(FABRIC_SERVICE)
	@sleep 3
	@$(MAKE) --no-print-directory fabric-status

fabric-status:
	@echo "=== Fabric Server ==="
	@printf "  Service:  "; systemctl is-active $(FABRIC_SERVICE) 2>/dev/null || echo "inactive"
	@printf "  tmux:     "; tmux has-session -t $(TMUX_SESSION) 2>/dev/null && echo "active" || echo "inactive"
	@printf "  Process:  "; pgrep -f "fabric-server-launch.jar" >/dev/null 2>&1 && echo "running (PID $$(pgrep -f 'fabric-server-launch.jar' | head -1))" || echo "not running"

console:
	@echo "Attaching to Minecraft console..."
	@echo "To detach: Press Ctrl+B, then D"
	@echo ""
	@tmux attach-session -t $(TMUX_SESSION)

logs:
	@tail -f logs/latest.log

backup:
	@./backup.sh

#===============================================================================
# PLAYIT TUNNEL
#===============================================================================

playit-start:
	@echo "Starting playit tunnel..."
	@sudo systemctl start $(PLAYIT_SERVICE)
	@sleep 2
	@systemctl is-active --quiet $(PLAYIT_SERVICE) && echo "Playit tunnel started (starbase.minecraft.party)" || echo "Failed to start tunnel"

playit-stop:
	@echo "Stopping playit tunnel..."
	@sudo systemctl stop $(PLAYIT_SERVICE)
	@sleep 1
	@systemctl is-active --quiet $(PLAYIT_SERVICE) && echo "Tunnel still running" || echo "Playit tunnel stopped"

playit-restart:
	@echo "Restarting playit tunnel..."
	@sudo systemctl restart $(PLAYIT_SERVICE)
	@sleep 2
	@$(MAKE) --no-print-directory playit-status

playit-status:
	@echo "=== Playit Tunnel ==="
	@printf "  Service:  "; systemctl is-active $(PLAYIT_SERVICE) 2>/dev/null || echo "inactive"
	@printf "  Address:  "; systemctl is-active --quiet $(PLAYIT_SERVICE) && echo "starbase.minecraft.party" || echo "n/a"

playit-logs:
	@journalctl -u $(PLAYIT_SERVICE) -f

#===============================================================================
# INSTALLATION
#===============================================================================

install: server-install playit-install
	@echo ""
	@echo "All services installed. Run 'make start' to start."

uninstall: playit-uninstall fabric-server-uninstall server-uninstall
	@echo ""
	@echo "All services removed."

server-install:
	@echo "Installing Minecraft service..."
	@sudo cp minecraft.service /etc/systemd/system/
	@sudo systemctl daemon-reload
	@sudo systemctl enable $(SERVER_SERVICE)
	@echo "Minecraft service installed and enabled."

server-uninstall:
	@echo "Removing vanilla Minecraft service..."
	@sudo systemctl stop $(SERVER_SERVICE) 2>/dev/null || true
	@sudo systemctl disable $(SERVER_SERVICE) 2>/dev/null || true
	@sudo rm -f /etc/systemd/system/minecraft.service
	@sudo systemctl daemon-reload
	@echo "Vanilla Minecraft service removed."

fabric-server-install:
	@echo "Installing Fabric server service..."
	@sudo cp minecraft-fabric.service /etc/systemd/system/
	@sudo systemctl daemon-reload
	@sudo systemctl enable $(FABRIC_SERVICE)
	@echo "Fabric server service installed and enabled."

fabric-server-uninstall:
	@echo "Removing Fabric server service..."
	@sudo systemctl stop $(FABRIC_SERVICE) 2>/dev/null || true
	@sudo systemctl disable $(FABRIC_SERVICE) 2>/dev/null || true
	@sudo rm -f /etc/systemd/system/minecraft-fabric.service
	@sudo systemctl daemon-reload
	@echo "Fabric server service removed."

playit-install:
	@echo "Installing playit service..."
	@sudo cp playit.service /etc/systemd/system/
	@sudo systemctl daemon-reload
	@sudo systemctl enable $(PLAYIT_SERVICE)
	@echo "Playit service installed and enabled."

playit-uninstall:
	@echo "Removing playit service..."
	@sudo systemctl stop $(PLAYIT_SERVICE) 2>/dev/null || true
	@sudo systemctl disable $(PLAYIT_SERVICE) 2>/dev/null || true
	@sudo rm -f /etc/systemd/system/playit.service
	@sudo systemctl daemon-reload
	@echo "Playit service removed."

#===============================================================================
# DOWNLOADS & FABRIC
#===============================================================================

playit-download:
	@echo "Downloading playit.gg..."
	@curl -SsL -L https://github.com/playit-cloud/playit-agent/releases/latest/download/playit-linux-amd64 -o $(PLAYIT_BIN)
	@chmod +x $(PLAYIT_BIN)
	@echo "Downloaded to $(PLAYIT_BIN)"
	@echo "Run './playit' to configure your tunnel, then 'make playit-install'."

fabric-download:
	@echo "Downloading Fabric installer $(FABRIC_INSTALLER_VERSION)..."
	@wget -q $(FABRIC_INSTALLER_URL) -O $(FABRIC_INSTALLER)
	@echo "Downloaded $(FABRIC_INSTALLER)"

fabric-install: fabric-download
	@echo "Installing Fabric server for Minecraft $(MC_VERSION)..."
	@java -jar $(FABRIC_INSTALLER) server -mcversion $(MC_VERSION) -downloadMinecraft
	@echo ""
	@echo "Fabric installed."
	@ls -la server.jar fabric-server-launch.jar 2>/dev/null || true
	@echo ""
	@echo "Next steps:"
	@echo "  1. Place mods in 'mods/' folder"
	@echo "  2. Run 'make fabric-server-install' to install the systemd service"
	@echo "  3. Run 'make fabric-start' to start the Fabric server"
