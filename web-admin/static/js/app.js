// Main Application Logic for Minecraft Web Admin

const app = {
    currentPage: 'dashboard',
    consoleWs: null,
    logsWs: null,
    commandHistory: [],
    commandHistoryIndex: -1,

    async init() {
        // Check authentication
        try {
            const auth = await api.checkAuth();
            if (!auth.authenticated) {
                window.location.href = '/login.html';
                return;
            }
        } catch (e) {
            window.location.href = '/login.html';
            return;
        }

        // Setup navigation
        this.setupNavigation();

        // Load initial page
        this.showPage('dashboard');
    },

    setupNavigation() {
        document.querySelectorAll('.nav-item').forEach(item => {
            item.addEventListener('click', () => {
                const page = item.dataset.page;
                this.showPage(page);
            });
        });

        // Logout button
        document.getElementById('logout-btn').addEventListener('click', async () => {
            await api.logout();
            window.location.href = '/login.html';
        });
    },

    showPage(page) {
        // Update nav
        document.querySelectorAll('.nav-item').forEach(item => {
            item.classList.toggle('active', item.dataset.page === page);
        });

        // Hide all pages
        document.querySelectorAll('.page').forEach(p => p.classList.add('hidden'));

        // Show selected page
        document.getElementById(`page-${page}`).classList.remove('hidden');

        // Disconnect WebSockets when leaving console/logs pages
        if (this.currentPage === 'console' && page !== 'console') {
            if (this.consoleWs) {
                this.consoleWs.disconnect();
                this.consoleWs = null;
            }
        }
        if (this.currentPage === 'logs' && page !== 'logs') {
            if (this.logsWs) {
                this.logsWs.disconnect();
                this.logsWs = null;
            }
        }

        this.currentPage = page;

        // Load page data
        this.loadPageData(page);
    },

    async loadPageData(page) {
        switch (page) {
            case 'dashboard':
                await this.loadDashboard();
                break;
            case 'console':
                this.initConsole();
                break;
            case 'logs':
                this.initLogs();
                break;
            case 'mods':
                await this.loadMods();
                break;
            case 'players':
                await this.loadPlayers();
                break;
            case 'backups':
                await this.loadBackups();
                break;
        }
    },

    // Dashboard
    async loadDashboard() {
        try {
            const status = await api.getServerStatus();
            this.updateServerStatus(status);
        } catch (e) {
            console.error('Failed to load dashboard:', e);
        }
    },

    updateServerStatus(status) {
        const statusBadge = document.getElementById('server-status-badge');
        const statusText = document.getElementById('server-status-text');
        const tmuxStatus = document.getElementById('tmux-status');

        statusBadge.className = `status-badge ${status.status}`;
        statusBadge.innerHTML = `<span class="status-dot"></span>${status.status}`;
        statusText.textContent = status.status === 'active' ? 'Server is running' : 'Server is stopped';
        tmuxStatus.textContent = status.tmux_active ? 'Active' : 'Inactive';
    },

    async serverAction(action) {
        const btn = event.target;
        btn.disabled = true;

        try {
            switch (action) {
                case 'start':
                    await api.startServer();
                    break;
                case 'stop':
                    await api.stopServer();
                    break;
                case 'restart':
                    await api.restartServer();
                    break;
            }

            // Wait a bit and refresh status
            setTimeout(async () => {
                await this.loadDashboard();
                btn.disabled = false;
            }, 2000);
        } catch (e) {
            alert('Action failed: ' + e.message);
            btn.disabled = false;
        }
    },

    // Console
    initConsole() {
        const output = document.getElementById('console-output');
        const input = document.getElementById('console-input');

        // Connect WebSocket
        this.consoleWs = new WebSocketManager('/api/console/ws', {
            onMessage: (data) => {
                if (data.type === 'output') {
                    output.textContent = data.payload;
                    output.scrollTop = output.scrollHeight;
                }
            }
        });
        this.consoleWs.connect();

        // Command input handler
        input.onkeydown = (e) => {
            if (e.key === 'Enter') {
                this.sendConsoleCommand();
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                this.navigateCommandHistory(-1);
            } else if (e.key === 'ArrowDown') {
                e.preventDefault();
                this.navigateCommandHistory(1);
            }
        };
    },

    async sendConsoleCommand() {
        const input = document.getElementById('console-input');
        const command = input.value.trim();

        if (!command) return;

        try {
            // Add to history
            this.commandHistory.push(command);
            this.commandHistoryIndex = this.commandHistory.length;

            // Send via REST (more reliable than WS for commands)
            await api.sendCommand(command);
            input.value = '';
        } catch (e) {
            alert('Failed to send command: ' + e.message);
        }
    },

    navigateCommandHistory(direction) {
        const input = document.getElementById('console-input');
        const newIndex = this.commandHistoryIndex + direction;

        if (newIndex >= 0 && newIndex < this.commandHistory.length) {
            this.commandHistoryIndex = newIndex;
            input.value = this.commandHistory[newIndex];
        } else if (newIndex >= this.commandHistory.length) {
            this.commandHistoryIndex = this.commandHistory.length;
            input.value = '';
        }
    },

    // Logs
    initLogs() {
        const container = document.getElementById('log-container');

        // Connect WebSocket
        this.logsWs = new WebSocketManager('/api/logs/ws', {
            onMessage: (data) => {
                if (data.type === 'initial') {
                    container.innerHTML = data.payload.map(line => this.formatLogLine(line)).join('');
                } else if (data.type === 'line') {
                    container.innerHTML += this.formatLogLine(data.payload);
                }
                container.scrollTop = container.scrollHeight;
            }
        });
        this.logsWs.connect();
    },

    formatLogLine(line) {
        let className = 'log-line';
        if (line.includes('WARN')) className += ' warn';
        else if (line.includes('ERROR')) className += ' error';
        else if (line.includes('INFO')) className += ' info';

        return `<div class="${className}">${this.escapeHtml(line)}</div>`;
    },

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    },

    // Mods
    async loadMods() {
        const list = document.getElementById('mods-list');
        list.innerHTML = '<tr><td colspan="4">Loading...</td></tr>';

        try {
            const data = await api.getMods();

            if (data.mods.length === 0) {
                list.innerHTML = '<tr><td colspan="4" class="empty-state">No mods installed</td></tr>';
                return;
            }

            list.innerHTML = data.mods.map(mod => `
                <tr>
                    <td>${this.escapeHtml(mod.name)}</td>
                    <td class="file-size">${this.formatSize(mod.size)}</td>
                    <td>${this.formatDate(mod.modified)}</td>
                    <td class="actions">
                        <button class="btn btn-danger btn-sm" onclick="app.deleteMod('${this.escapeHtml(mod.name)}')">Delete</button>
                    </td>
                </tr>
            `).join('');
        } catch (e) {
            list.innerHTML = `<tr><td colspan="4" class="error-message">Failed to load mods: ${e.message}</td></tr>`;
        }
    },

    async deleteMod(filename) {
        if (!confirm(`Delete mod "${filename}"?`)) return;

        try {
            await api.deleteMod(filename);
            await this.loadMods();
        } catch (e) {
            alert('Failed to delete mod: ' + e.message);
        }
    },

    setupModUpload() {
        const area = document.getElementById('mod-upload-area');
        const input = document.getElementById('mod-file-input');

        area.onclick = () => input.click();

        area.ondragover = (e) => {
            e.preventDefault();
            area.classList.add('dragover');
        };

        area.ondragleave = () => {
            area.classList.remove('dragover');
        };

        area.ondrop = async (e) => {
            e.preventDefault();
            area.classList.remove('dragover');

            const file = e.dataTransfer.files[0];
            if (file) await this.uploadMod(file);
        };

        input.onchange = async () => {
            if (input.files[0]) {
                await this.uploadMod(input.files[0]);
                input.value = '';
            }
        };
    },

    async uploadMod(file) {
        if (!file.name.endsWith('.jar')) {
            alert('Only .jar files are allowed');
            return;
        }

        try {
            await api.uploadMod(file);
            await this.loadMods();
        } catch (e) {
            alert('Failed to upload mod: ' + e.message);
        }
    },

    // Players
    async loadPlayers() {
        await Promise.all([
            this.loadOps(),
            this.loadWhitelist(),
            this.loadBanned()
        ]);
    },

    async loadOps() {
        const list = document.getElementById('ops-list');

        try {
            const data = await api.getOps();
            list.innerHTML = data.players.length === 0
                ? '<tr><td colspan="3" class="empty-state">No operators</td></tr>'
                : data.players.map(p => `
                    <tr>
                        <td>${this.escapeHtml(p.name)}</td>
                        <td>Level ${p.level}</td>
                        <td class="actions">
                            <button class="btn btn-danger btn-sm" onclick="app.removeOp('${this.escapeHtml(p.name)}')">Remove</button>
                        </td>
                    </tr>
                `).join('');
        } catch (e) {
            list.innerHTML = `<tr><td colspan="3" class="error-message">${e.message}</td></tr>`;
        }
    },

    async addOp() {
        const input = document.getElementById('add-op-input');
        const name = input.value.trim();
        if (!name) return;

        try {
            await api.addOp(name);
            input.value = '';
            await new Promise(r => setTimeout(r, 500));
            await this.loadOps();
        } catch (e) {
            alert('Failed to add op: ' + e.message);
        }
    },

    async removeOp(name) {
        if (!confirm(`Remove "${name}" from operators?`)) return;

        try {
            await api.removeOp(name);
            // Wait for server to update the JSON file
            await new Promise(r => setTimeout(r, 500));
            await this.loadOps();
        } catch (e) {
            alert('Failed to remove op: ' + e.message);
        }
    },

    async loadWhitelist() {
        const list = document.getElementById('whitelist-list');

        try {
            const data = await api.getWhitelist();
            list.innerHTML = data.players.length === 0
                ? '<tr><td colspan="2" class="empty-state">Whitelist empty</td></tr>'
                : data.players.map(p => `
                    <tr>
                        <td>${this.escapeHtml(p.name)}</td>
                        <td class="actions">
                            <button class="btn btn-danger btn-sm" onclick="app.removeWhitelist('${this.escapeHtml(p.name)}')">Remove</button>
                        </td>
                    </tr>
                `).join('');
        } catch (e) {
            list.innerHTML = `<tr><td colspan="2" class="error-message">${e.message}</td></tr>`;
        }
    },

    async addWhitelist() {
        const input = document.getElementById('add-whitelist-input');
        const name = input.value.trim();
        if (!name) return;

        try {
            await api.addWhitelist(name);
            input.value = '';
            await new Promise(r => setTimeout(r, 500));
            await this.loadWhitelist();
        } catch (e) {
            alert('Failed to add to whitelist: ' + e.message);
        }
    },

    async removeWhitelist(name) {
        if (!confirm(`Remove "${name}" from whitelist?`)) return;

        try {
            await api.removeWhitelist(name);
            await new Promise(r => setTimeout(r, 500));
            await this.loadWhitelist();
        } catch (e) {
            alert('Failed to remove from whitelist: ' + e.message);
        }
    },

    async loadBanned() {
        const list = document.getElementById('banned-list');

        try {
            const data = await api.getBanned();
            list.innerHTML = data.players.length === 0
                ? '<tr><td colspan="3" class="empty-state">No banned players</td></tr>'
                : data.players.map(p => `
                    <tr>
                        <td>${this.escapeHtml(p.name)}</td>
                        <td>${this.escapeHtml(p.reason || 'No reason')}</td>
                        <td class="actions">
                            <button class="btn btn-success btn-sm" onclick="app.unbanPlayer('${this.escapeHtml(p.name)}')">Unban</button>
                        </td>
                    </tr>
                `).join('');
        } catch (e) {
            list.innerHTML = `<tr><td colspan="3" class="error-message">${e.message}</td></tr>`;
        }
    },

    async banPlayer() {
        const input = document.getElementById('ban-player-input');
        const name = input.value.trim();
        if (!name) return;

        try {
            await api.banPlayer(name);
            input.value = '';
            await new Promise(r => setTimeout(r, 500));
            await this.loadBanned();
        } catch (e) {
            alert('Failed to ban player: ' + e.message);
        }
    },

    async unbanPlayer(name) {
        if (!confirm(`Unban "${name}"?`)) return;

        try {
            await api.unbanPlayer(name);
            await new Promise(r => setTimeout(r, 500));
            await this.loadBanned();
        } catch (e) {
            alert('Failed to unban player: ' + e.message);
        }
    },

    showTab(tabId) {
        // Update tab buttons
        document.querySelectorAll('.tab').forEach(t => {
            t.classList.toggle('active', t.dataset.tab === tabId);
        });

        // Update tab content
        document.querySelectorAll('.tab-content').forEach(c => {
            c.classList.toggle('active', c.id === `tab-${tabId}`);
        });
    },

    // Backups
    async loadBackups() {
        const list = document.getElementById('backups-list');
        list.innerHTML = '<tr><td colspan="4">Loading...</td></tr>';

        try {
            const data = await api.getBackups();

            if (data.backups.length === 0) {
                list.innerHTML = '<tr><td colspan="4" class="empty-state">No backups found</td></tr>';
                return;
            }

            list.innerHTML = data.backups.map(backup => `
                <tr>
                    <td>${this.escapeHtml(backup.name)}</td>
                    <td class="file-size">${this.formatSize(backup.size)}</td>
                    <td>${this.formatDate(backup.modified)}</td>
                    <td class="actions">
                        <button class="btn btn-warning btn-sm" onclick="app.restoreBackup('${this.escapeHtml(backup.name)}')">Restore</button>
                        <button class="btn btn-danger btn-sm" onclick="app.deleteBackup('${this.escapeHtml(backup.name)}')">Delete</button>
                    </td>
                </tr>
            `).join('');
        } catch (e) {
            list.innerHTML = `<tr><td colspan="4" class="error-message">Failed to load backups: ${e.message}</td></tr>`;
        }
    },

    async createBackup() {
        const input = document.getElementById('backup-name-input');
        const name = input.value.trim() || null;
        const btn = event.target;

        btn.disabled = true;
        btn.textContent = 'Creating...';

        try {
            await api.createBackup(name);
            input.value = '';
            await this.loadBackups();
        } catch (e) {
            alert('Failed to create backup: ' + e.message);
        } finally {
            btn.disabled = false;
            btn.textContent = 'Create Backup';
        }
    },

    async deleteBackup(filename) {
        if (!confirm(`Delete backup "${filename}"?`)) return;

        try {
            await api.deleteBackup(filename);
            await this.loadBackups();
        } catch (e) {
            alert('Failed to delete backup: ' + e.message);
        }
    },

    async restoreBackup(filename) {
        if (!confirm(`Restore backup "${filename}"? This will replace the current world!`)) return;

        try {
            const result = await api.restoreBackup(filename);
            alert(result.message + (result.output ? '\n\n' + result.output : ''));
        } catch (e) {
            alert('Failed to restore backup: ' + e.message);
        }
    },

    // Utilities
    formatSize(bytes) {
        const units = ['B', 'KB', 'MB', 'GB'];
        let size = bytes;
        let unit = 0;
        while (size >= 1024 && unit < units.length - 1) {
            size /= 1024;
            unit++;
        }
        return `${size.toFixed(1)} ${units[unit]}`;
    },

    formatDate(dateStr) {
        const date = new Date(dateStr);
        return date.toLocaleString();
    }
};

// Initialize app when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    app.init();
    app.setupModUpload();
});
