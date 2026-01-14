# Minecraft Server Management
# Server: Minecraft 1.21.11
# Location: /home/caavere/Projects/minecraft

.PHONY: start stop restart status backup logs console install uninstall help

TMUX_SESSION = minecraft
SERVICE_NAME = minecraft

# Default target
help:
	@echo "Minecraft Server Management"
	@echo ""
	@echo "Usage: make <target>"
	@echo ""
	@echo "Targets:"
	@echo "  start     - Start the Minecraft server"
	@echo "  stop      - Stop the Minecraft server (graceful)"
	@echo "  restart   - Restart the Minecraft server"
	@echo "  status    - Show server status"
	@echo "  backup    - Create a world backup"
	@echo "  logs      - Show server logs (tail -f)"
	@echo "  console   - Attach to server console (Ctrl+B, D to detach)"
	@echo "  install   - Install systemd service (requires sudo)"
	@echo "  uninstall - Remove systemd service (requires sudo)"

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
	@echo "=== Systemd Service ==="
	@systemctl is-active $(SERVICE_NAME) 2>/dev/null || echo "Service not running"
	@echo ""
	@echo "=== tmux Session ==="
	@tmux has-session -t $(TMUX_SESSION) 2>/dev/null && echo "Session '$(TMUX_SESSION)' is active" || echo "Session '$(TMUX_SESSION)' is not running"
	@echo ""
	@echo "=== Java Process ==="
	@pgrep -f "server.jar" >/dev/null 2>&1 && ps -p $$(pgrep -f "server.jar") -o pid,user,%mem,%cpu,etime,comm --no-headers || echo "No Minecraft process found"

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
	@echo "Installing systemd service..."
	@sudo cp minecraft.service /etc/systemd/system/
	@sudo systemctl daemon-reload
	@sudo systemctl enable $(SERVICE_NAME)
	@echo ""
	@echo "Service installed and enabled for auto-start on boot."
	@echo "Run 'make start' to start the server."

uninstall:
	@echo "Removing systemd service..."
	@sudo systemctl stop $(SERVICE_NAME) 2>/dev/null || true
	@sudo systemctl disable $(SERVICE_NAME) 2>/dev/null || true
	@sudo rm -f /etc/systemd/system/minecraft.service
	@sudo systemctl daemon-reload
	@echo "Service removed."
