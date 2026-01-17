// WebSocket Manager for Minecraft Web Admin

class WebSocketManager {
    constructor(path, handlers) {
        this.path = path;
        this.handlers = handlers;
        this.ws = null;
        this.reconnectTimeout = null;
        this.reconnectDelay = 1000;
        this.maxReconnectDelay = 30000;
    }

    connect() {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const url = `${protocol}//${window.location.host}${this.path}`;

        this.ws = new WebSocket(url);

        this.ws.onopen = () => {
            console.log(`WebSocket connected: ${this.path}`);
            this.reconnectDelay = 1000;
            if (this.handlers.onOpen) {
                this.handlers.onOpen();
            }
        };

        this.ws.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                if (this.handlers.onMessage) {
                    this.handlers.onMessage(data);
                }
            } catch (e) {
                console.error('Failed to parse WebSocket message:', e);
            }
        };

        this.ws.onclose = () => {
            console.log(`WebSocket closed: ${this.path}`);
            if (this.handlers.onClose) {
                this.handlers.onClose();
            }
            this.scheduleReconnect();
        };

        this.ws.onerror = (error) => {
            console.error(`WebSocket error: ${this.path}`, error);
            if (this.handlers.onError) {
                this.handlers.onError(error);
            }
        };
    }

    send(data) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify(data));
        }
    }

    scheduleReconnect() {
        if (this.reconnectTimeout) {
            clearTimeout(this.reconnectTimeout);
        }

        this.reconnectTimeout = setTimeout(() => {
            console.log(`Reconnecting WebSocket: ${this.path}`);
            this.connect();
            this.reconnectDelay = Math.min(this.reconnectDelay * 2, this.maxReconnectDelay);
        }, this.reconnectDelay);
    }

    disconnect() {
        if (this.reconnectTimeout) {
            clearTimeout(this.reconnectTimeout);
            this.reconnectTimeout = null;
        }

        if (this.ws) {
            this.ws.close();
            this.ws = null;
        }
    }
}

// Export for use in other modules
window.WebSocketManager = WebSocketManager;
