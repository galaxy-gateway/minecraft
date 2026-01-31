// API Client for Minecraft Web Admin

const api = {
    // Auth
    async checkAuth() {
        const res = await fetch('/api/auth/status');
        return res.json();
    },

    async logout() {
        const res = await fetch('/api/auth/logout', { method: 'POST' });
        return res.json();
    },

    // Server
    async getServerStatus() {
        const res = await fetch('/api/server/status');
        if (!res.ok) throw new Error('Failed to get server status');
        return res.json();
    },

    async startServer() {
        const res = await fetch('/api/server/start', { method: 'POST' });
        if (!res.ok) throw new Error('Failed to start server');
        return res.json();
    },

    async stopServer() {
        const res = await fetch('/api/server/stop', { method: 'POST' });
        if (!res.ok) throw new Error('Failed to stop server');
        return res.json();
    },

    async restartServer() {
        const res = await fetch('/api/server/restart', { method: 'POST' });
        if (!res.ok) throw new Error('Failed to restart server');
        return res.json();
    },

    // Console
    async sendCommand(command) {
        const res = await fetch('/api/console/command', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ command })
        });
        if (!res.ok) {
            const data = await res.json();
            throw new Error(data.error || 'Failed to send command');
        }
        return res.json();
    },

    async getConsoleOutput() {
        const res = await fetch('/api/console/output');
        if (!res.ok) throw new Error('Failed to get console output');
        return res.json();
    },

    // Logs
    async getLatestLogs(lines = 100) {
        const res = await fetch(`/api/logs/latest?lines=${lines}`);
        if (!res.ok) throw new Error('Failed to get logs');
        return res.json();
    },

    async getLogFiles() {
        const res = await fetch('/api/logs/files');
        if (!res.ok) throw new Error('Failed to get log files');
        return res.json();
    },

    // Mods
    async getMods() {
        const res = await fetch('/api/mods');
        if (!res.ok) throw new Error('Failed to get mods');
        return res.json();
    },

    async uploadMod(file) {
        const formData = new FormData();
        formData.append('file', file);
        const res = await fetch('/api/mods/upload', {
            method: 'POST',
            body: formData
        });
        if (!res.ok) {
            const data = await res.json();
            throw new Error(data.error || 'Failed to upload mod');
        }
        return res.json();
    },

    async deleteMod(filename) {
        const res = await fetch(`/api/mods/${encodeURIComponent(filename)}`, {
            method: 'DELETE',
            credentials: 'include'
        });
        const text = await res.text();
        const data = text ? JSON.parse(text) : {};
        if (!res.ok) {
            throw new Error(data.error || `Server returned ${res.status}: ${text || 'empty response'}`);
        }
        return data;
    },

    // Players - Ops
    async getOps() {
        const res = await fetch('/api/players/ops');
        if (!res.ok) throw new Error('Failed to get ops');
        return res.json();
    },

    async addOp(name) {
        const res = await fetch('/api/players/ops', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        });
        if (!res.ok) {
            const data = await res.json();
            throw new Error(data.error || 'Failed to add op');
        }
        return res.json();
    },

    async removeOp(name) {
        const res = await fetch(`/api/players/ops/${encodeURIComponent(name)}`, {
            method: 'DELETE',
            credentials: 'include'
        });
        const text = await res.text();
        const data = text ? JSON.parse(text) : {};
        if (!res.ok) {
            throw new Error(data.error || `Server returned ${res.status}: ${text || 'empty response'}`);
        }
        return data;
    },

    // Players - Whitelist
    async getWhitelist() {
        const res = await fetch('/api/players/whitelist');
        if (!res.ok) throw new Error('Failed to get whitelist');
        return res.json();
    },

    async addWhitelist(name) {
        const res = await fetch('/api/players/whitelist', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        });
        if (!res.ok) {
            const data = await res.json();
            throw new Error(data.error || 'Failed to add to whitelist');
        }
        return res.json();
    },

    async removeWhitelist(name) {
        const res = await fetch(`/api/players/whitelist/${encodeURIComponent(name)}`, {
            method: 'DELETE'
        });
        const text = await res.text();
        const data = text ? JSON.parse(text) : {};
        if (!res.ok) {
            throw new Error(data.error || 'Failed to remove from whitelist');
        }
        return data;
    },

    // Players - Banned
    async getBanned() {
        const res = await fetch('/api/players/banned');
        if (!res.ok) throw new Error('Failed to get banned players');
        return res.json();
    },

    async banPlayer(name) {
        const res = await fetch('/api/players/banned', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        });
        if (!res.ok) {
            const data = await res.json();
            throw new Error(data.error || 'Failed to ban player');
        }
        return res.json();
    },

    async unbanPlayer(name) {
        const res = await fetch(`/api/players/banned/${encodeURIComponent(name)}`, {
            method: 'DELETE'
        });
        const text = await res.text();
        const data = text ? JSON.parse(text) : {};
        if (!res.ok) {
            throw new Error(data.error || 'Failed to unban player');
        }
        return data;
    },

    // Backups
    async getBackups() {
        const res = await fetch('/api/backups');
        if (!res.ok) throw new Error('Failed to get backups');
        return res.json();
    },

    async createBackup(name = null) {
        const res = await fetch('/api/backups', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        });
        if (!res.ok) {
            const data = await res.json();
            throw new Error(data.error || 'Failed to create backup');
        }
        return res.json();
    },

    async deleteBackup(filename) {
        const res = await fetch(`/api/backups/${encodeURIComponent(filename)}`, {
            method: 'DELETE'
        });
        const text = await res.text();
        const data = text ? JSON.parse(text) : {};
        if (!res.ok) {
            throw new Error(data.error || 'Failed to delete backup');
        }
        return data;
    },

    async restoreBackup(filename) {
        const res = await fetch(`/api/backups/${encodeURIComponent(filename)}/restore`, {
            method: 'POST'
        });
        if (!res.ok) {
            const data = await res.json();
            throw new Error(data.error || 'Failed to restore backup');
        }
        return res.json();
    },

    // Scripts
    async getScripts() {
        const res = await fetch('/api/scripts');
        if (!res.ok) throw new Error('Failed to get scripts');
        return res.json();
    },

    async uploadScript(file, metadata) {
        const formData = new FormData();
        formData.append('file', file);
        if (metadata.name) formData.append('name', metadata.name);
        if (metadata.description) formData.append('description', metadata.description);
        if (metadata.version) formData.append('version', metadata.version);
        if (metadata.category) formData.append('category', metadata.category);
        if (metadata.author) formData.append('author', metadata.author);

        const res = await fetch('/api/scripts/upload', {
            method: 'POST',
            body: formData
        });
        if (!res.ok) {
            const data = await res.json();
            throw new Error(data.error || 'Failed to upload script');
        }
        return res.json();
    },

    async getScript(filename) {
        const res = await fetch(`/api/scripts/${encodeURIComponent(filename)}`);
        if (!res.ok) {
            const data = await res.json();
            throw new Error(data.error || 'Failed to get script');
        }
        return res.json();
    },

    async deleteScript(filename) {
        const res = await fetch(`/api/scripts/${encodeURIComponent(filename)}`, {
            method: 'DELETE'
        });
        const text = await res.text();
        const data = text ? JSON.parse(text) : {};
        if (!res.ok) {
            throw new Error(data.error || 'Failed to delete script');
        }
        return data;
    },

    async updateScriptMetadata(filename, metadata) {
        const res = await fetch(`/api/scripts/${encodeURIComponent(filename)}/metadata`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(metadata)
        });
        if (!res.ok) {
            const data = await res.json();
            throw new Error(data.error || 'Failed to update script metadata');
        }
        return res.json();
    }
};

// Export for use in other modules
window.api = api;
