# Minecraft Server Management
# Server: Minecraft 1.21.11
# Location: /home/caavere/Projects/minecraft
# External Access: playit.gg tunnel (starbase.minecraft.party)

.PHONY: start stop restart status backup logs console install uninstall help playit-download

TMUX_SESSION = minecraft
SERVICE_NAME = minecraft
PLAYIT_SERVICE = playit
PLAYIT_BIN = /home/caavere/Projects/minecraft/playit

# Default target
help:
	@echo "Minecraft Server Management"
	@echo ""
	@echo "Usage: make <target>"
	@echo ""
	@echo "Server:"
	@echo "  start     - Start Minecraft server and playit tunnel"
	@echo "  stop      - Stop Minecraft server and playit tunnel"
	@echo "  restart   - Restart Minecraft server and playit tunnel"
	@echo "  status    - Show server and tunnel status"
	@echo "  backup    - Create a world backup"
	@echo "  logs      - Show server logs (tail -f)"
	@echo "  console   - Attach to server console (Ctrl+B, D to detach)"
	@echo ""
	@echo "Setup:"
	@echo "  install       - Install systemd services (requires sudo)"
	@echo "  uninstall     - Remove systemd services (requires sudo)"
	@echo "  playit-download - Download playit.gg binary"

start:
	@sudo systemctl start $(SERVICE_NAME)
	@echo "Server starting..."
	@sleep 3
	@$(MAKE) --no-print-directory status

stop:
	@sudo systemctl stop $(SERVICE_NAME)
	@echo "Server stopping..."
	@sleep 3
	@$(MAKE) --no-print-directory status

restart:
	@sudo systemctl restart $(SERVICE_NAME)
	@echo "Server restarting..."
	@sleep 5
	@$(MAKE) --no-print-directory status

status:
	@echo "=== Minecraft Service ==="
	@systemctl is-active $(SERVICE_NAME) 2>/dev/null || echo "Service not running"
	@echo ""
	@echo "=== Playit Tunnel ==="
	@systemctl is-active $(PLAYIT_SERVICE) 2>/dev/null && echo "Tunnel active (starbase.minecraft.party)" || echo "Tunnel not running"
	@echo ""
	@echo "=== tmux Session ==="
	@tmux has-session -t $(TMUX_SESSION) 2>/dev/null && echo "Session '$(TMUX_SESSION)' is active" || echo "Session '$(TMUX_SESSION)' is not running"
	@echo ""
	@echo "=== Java Process ==="
	@pgrep -f "server.jar" >/dev/null 2>&1 && ps -p $$(pgrep -f "server.jar" | tr '\n' ',' | sed 's/,$$//') -o pid,user,%mem,%cpu,etime,comm --no-headers || echo "No Minecraft process found"

backup:
	@./backup.sh

logs:
	@tail -f logs/latest.log

console:
	@echo "Attaching to Minecraft console..."
	@echo "To detach: Press Ctrl+B, then D"
	@echo ""
	@tmux attach-session -t $(TMUX_SESSION)

install:
	@echo "Installing systemd services..."
	@sudo cp minecraft.service /etc/systemd/system/
	@sudo cp playit.service /etc/systemd/system/
	@sudo systemctl daemon-reload
	@sudo systemctl enable $(SERVICE_NAME)
	@sudo systemctl enable $(PLAYIT_SERVICE)
	@echo ""
	@echo "Services installed and enabled."
	@echo "Playit tunnel will start automatically with Minecraft."
	@echo "Run 'make start' to start the server."

uninstall:
	@echo "Removing systemd services..."
	@sudo systemctl stop $(PLAYIT_SERVICE) 2>/dev/null || true
	@sudo systemctl stop $(SERVICE_NAME) 2>/dev/null || true
	@sudo systemctl disable $(PLAYIT_SERVICE) 2>/dev/null || true
	@sudo systemctl disable $(SERVICE_NAME) 2>/dev/null || true
	@sudo rm -f /etc/systemd/system/minecraft.service
	@sudo rm -f /etc/systemd/system/playit.service
	@sudo systemctl daemon-reload
	@echo "Services removed."

playit-download:
	@echo "Downloading playit.gg..."
	@curl -SsL https://playit.gg/downloads/playit-linux-amd64 -o $(PLAYIT_BIN)
	@chmod +x $(PLAYIT_BIN)
	@echo "Downloaded to $(PLAYIT_BIN)"
	@echo "Run './playit' to configure your tunnel, then 'make install' to set up the service."
