# Minecraft Server Management
# Server: Minecraft 1.21.11
# Location: /home/caavere/Projects/minecraft
# External Access: playit.gg tunnel (starbase.minecraft.party)

.PHONY: help start stop restart status \
        server-start server-stop server-restart server-status \
        playit-start playit-stop playit-restart playit-status \
        backup logs console \
        install uninstall server-install server-uninstall playit-install playit-uninstall \
        playit-download fabric-download fabric-install

# Configuration
TMUX_SESSION = minecraft
SERVER_SERVICE = minecraft
PLAYIT_SERVICE = playit
PLAYIT_BIN = /home/caavere/Projects/minecraft/playit

# Fabric configuration
MC_VERSION = 1.21.11
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
	@echo "Minecraft Server:"
	@echo "  server-start    - Start Minecraft server only"
	@echo "  server-stop     - Stop Minecraft server only"
	@echo "  server-restart  - Restart Minecraft server only"
	@echo "  server-status   - Show Minecraft server status"
	@echo "  console         - Attach to server console (Ctrl+B, D to detach)"
	@echo "  logs            - Tail server logs"
	@echo "  backup          - Create world backup"
	@echo ""
	@echo "Playit Tunnel:"
	@echo "  playit-start    - Start playit tunnel only"
	@echo "  playit-stop     - Stop playit tunnel only"
	@echo "  playit-restart  - Restart playit tunnel only"
	@echo "  playit-status   - Show playit tunnel status"
	@echo ""
	@echo "Setup:"
	@echo "  install         - Install all systemd services"
	@echo "  uninstall       - Remove all systemd services"
	@echo "  server-install  - Install Minecraft service only"
	@echo "  server-uninstall- Remove Minecraft service only"
	@echo "  playit-install  - Install playit service only"
	@echo "  playit-uninstall- Remove playit service only"
	@echo "  playit-download - Download playit.gg binary"
	@echo "  fabric-download - Download Fabric installer"
	@echo "  fabric-install  - Install Fabric server (MC $(MC_VERSION))"

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
	@echo "=== Minecraft Server ==="
	@printf "  Service:  "; systemctl is-active $(SERVER_SERVICE) 2>/dev/null || echo "inactive"
	@printf "  tmux:     "; tmux has-session -t $(TMUX_SESSION) 2>/dev/null && echo "active" || echo "inactive"
	@printf "  Process:  "; pgrep -f "server.jar" >/dev/null 2>&1 && echo "running (PID $$(pgrep -f 'server.jar' | head -1))" || echo "not running"

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

#===============================================================================
# INSTALLATION
#===============================================================================

install: server-install playit-install
	@echo ""
	@echo "All services installed. Run 'make start' to start."

uninstall: playit-uninstall server-uninstall
	@echo ""
	@echo "All services removed."

server-install:
	@echo "Installing Minecraft service..."
	@sudo cp minecraft.service /etc/systemd/system/
	@sudo systemctl daemon-reload
	@sudo systemctl enable $(SERVER_SERVICE)
	@echo "Minecraft service installed and enabled."

server-uninstall:
	@echo "Removing Minecraft service..."
	@sudo systemctl stop $(SERVER_SERVICE) 2>/dev/null || true
	@sudo systemctl disable $(SERVER_SERVICE) 2>/dev/null || true
	@sudo rm -f /etc/systemd/system/minecraft.service
	@sudo systemctl daemon-reload
	@echo "Minecraft service removed."

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
	@curl -SsL https://playit.gg/downloads/playit-linux-amd64 -o $(PLAYIT_BIN)
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
	@echo "Next: Place mods in 'mods/' and run 'make start'"
