// GLOBAL STATE
let chart = null;
let areaSeries = null;
let candlestickSeries = null;

let currentChartMode = 'equity'; // 'equity' or 'candles'
let selectedSymbol = 'EURUSD';
let selectedTimeframe = 'M5';
let pollInterval = null;

// Risk & Overlay Cache
let activeTradesCache = [];
let activePriceLines = [];
let lastOpenTradeTickets = null;
let lastAdvisoryId = null;
let previousProfitCache = {};

// Dynamic Broker Asset Mapping Cache
let brokerSymbolsCache = [];

// INIT ON PAGE LOAD
document.addEventListener("DOMContentLoaded", () => {
    // 1. Fetch initial configuration settings
    loadSettings();
    
    // 3. Load active theme from localStorage
    initActiveTheme();
    
    // 4. Load symbols list dynamically and match broker naming suffixes (e.g. Exness "m")
    loadSymbolsList();
    
    // 5. Initial dashboard data fetch
    refreshDashboard();
    
    // 6. Start real-time polling (every 2.5 seconds)
    startPoller();
    
    // 7. Load Advisory feed
    loadAdvisoryFeed();
    
    // 8. Load Forex Economic Calendar & News Bulletins
    refreshMarketIntelligence();
    
    // Set share link input
    const investorUrlInput = document.getElementById("investor-url");
    if (investorUrlInput) {
        investorUrlInput.value = window.location.href;
    }
});



// THEME PALETTE CONTROLLER
function initActiveTheme() {
    const activeTheme = localStorage.getItem("jetro-theme") || "obsidian";
    document.body.className = `theme-${activeTheme}`;
    document.getElementById("theme-select").value = activeTheme;
}

function changeTheme(themeVal) {
    document.body.className = `theme-${themeVal}`;
    localStorage.setItem("jetro-theme", themeVal);
    showToast(`Switched dashboard palette to ${themeVal.toUpperCase()} mode!`, "success");
}

// STATEMENT REPORTS NAVIGATION TRIGGERS
function openReport(type) {
    let reportPath = "open-trades";
    if (type === "history") reportPath = "history";
    if (type === "daily") reportPath = "daily-pl";
    
    const url = `/report/${reportPath}`;
    window.open(url, "_blank");
}

// TOAST NOTIFICATIONS
function showToast(message, type = 'info') {
    const toast = document.getElementById("toast");
    const icon = document.getElementById("toast-icon");
    const msg = document.getElementById("toast-msg");
    
    toast.className = `show ${type}`;
    msg.textContent = message;
    
    if (type === 'success') {
        icon.className = "fa-solid fa-circle-check";
        icon.style.color = "var(--accent-emerald)";
    } else if (type === 'error') {
        icon.className = "fa-solid fa-triangle-exclamation";
        icon.style.color = "var(--accent-rose)";
    } else {
        icon.className = "fa-solid fa-circle-info";
        icon.style.color = "var(--accent-cyan)";
    }
    
    setTimeout(() => {
        toast.className = "";
    }, 4000);
}

// FORMAT CURRENCY
function formatCurrency(value) {
    const num = parseFloat(value);
    if (isNaN(num)) return "$0.00";
    
    const formatted = Math.abs(num).toLocaleString('en-US', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
    
    return `${num < 0 ? '-' : ''}$${formatted}`;
}

function playSound(type) {
    try {
        const AudioContext = window.AudioContext || window.webkitAudioContext;
        if (!AudioContext) return;
        const ctx = new AudioContext();
        
        if (type === 'open') {
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.connect(gain);
            gain.connect(ctx.destination);
            
            osc.type = 'sine';
            osc.frequency.setValueAtTime(523.25, ctx.currentTime); // C5
            gain.gain.setValueAtTime(0.08, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.15);
            osc.start(ctx.currentTime);
            osc.stop(ctx.currentTime + 0.15);
            
            setTimeout(() => {
                const osc2 = ctx.createOscillator();
                const gain2 = ctx.createGain();
                osc2.connect(gain2);
                gain2.connect(ctx.destination);
                osc2.type = 'sine';
                osc2.frequency.setValueAtTime(659.25, ctx.currentTime); // E5
                gain2.gain.setValueAtTime(0.08, ctx.currentTime);
                gain2.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.2);
                osc2.start(ctx.currentTime);
                osc2.stop(ctx.currentTime + 0.2);
            }, 120);
        } else if (type === 'close') {
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.connect(gain);
            gain.connect(ctx.destination);
            
            osc.type = 'triangle';
            osc.frequency.setValueAtTime(587.33, ctx.currentTime); // D5
            gain.gain.setValueAtTime(0.08, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.35);
            osc.start(ctx.currentTime);
            osc.stop(ctx.currentTime + 0.35);
        } else if (type === 'advisory') {
            const notes = [440, 554, 659, 880];
            notes.forEach((freq, i) => {
                setTimeout(() => {
                    const osc = ctx.createOscillator();
                    const gain = ctx.createGain();
                    osc.connect(gain);
                    gain.connect(ctx.destination);
                    osc.type = 'sine';
                    osc.frequency.setValueAtTime(freq, ctx.currentTime);
                    gain.gain.setValueAtTime(0.06, ctx.currentTime);
                    gain.gain.exponentialRampToValueAtTime(0.005, ctx.currentTime + 0.12);
                    osc.start(ctx.currentTime);
                    osc.stop(ctx.currentTime + 0.12);
                }, i * 90);
            });
        }
    } catch (e) {
        console.log("Audio Context blocked or failed:", e);
    }
}



// ----------------------------------------------------
// SETTINGS MANAGEMENT
// ----------------------------------------------------

async function loadSettings() {
    try {
        const tokenInput = document.getElementById("telegram-token");
        if (!tokenInput) {
            // Investors don't load credentials settings
            return;
        }
        const response = await fetch("/api/settings");
        if (response.status === 401 || response.status === 403) {
            console.log("Settings access unauthorized");
            return;
        }
        const data = await response.json();
        
        if (data.success && data.settings) {
            const s = data.settings;
            document.getElementById("telegram-token").value = s.telegram_bot_token || "";
            document.getElementById("telegram-chat").value = s.telegram_chat_id || "";
            document.getElementById("whatsapp-webhook").value = s.whatsapp_webhook || "";
            document.getElementById("telegram-enabled-toggle").checked = s.telegram_enabled || false;
            document.getElementById("whatsapp-enabled-toggle").checked = s.whatsapp_enabled || false;
            document.getElementById("multiplier-val").value = s.risk_multiplier || 1.0;
            
            // Risk Safeguards
            const trailingToggle = document.getElementById("trailing-enabled-toggle");
            if (trailingToggle) trailingToggle.checked = s.trailing_stop_enabled || false;
            const trailingDist = document.getElementById("trailing-distance");
            if (trailingDist) trailingDist.value = s.trailing_stop_distance || 20.0;
            
            const beToggle = document.getElementById("be-enabled-toggle");
            if (beToggle) beToggle.checked = s.break_even_enabled || false;
            const beAct = document.getElementById("be-activation");
            if (beAct) beAct.value = s.break_even_activation || 15.0;
            
            toggleRiskFields();
            
            const mt5LoginEl = document.getElementById("mt5-login");
            if (mt5LoginEl) mt5LoginEl.value = s.mt5_login || "";
            const mt5PassEl = document.getElementById("mt5-password");
            if (mt5PassEl) mt5PassEl.value = s.mt5_password || "";
            const mt5ServerEl = document.getElementById("mt5-server");
            if (mt5ServerEl) mt5ServerEl.value = s.mt5_server || "";
        }
    } catch (e) {
        console.error("Failed to load settings:", e);
    }
}

async function saveAlertSettings() {
    const settingsData = {
        telegram_bot_token: document.getElementById("telegram-token").value,
        telegram_chat_id: document.getElementById("telegram-chat").value,
        whatsapp_webhook: document.getElementById("whatsapp-webhook").value,
        telegram_enabled: document.getElementById("telegram-enabled-toggle").checked,
        whatsapp_enabled: document.getElementById("whatsapp-enabled-toggle").checked,
        risk_multiplier: parseFloat(document.getElementById("multiplier-val").value) || 1.0
    };
    
    try {
        const response = await fetch("/api/settings", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(settingsData)
        });
        const data = await response.json();
        
        if (data.success) {
            showToast("Notification settings saved successfully!", "success");
        } else {
            showToast(data.message || "Failed to save settings.", "error");
        }
    } catch (e) {
        showToast("Error connecting to server to save settings.", "error");
    }
}

function toggleRiskFields() {
    const trailingToggle = document.getElementById("trailing-enabled-toggle");
    const beToggle = document.getElementById("be-enabled-toggle");
    
    const trailingGroup = document.getElementById("trailing-distance-group");
    const beGroup = document.getElementById("be-activation-group");
    
    if (trailingGroup && trailingToggle) {
        trailingGroup.style.display = trailingToggle.checked ? "block" : "none";
    }
    if (beGroup && beToggle) {
        beGroup.style.display = beToggle.checked ? "block" : "none";
    }
}

async function saveRiskSettings() {
    const trailingToggle = document.getElementById("trailing-enabled-toggle");
    const trailingDist = document.getElementById("trailing-distance");
    const beToggle = document.getElementById("be-enabled-toggle");
    const beAct = document.getElementById("be-activation");
    
    if (!trailingToggle || !beToggle) return;
    
    const settingsData = {
        trailing_stop_enabled: trailingToggle.checked,
        trailing_stop_distance: parseFloat(trailingDist.value) || 20.0,
        break_even_enabled: beToggle.checked,
        break_even_activation: parseFloat(beAct.value) || 15.0
    };
    
    showToast("Saving risk safeguards settings...", "info");
    
    try {
        const response = await fetch("/api/settings", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(settingsData)
        });
        const data = await response.json();
        
        if (data.success) {
            showToast("Automated risk settings saved successfully!", "success");
        } else {
            showToast(data.message || "Failed to save risk safeguards.", "error");
        }
    } catch (e) {
        showToast("Error connecting to server to save risk safeguards.", "error");
    }
}

async function saveMT5Settings() {
    const login = document.getElementById("mt5-login").value.trim();
    const password = document.getElementById("mt5-password").value;
    const server = document.getElementById("mt5-server").value.trim();
    
    if (!login || !password || !server) {
        showToast("Please fill in all MT5 connection fields.", "error");
        return;
    }
    
    const settingsData = {
        mt5_login: login,
        mt5_password: password,
        mt5_server: server
    };
    
    showToast("Connecting to MT5 with new credentials...", "info");
    
    try {
        const response = await fetch("/api/settings", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(settingsData)
        });
        const data = await response.json();
        
        if (data.success) {
            showToast(data.message || "MT5 Connection updated successfully!", "success");
            refreshDashboard();
        } else {
            showToast(data.message || "Failed to update MT5 connection.", "error");
        }
    } catch (e) {
        showToast("Error connecting to server to save MT5 config.", "error");
    }
}

async function updateMultiplier(val) {
    const multiplier = parseFloat(val) || 1.0;
    try {
        await fetch("/api/settings", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ risk_multiplier: multiplier })
        });
        showToast(`Risk multiplier adjusted to ${multiplier}x`, "info");
    } catch (e) {
        console.error(e);
    }
}

async function sendTestNotification(channel) {
    const payload = {
        channel: channel,
        telegram_bot_token: document.getElementById("telegram-token").value,
        telegram_chat_id: document.getElementById("telegram-chat").value,
        whatsapp_webhook: document.getElementById("whatsapp-webhook").value
    };
    
    showToast(`Sending test alert to ${channel}...`, "info");
    
    try {
        const response = await fetch("/api/send-test-notification", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        const data = await response.json();
        
        if (data.success) {
            showToast(`Test ${channel} notification sent!`, "success");
        } else {
            showToast(data.message || "Test notification failed.", "error");
        }
    } catch (e) {
        showToast("Error sending test notification request.", "error");
    }
}

// ----------------------------------------------------
// DATA REFRESH ENGINE (POLLER)
// ----------------------------------------------------

function startPoller() {
    if (pollInterval) clearInterval(pollInterval);
    
    pollInterval = setInterval(() => {
        refreshDashboard();
    }, 2500);
}

async function refreshDashboard() {
    try {
        // Fetch core account info & update status badge
        const accountRes = await fetch("/api/account");
        const account = await accountRes.json();
        
        const dot = document.getElementById("connection-dot");
        const text = document.getElementById("connection-text");
        
        if (account.success) {
            dot.className = "status-dot online";
            text.textContent = "MT5 CONNECTED";
            text.style.color = "var(--accent-emerald)";
            
            document.getElementById("header-login").textContent = account.login;
        } else {
            dot.className = "status-dot offline";
            text.textContent = account.error || "TERMINAL OFFLINE";
            text.style.color = "var(--accent-rose)";
        }
        
        // Fetch stats / advanced analytics
        let url = "/api/advanced-analytics";
        
        const analyticsRes = await fetch(url);
        const analytics = await analyticsRes.json();
        
        if (analytics.success) {
            updateMetricsDashboard(analytics);
            
            // Re-render chart if mode is equity curve
            if (currentChartMode === 'equity') {
                updateEquityChart(analytics.equity_curve);
            }
        }
        
        // Fetch active positions
        refreshActivePositions();
        
        // Fetch portfolio data
        refreshPortfolio();
        
        // Fetch Market economic events & live news
        refreshMarketIntelligence();
        
    } catch (e) {
        console.error("Dashboard poll failed:", e);
    }
}

// UPDATE CORE METRICS BINDINGS
function updateMetricsDashboard(data) {
    const netProfEl = document.getElementById("stat-net-profit");
    netProfEl.textContent = formatCurrency(data.net_profit);
    if (data.net_profit >= 0) {
        netProfEl.style.color = "var(--accent-emerald)";
    } else {
        netProfEl.style.color = "var(--accent-rose)";
    }
    
    const deposits = data.balance - data.net_profit;
    const roi = deposits > 0 ? (data.net_profit / deposits) * 100 : 0;
    
    const netPctEl = document.getElementById("stat-net-profit-pct");
    netPctEl.className = roi >= 0 ? "metric-change change-up" : "metric-change change-down";
    netPctEl.innerHTML = `<i class="fa-solid ${roi >= 0 ? 'fa-arrow-trend-up' : 'fa-arrow-trend-down'}"></i> ${roi.toFixed(1)}% ROI`;
    
    document.getElementById("stat-balance").textContent = formatCurrency(data.balance);
    const equityEl = document.getElementById("stat-equity");
    equityEl.textContent = formatCurrency(data.equity);
    
    const floatingProfit = data.equity - data.balance;
    if (floatingProfit > 0.01) {
        equityEl.style.color = "var(--accent-emerald)";
    } else if (floatingProfit < -0.01) {
        equityEl.style.color = "var(--accent-rose)";
    } else {
        equityEl.style.color = "var(--text-primary)";
    }
    
    document.getElementById("stat-win-rate").textContent = `${data.win_rate}%`;
    document.getElementById("stat-total-trades").textContent = data.total_trades;
    
    document.getElementById("stat-profit-factor").textContent = data.profit_factor.toFixed(2);
    document.getElementById("stat-sharpe").textContent = data.sharpe_ratio.toFixed(2);
    
    const sortinoEl = document.getElementById("stat-sortino");
    if (sortinoEl) {
        sortinoEl.textContent = data.sortino_ratio.toFixed(2);
    }
    
    const drawdownEl = document.getElementById("stat-drawdown");
    if (drawdownEl) {
        drawdownEl.textContent = `${data.max_drawdown}%`;
    }
    
    const recoveryEl = document.getElementById("stat-recovery");
    if (recoveryEl) {
        recoveryEl.textContent = data.recovery_factor.toFixed(2);
    }

    // Total Deposit
    const depositsEl = document.getElementById("stat-total-deposits");
    if (depositsEl) {
        depositsEl.textContent = formatCurrency(data.total_deposits || 0);
    }

    // Total Withdrawal
    const withdrawalsEl = document.getElementById("stat-total-withdrawals");
    if (withdrawalsEl) {
        withdrawalsEl.textContent = formatCurrency(data.total_withdrawals || 0);
    }
    
    updateSymbolDistributionList(data.symbol_distribution, data.total_trades);
    updateDailyList(data.daily_profit);
}

// UPDATE ACTIVE SYMBOL DISTRIBUTION VIEW
function updateSymbolDistributionList(dist, totalTrades) {
    const container = document.getElementById("symbol-dist-container");
    if (!dist || dist.length === 0) {
        container.innerHTML = `<div class="empty-state" style="padding: 1.5rem 0;">No trades recorded.</div>`;
        return;
    }
    
    dist.sort((a, b) => b.trades - a.trades);
    
    let html = '';
    dist.forEach(item => {
        const pct = totalTrades > 0 ? (item.trades / totalTrades) * 100 : 0;
        html += `
            <div class="symbol-item">
                <span class="symbol-name">${item.symbol}</span>
                <div style="display: flex; align-items: center; gap: 0.75rem;">
                    <div class="symbol-bar-container">
                        <div class="symbol-bar" style="width: ${pct}%"></div>
                    </div>
                    <span class="symbol-percentage">${pct.toFixed(0)}% (${item.trades})</span>
                </div>
            </div>
        `;
    });
    
    container.innerHTML = html;
}

// UPDATE ACTIVE DAILY SUMMARY
function updateDailyList(dailyData) {
    const container = document.getElementById("daily-pl-container");
    if (!dailyData || dailyData.length === 0) {
        container.innerHTML = `<div class="empty-state" style="padding: 1.5rem 0;">No daily trading details.</div>`;
        return;
    }
    
    const reversed = [...dailyData].reverse();
    
    let html = '';
    reversed.forEach(item => {
        const profit = parseFloat(item.profit);
        const profitClass = profit >= 0 ? "profit-positive" : "profit-negative";
        html += `
            <div class="symbol-item">
                <span style="font-size: 0.8rem; font-weight: 600; color: var(--text-secondary);"><i class="fa-regular fa-calendar" style="margin-right: 0.3rem;"></i> ${item.date}</span>
                <span class="profit-text ${profitClass}">${profit >= 0 ? '+' : ''}${formatCurrency(profit)}</span>
            </div>
        `;
    });
    
    container.innerHTML = html;
}

// REFRESH ACTIVE POSITIONS
async function refreshActivePositions() {
    try {
        const response = await fetch("/api/open-trades");
        const data = await response.json();
        
        const tbody = document.getElementById("open-positions-tbody");
        document.getElementById("open-trades-count").textContent = data.trades ? data.trades.length : 0;
        
        let floatingPl = 0;
        
        const currentTickets = data.trades ? data.trades.map(t => t.ticket) : [];
        if (lastOpenTradeTickets !== null) {
            const opened = currentTickets.filter(x => !lastOpenTradeTickets.includes(x));
            const closed = lastOpenTradeTickets.filter(x => !currentTickets.includes(x));
            
            if (opened.length > 0) playSound('open');
            else if (closed.length > 0) playSound('close');
        }
        lastOpenTradeTickets = currentTickets;
        activeTradesCache = data.trades || [];
        
        if (currentChartMode === 'candles') {
            updateChartPriceLines();
        }
        
        if (data.success && data.trades && data.trades.length > 0) {
            // Clean up closed tickets from cache
            const cacheKeys = Object.keys(previousProfitCache);
            cacheKeys.forEach(k => {
                const ticketNum = parseInt(k);
                if (!currentTickets.includes(ticketNum)) {
                    delete previousProfitCache[k];
                }
            });

            let html = '';
            data.trades.forEach(t => {
                floatingPl += parseFloat(t.profit);
                const isBuy = t.type === 'BUY';
                const pClass = t.profit >= 0 ? 'profit-positive' : 'profit-negative';
                
                // Tick Flash logic
                let flashClass = '';
                const prevVal = previousProfitCache[t.ticket];
                if (prevVal !== undefined) {
                    if (t.profit > prevVal) {
                        flashClass = 'flash-up';
                    } else if (t.profit < prevVal) {
                        flashClass = 'flash-down';
                    }
                }
                previousProfitCache[t.ticket] = t.profit;
                
                const shareText = encodeURIComponent(
                    `📊 *Jetro AI Live Trade Alert*\n\n` +
                    `🌐 *Symbol:* ${t.symbol}\n` +
                    `🔔 *Action:* ${t.type} (Volume: ${t.volume})\n` +
                    `📥 *Entry Price:* ${t.open_price.toFixed(5)}\n` +
                    `🛡️ *Stop Loss:* ${t.sl ? t.sl.toFixed(5) : 'None'}\n` +
                    `🎯 *Target Price:* ${t.tp ? t.tp.toFixed(5) : 'None'}\n\n` +
                    `🔗 Join my copy trader: http://127.0.0.1:5000/`
                );
                
                html += `
                    <tr class="${flashClass}">
                        <td>#${t.ticket}</td>
                        <td style="font-weight: 700;">${t.symbol}</td>
                        <td><span class="badge-type ${isBuy ? 'badge-buy' : 'badge-sell'}">${t.type}</span></td>
                        <td><strong>${t.volume.toFixed(2)}</strong></td>
                        <td>${t.open_price.toFixed(5)}</td>
                        <td>${t.current_price.toFixed(5)}</td>
                        <td style="color: var(--accent-rose); font-weight: 600;">${t.sl ? t.sl.toFixed(5) : '-'}</td>
                        <td style="color: var(--accent-emerald); font-weight: 600;">${t.tp ? t.tp.toFixed(5) : '-'}</td>
                        <td><span class="profit-text ${pClass}">${t.profit >= 0 ? '+' : ''}${formatCurrency(t.profit)}</span></td>
                        <td>
                            <div style="display: flex; gap: 0.5rem;" onclick="event.stopPropagation()">
                                <a href="https://api.whatsapp.com/send?text=${shareText}" target="_blank" class="chart-btn btn-mini" style="background: rgba(16, 185, 129, 0.15); border-color: rgba(16, 185, 129, 0.3); color: var(--accent-emerald);"><i class="fa-brands fa-whatsapp"></i> Broadcast</a>
                                <button onclick="copyToClipboard('${shareText}')" class="chart-btn btn-mini" style="background: rgba(6, 182, 212, 0.15); border-color: rgba(6, 182, 212, 0.3); color: var(--accent-cyan);"><i class="fa-solid fa-share-nodes"></i> Copy Msg</button>
                            </div>
                        </td>
                    </tr>
                `;
            });
            
            tbody.innerHTML = html;
        } else {
            // Reset cache if no positions are active
            previousProfitCache = {};
            tbody.innerHTML = `
                <tr>
                    <td colspan="10" class="empty-state">
                        <i class="fa-regular fa-folder-open"></i>
                        <p>No active positions open at the moment.</p>
                    </td>
                </tr>
            `;
        }
        
        const floatPlEl = document.getElementById("live-floating-pl");
        floatPlEl.textContent = formatCurrency(floatingPl);
        floatPlEl.className = `profit-text ${floatingPl >= 0 ? 'profit-positive' : 'profit-negative'}`;
        
    } catch (e) {
        console.error("Open trades update failed:", e);
    }
}

// COPY TO CLIPBOARD HELPER
function copyToClipboard(encodedText) {
    const decoded = decodeURIComponent(encodedText);
    navigator.clipboard.writeText(decoded).then(() => {
        showToast("Trade alert template copied to clipboard!", "success");
    }).catch(err => {
        showToast("Failed to copy message text.", "error");
    });
}

// ----------------------------------------------------
// CLOSED TRADE HISTORY WITH RANGE DATE FILTERS
// ----------------------------------------------------

async function refreshHistory() {
    try {
        let url = "/api/history";
        
        const response = await fetch(url);
        const data = await response.json();
        
        const tbody = document.getElementById("history-tbody");
        
        if (data.success && data.history && data.history.length > 0) {
            let html = '';
            const recent = data.history.slice(0, 15);
            
            recent.forEach(h => {
                const isBuy = h.type === 'BUY';
                const pClass = h.profit >= 0 ? 'profit-positive' : 'profit-negative';
                
                html += `
                    <tr>
                        <td>#${h.ticket}</td>
                        <td style="font-weight: 700;">${h.symbol}</td>
                        <td><span class="badge-type ${isBuy ? 'badge-buy' : 'badge-sell'}">${h.type}</span></td>
                        <td>${h.volume.toFixed(2)}</td>
                        <td>${h.price.toFixed(5)}</td>
                        <td style="color: var(--text-secondary); font-size: 0.8rem;">${h.time}</td>
                        <td><span class="profit-text ${pClass}">${h.profit >= 0 ? '+' : ''}${formatCurrency(h.profit)}</span></td>
                    </tr>
                `;
            });
            
            tbody.innerHTML = html;
        } else {
            tbody.innerHTML = `
                <tr>
                    <td colspan="7" class="empty-state">
                        <i class="fa-solid fa-rectangle-xmark"></i>
                        <p>No completed trades within selected dates.</p>
                    </td>
                </tr>
            `;
        }
    } catch (e) {
        console.error("History fetch failed:", e);
    }
}

// Fetch history immediately on load
refreshHistory();

// ----------------------------------------------------
// DYNAMIC SYMBOLS DROPDOWN & WATCHLIST ALIGNMENT
// ----------------------------------------------------

async function loadSymbolsList() {
    try {
        const response = await fetch("/api/symbols");
        const data = await response.json();
        
        if (data.success && data.symbols) {
            brokerSymbolsCache = data.symbols; // Cache broker names! (e.g. Exness EURUSDm)
            
            const select = document.getElementById("candle-symbol-select");
            select.innerHTML = '';
            
            data.symbols.forEach(sym => {
                const opt = document.createElement("option");
                opt.value = sym;
                opt.textContent = sym;
                
                // Select EURUSDm or EURUSD by default
                if (sym.includes('EURUSD')) opt.selected = true;
                select.appendChild(opt);
            });
            
            // Re-render sparklines now that the broker names are cached
            loadWatchlistSparklines();
        }
    } catch (e) {
        console.error("Symbols fetch failed:", e);
    }
}

// ----------------------------------------------------
// WATCHLIST SPARKLINE GENERATOR (Broker Naming Aware)
// ----------------------------------------------------

async function loadWatchlistSparklines() {
    const rootSymbols = ['EURUSD', 'GBPUSD', 'USDJPY', 'XAUUSD'];
    const container = document.getElementById("watchlist-container");
    if (!container) return;
    
    if (container.querySelector(".empty-state")) {
        container.innerHTML = '';
    }
    
    for (const root of rootSymbols) {
        // Resolve actual broker symbol name from cache (e.g. EURUSD -> EURUSDm)
        const sym = brokerSymbolsCache.find(s => s.includes(root)) || root;
        
        try {
            const response = await fetch(`/api/candles?symbol=${sym}&timeframe=M5&count=20`);
            const data = await response.json();
            
            if (data.success && data.candles && data.candles.length > 0) {
                const prices = data.candles.map(c => c.close);
                const currentPrice = prices[prices.length - 1];
                const priorPrice = prices[0];
                const isUp = currentPrice >= priorPrice;
                const priceClass = isUp ? 'profit-positive' : 'profit-negative';
                const pctChange = ((currentPrice - priorPrice) / priorPrice) * 100;
                
                let itemDiv = document.getElementById(`watchlist-item-${root}`);
                if (!itemDiv) {
                    itemDiv = document.createElement("div");
                    itemDiv.id = `watchlist-item-${root}`;
                    itemDiv.className = "symbol-item";
                    itemDiv.style.padding = "0.75rem 1rem";
                    container.appendChild(itemDiv);
                }
                
                itemDiv.innerHTML = `
                    <div style="width: 25%">
                        <span style="font-weight: 700; font-size: 0.85rem;">${sym}</span>
                        <p style="font-size: 0.65rem; color: var(--text-muted);">M5 Feed</p>
                    </div>
                    <div style="width: 45%; text-align: center;">
                        <canvas id="sparkline-${root}" width="95" height="25" style="opacity: 0.95; display: inline-block;"></canvas>
                    </div>
                    <div style="width: 30%; text-align: right;">
                        <span class="profit-text ${priceClass}" style="font-size: 0.85rem; font-weight: 700;">
                            ${sym.includes('JPY') ? currentPrice.toFixed(2) : currentPrice.toFixed(5)}
                        </span>
                        <p style="font-size: 0.65rem; font-weight: 600; color: ${isUp ? 'var(--accent-emerald)' : 'var(--accent-rose)'};">
                            ${isUp ? '▲' : '▼'} ${pctChange.toFixed(2)}%
                        </p>
                    </div>
                `;
                
                // Draw sparkline canvas
                drawSparkline(`sparkline-${root}`, prices, isUp);
            }
        } catch (e) {
            console.error(`Failed to load sparkline for ${root}:`, e);
        }
    }
}

function drawSparkline(canvasId, data, isUp) {
    const canvas = document.getElementById(canvasId);
    if (!canvas) return;
    
    const ctx = canvas.getContext("2d");
    const width = canvas.width;
    const height = canvas.height;
    
    ctx.clearRect(0, 0, width, height);
    
    const min = Math.min(...data);
    const max = Math.max(...data);
    const range = max - min;
    
    ctx.beginPath();
    ctx.lineWidth = 2.0;
    ctx.strokeStyle = isUp ? '#10b981' : '#f43f5e';
    ctx.lineJoin = 'round';
    ctx.lineCap = 'round';
    
    for (let i = 0; i < data.length; i++) {
        const x = (i / (data.length - 1)) * width;
        const y = height - 2 - ((data[i] - min) / (range || 1)) * (height - 6);
        
        if (i === 0) {
            ctx.moveTo(x, y);
        } else {
            ctx.lineTo(x, y);
        }
    }
    ctx.stroke();
    
    const grad = ctx.createLinearGradient(0, 0, 0, height);
    grad.addColorStop(0, isUp ? 'rgba(16, 185, 129, 0.18)' : 'rgba(244, 63, 94, 0.18)');
    grad.addColorStop(1, 'rgba(0, 0, 0, 0)');
    
    ctx.lineTo(width, height);
    ctx.lineTo(0, height);
    ctx.fillStyle = grad;
    ctx.fill();
}

// ----------------------------------------------------
// LIVE TRADING ADVISORY SIGNALS & UPLOADS
// ----------------------------------------------------

let editingAdvisoryId = null;
let advisoryListCache = [];

async function loadAdvisoryFeed() {
    try {
        const response = await fetch("/api/advisory");
        const data = await response.json();
        
        const feed = document.getElementById("advisory-feed");
        
        if (data.success && data.advisories && data.advisories.length > 0) {
            advisoryListCache = data.advisories;
            
            // Audio alert trigger on new advisory broadcast
            const latestId = data.advisories[0].id;
            if (lastAdvisoryId !== null && latestId > lastAdvisoryId) {
                playSound('advisory');
            }
            lastAdvisoryId = latestId;
            
            let html = '';
            const isAdmin = document.getElementById("advisory-title") !== null;
            
            data.advisories.forEach(adv => {
                html += `
                    <div id="advisory-card-${adv.id}" style="background: rgba(15, 23, 42, 0.55); padding: 0.85rem; border-radius: 8px; border: 1px solid rgba(255,255,255,0.03); position: relative; margin-bottom: 0.25rem;">
                        <div style="display: flex; justify-content: space-between; margin-bottom: 0.4rem;">
                            <strong style="font-size: 0.8rem; font-family: var(--font-heading); color: #ffffff;">${adv.title}</strong>
                            <span style="font-size: 0.65rem; color: var(--text-muted);">${adv.timestamp}</span>
                        </div>
                        <p style="font-size: 0.75rem; color: var(--text-secondary); line-height: 1.4; margin-bottom: 0.5rem; white-space: pre-wrap;">${adv.text}</p>
                `;
                
                if (adv.is_signal) {
                    html += `
                        <div style="background: rgba(139, 92, 246, 0.08); border: 1px dashed rgba(139, 92, 246, 0.3); padding: 0.75rem; border-radius: 6px; margin: 0.5rem 0; font-size: 0.75rem;">
                            <strong style="color: var(--accent-violet);"><i class="fa-solid fa-signal"></i> ACTIVE SIGNAL ALERT</strong>
                            <div style="margin-top: 0.35rem; display: flex; flex-direction: column; gap: 0.2rem;">
                                <div><span style="color: var(--text-secondary);">Asset/Direction:</span> <strong>${adv.signal_type} ${adv.signal_symbol}</strong></div>
                                <div><span style="color: var(--text-secondary);">Lots/Volume:</span> <strong>${adv.signal_volume} lots</strong></div>
                                <div>
                                    <span style="color: var(--text-secondary); margin-right: 0.75rem;">SL: <strong style="color: var(--accent-rose);">${adv.signal_sl > 0 ? adv.signal_sl.toFixed(5) : 'None'}</strong></span>
                                    <span>TP: <strong style="color: var(--accent-emerald);">${adv.signal_tp > 0 ? adv.signal_tp.toFixed(5) : 'None'}</strong></span>
                                </div>
                            </div>
                            <button class="btn-primary btn-mini" style="margin-top: 0.6rem; border-radius: 4px; padding: 0.35rem 0.75rem; font-size: 0.7rem; background: var(--accent-violet); border-color: var(--accent-violet); width: auto; height: auto;" onclick="copyTradeSignal('${adv.signal_symbol}', '${adv.signal_type}', ${adv.signal_volume}, ${adv.signal_sl}, ${adv.signal_tp})">
                                <i class="fa-solid fa-bolt"></i> Copy Trade Signal
                            </button>
                        </div>
                    `;
                }
                
                if (adv.audio_url) {
                    html += `
                        <div style="margin-top: 0.5rem;">
                            <audio controls style="width: 100%; height: 26px; border-radius: 4px; outline: none; background: #0f172a;">
                                <source src="${adv.audio_url}" type="audio/mpeg">
                                Your browser does not support the audio element.
                            </audio>
                        </div>
                    `;
                }
                
                if (isAdmin) {
                    html += `
                        <div style="display: flex; gap: 0.5rem; justify-content: flex-end; margin-top: 0.75rem; border-top: 1px solid rgba(255, 255, 255, 0.05); padding-top: 0.5rem;">
                            <button class="chart-btn btn-mini" style="background: rgba(139, 92, 246, 0.15); border-color: rgba(139, 92, 246, 0.3); color: var(--accent-violet);" onclick="editAdvisory(${adv.id})"><i class="fa-solid fa-pen-to-square"></i> Edit</button>
                            <button class="chart-btn btn-mini" style="background: rgba(244, 63, 94, 0.15); border-color: rgba(244, 63, 94, 0.3); color: var(--accent-rose);" onclick="deleteAdvisory(${adv.id})"><i class="fa-solid fa-trash"></i> Delete</button>
                        </div>
                    `;
                }
                
                html += `</div>`;
            });
            feed.innerHTML = html;
        } else {
            feed.innerHTML = `
                <div class="empty-state" style="padding: 1rem 0;">
                    <i class="fa-solid fa-microphone-slash" style="opacity: 0.5;"></i>
                    <p>No live advisory broadcasts available yet.</p>
                </div>
            `;
        }
    } catch (e) {
        console.error("Advisory load failed:", e);
    }
}

function toggleSignalFields(checked) {
    const grid = document.getElementById("signal-fields-grid");
    if (grid) {
        grid.style.display = checked ? "grid" : "none";
    }
}

function editAdvisory(id) {
    const adv = advisoryListCache.find(x => x.id === id);
    if (!adv) return;
    
    editingAdvisoryId = id;
    
    document.getElementById("advisory-title").value = adv.title;
    document.getElementById("advisory-text").value = adv.text;
    
    const isSignalBox = document.getElementById("advisory-is-signal");
    if (isSignalBox) {
        isSignalBox.checked = adv.is_signal || false;
        toggleSignalFields(adv.is_signal || false);
        
        if (adv.is_signal) {
            document.getElementById("signal-symbol").value = adv.signal_symbol || "EURUSD";
            document.getElementById("signal-type").value = adv.signal_type || "BUY";
            document.getElementById("signal-volume").value = adv.signal_volume || "0.01";
            document.getElementById("signal-sl").value = adv.signal_sl || "";
            document.getElementById("signal-tp").value = adv.signal_tp || "";
        }
    }
    
    document.getElementById("advisory-form-title").innerHTML = `<i class="fa-solid fa-microphone-lines"></i> Edit Advisory #${id}`;
    document.getElementById("advisory-submit-btn").innerHTML = `<i class="fa-solid fa-circle-check"></i> Save Changes`;
    
    const cancelBtn = document.getElementById("advisory-cancel-btn");
    if (cancelBtn) cancelBtn.style.display = "inline-block";
    
    const audioIndicator = document.getElementById("edit-audio-indicator");
    if (audioIndicator) {
        audioIndicator.style.display = adv.audio_url ? "block" : "none";
        document.getElementById("remove-audio-checkbox").checked = false;
    }
    
    document.getElementById("advisory-title").focus();
    showToast(`Editing advisory: "${adv.title}"`, "info");
}

function cancelAdvisoryEdit() {
    editingAdvisoryId = null;
    
    document.getElementById("advisory-title").value = "";
    document.getElementById("advisory-text").value = "";
    document.getElementById("advisory-audio").value = "";
    
    const isSignalBox = document.getElementById("advisory-is-signal");
    if (isSignalBox) {
        isSignalBox.checked = false;
        toggleSignalFields(false);
        document.getElementById("signal-symbol").value = "EURUSD";
        document.getElementById("signal-type").value = "BUY";
        document.getElementById("signal-volume").value = "0.01";
        document.getElementById("signal-sl").value = "";
        document.getElementById("signal-tp").value = "";
    }
    
    document.getElementById("advisory-form-title").innerHTML = `<i class="fa-solid fa-microphone"></i> Broadcast New Advisory`;
    document.getElementById("advisory-submit-btn").innerHTML = `<i class="fa-solid fa-paper-plane"></i> Publish Advisory`;
    
    const cancelBtn = document.getElementById("advisory-cancel-btn");
    if (cancelBtn) cancelBtn.style.display = "none";
    
    const audioIndicator = document.getElementById("edit-audio-indicator");
    if (audioIndicator) audioIndicator.style.display = "none";
    
    showToast("Advisory editing cancelled.", "info");
}

async function deleteAdvisory(id) {
    if (!confirm("Are you sure you want to permanently delete this advisory broadcast?")) {
        return;
    }
    
    showToast("Deleting advisory broadcast...", "info");
    
    try {
        const response = await fetch(`/api/advisory/${id}`, {
            method: "DELETE"
        });
        const data = await response.json();
        
        if (data.success) {
            showToast("Advisory broadcast deleted successfully!", "success");
            loadAdvisoryFeed();
            
            if (editingAdvisoryId === id) {
                cancelAdvisoryEdit();
            }
        } else {
            showToast(data.error || "Failed to delete advisory.", "error");
        }
    } catch (e) {
        showToast("Error sending delete request.", "error");
    }
}

async function broadcastAdvisory() {
    const title = document.getElementById("advisory-title").value.trim();
    const text = document.getElementById("advisory-text").value.trim();
    const audioInput = document.getElementById("advisory-audio");
    
    if (!title && !text) {
        showToast("Please provide at least a title or details for the Advisory call.", "error");
        return;
    }
    
    const formData = new FormData();
    formData.append("title", title || "Live Trading Advisory call");
    formData.append("text", text);
    
    // Add signal parameters if signal checked
    const isSignalBox = document.getElementById("advisory-is-signal");
    if (isSignalBox && isSignalBox.checked) {
        formData.append("is_signal", "true");
        formData.append("signal_symbol", document.getElementById("signal-symbol").value);
        formData.append("signal_type", document.getElementById("signal-type").value);
        formData.append("signal_volume", document.getElementById("signal-volume").value);
        formData.append("signal_sl", document.getElementById("signal-sl").value || "0.0");
        formData.append("signal_tp", document.getElementById("signal-tp").value || "0.0");
    } else {
        formData.append("is_signal", "false");
    }
    
    if (audioInput.files && audioInput.files[0]) {
        formData.append("audio", audioInput.files[0]);
    }
    
    const removeAudioBox = document.getElementById("remove-audio-checkbox");
    if (removeAudioBox && removeAudioBox.checked) {
        formData.append("remove_audio", "true");
    }
    
    let url = "/api/advisory";
    let method = "POST";
    
    if (editingAdvisoryId !== null) {
        url = `/api/advisory/${editingAdvisoryId}`;
        method = "PUT";
        showToast("Saving advisory changes...", "info");
    } else {
        showToast("Publishing live trading advisory signal...", "info");
    }
    
    try {
        const response = await fetch(url, {
            method: method,
            body: formData
        });
        const data = await response.json();
        
        if (data.success) {
            showToast(editingAdvisoryId !== null ? "Changes saved successfully!" : "Trading advisory published successfully!", "success");
            cancelAdvisoryEdit();
            loadAdvisoryFeed();
        } else {
            showToast(data.error || "Advisory broadcast failed.", "error");
        }
    } catch (e) {
        showToast("Error uploading advisory request.", "error");
    }
}

async function copyTradeSignal(symbol, type, volume, sl, tp) {
    const msg = `⚡ Copy Trade Signal Alert:\n\nAsset: ${symbol}\nType: ${type}\nVolume: ${volume} Lots\n\nExecute trade on your MT5 Terminal?`;
    if (!confirm(msg)) {
        return;
    }
    
    showToast(`Sending copy trade order for ${symbol}...`, "info");
    
    try {
        const response = await fetch("/api/trade/execute", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ symbol, type, volume, sl, tp })
        });
        const data = await response.json();
        
        if (data.success) {
            showToast(data.message || "Trade executed successfully!", "success");
            refreshDashboard();
        } else {
            showToast(data.error || "Execution failed.", "error");
        }
    } catch (e) {
        showToast("Network error executing copy trade.", "error");
    }
}

async function executeQuickTrade(action) {
    const symbol = document.getElementById("quick-order-symbol").value;
    const volume = parseFloat(document.getElementById("quick-order-volume").value) || 0.01;
    const slInput = document.getElementById("quick-order-sl").value;
    const tpInput = document.getElementById("quick-order-tp").value;
    
    const sl = slInput ? parseFloat(slInput) : 0.0;
    const tp = tpInput ? parseFloat(tpInput) : 0.0;
    
    const confirmMsg = `⚡ One-Click Trade Confirmation:\n\nAction: ${action}\nAsset: ${symbol}\nVolume: ${volume} Lots\nSL: ${sl > 0 ? sl : 'None'}\nTP: ${tp > 0 ? tp : 'None'}\n\nExecute trade on MT5?`;
    if (!confirm(confirmMsg)) {
        return;
    }
    
    showToast(`Submitting ${action} quick order for ${symbol}...`, "info");
    
    try {
        const response = await fetch("/api/admin/trade", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ symbol, type: action, volume, sl, tp })
        });
        const data = await response.json();
        
        if (data.success) {
            playSound('open');
            showToast(data.message || `Successfully executed ${action} Quick Order!`, "success");
            
            // Clear SL and TP values
            document.getElementById("quick-order-sl").value = "";
            document.getElementById("quick-order-tp").value = "";
            
            refreshDashboard();
        } else {
            showToast(data.error || "Quick order execution failed.", "error");
        }
    } catch (e) {
        showToast("Network error executing quick order.", "error");
    }
}

// ----------------------------------------------------
// CHARTING ENGINE (TRADINGVIEW LIGHTWEIGHT CHARTS)
// ----------------------------------------------------

function initLightweightChart() {
    const container = document.getElementById("liveChartContainer");
    container.innerHTML = ''; // Clear prior mounts
    
    // Explicit safety width & height values to prevent collapse on load
    const computedWidth = container.clientWidth || 800;
    const computedHeight = container.clientHeight || 380;
    
    chart = LightweightCharts.createChart(container, {
        width: computedWidth,
        height: computedHeight,
        layout: {
            background: { type: 'solid', color: 'transparent' },
            textColor: '#94a3b8',
            fontFamily: 'Outfit, sans-serif',
            fontSize: 11
        },
        grid: {
            vertLines: { color: 'rgba(255, 255, 255, 0.02)' },
            horzLines: { color: 'rgba(255, 255, 255, 0.02)' }
        },
        crosshair: {
            mode: LightweightCharts.CrosshairMode.Normal,
            vertLine: {
                color: 'rgba(6, 182, 212, 0.35)',
                width: 1,
                style: 3
            },
            horzLine: {
                color: 'rgba(6, 182, 212, 0.35)',
                width: 1,
                style: 3
            }
        },
        rightPriceScale: {
            borderColor: 'rgba(255, 255, 255, 0.06)',
            visible: true
        },
        timeScale: {
            borderColor: 'rgba(255, 255, 255, 0.06)',
            timeVisible: true,
            secondsVisible: false
        }
    });

    const resizeObserver = new ResizeObserver(entries => {
        if (entries.length === 0 || !chart) return;
        const { width, height } = entries[0].contentRect;
        chart.resize(width, height);
    });
    resizeObserver.observe(container);
}

function switchChartMode(mode) {
    currentChartMode = mode;
    
    document.getElementById("btn-chart-equity").classList.toggle("active", mode === 'equity');
    document.getElementById("btn-chart-candles").classList.toggle("active", mode === 'candles');
    
    document.getElementById("candle-symbol-select").style.display = mode === 'candles' ? 'inline-block' : 'none';
    document.getElementById("candle-tf-select").style.display = mode === 'candles' ? 'inline-block' : 'none';
    
    const titleEl = document.getElementById("chart-display-title");
    
    if (mode === 'equity') {
        titleEl.innerHTML = `<i class="fa-solid fa-chart-area text-muted"></i> Equity Growth Curve`;
        refreshDashboard();
    } else {
        selectedSymbol = document.getElementById("candle-symbol-select").value;
        selectedTimeframe = document.getElementById("candle-tf-select").value;
        titleEl.innerHTML = `<i class="fa-solid fa-candles text-muted"></i> Live Price Action: <strong>${selectedSymbol} (${selectedTimeframe})</strong>`;
        loadLiveCandles();
    }
}

// RENDER OR UPDATE EQUITY/BALANCE GROWTH CURVE
function updateEquityChart(equityData) {
    if (!equityData || equityData.length === 0) return;
    
    if (!chart) initLightweightChart();
    
    // Clear any previous Series
    if (areaSeries) { chart.removeSeries(areaSeries); areaSeries = null; }
    if (candlestickSeries) { chart.removeSeries(candlestickSeries); candlestickSeries = null; }
    
    areaSeries = chart.addAreaSeries({
        topColor: 'rgba(6, 182, 212, 0.35)',
        bottomColor: 'rgba(139, 92, 246, 0.0)',
        lineColor: '#06b6d4',
        lineWidth: 3,
        crosshairMarkerVisible: true
    });
    
    // Group and sort data points by unique date (YYYY-MM-DD) to satisfy TradingView requirements
    const seenDates = new Set();
    const uniquePoints = [];
    
    for (let i = equityData.length - 1; i >= 0; i--) {
        const item = equityData[i];
        const datePart = item.time.split(' ')[0];
        if (!seenDates.has(datePart)) {
            seenDates.add(datePart);
            uniquePoints.unshift({
                time: datePart,
                value: item.balance
            });
        }
    }
    
    uniquePoints.sort((a, b) => new Date(a.time) - new Date(b.time));
    
    areaSeries.setData(uniquePoints);
    chart.timeScale().fitContent();
}

// FETCH AND RENDER CANDLESTICK CHART REAL-TIME CLOSE DATA
async function loadLiveCandles() {
    if (currentChartMode !== 'candles') return;
    
    selectedSymbol = document.getElementById("candle-symbol-select").value;
    selectedTimeframe = document.getElementById("candle-tf-select").value;
    
    const titleEl = document.getElementById("chart-display-title");
    titleEl.innerHTML = `<i class="fa-solid fa-candles text-muted"></i> Live Price Action: <strong>${selectedSymbol} (${selectedTimeframe})</strong>`;
    
    try {
        const response = await fetch(`/api/candles?symbol=${selectedSymbol}&timeframe=${selectedTimeframe}&count=150`);
        const data = await response.json();
        
        if (data.success && data.candles && data.candles.length > 0) {
            renderLiveCandleChart(data.candles);
        } else {
            console.error(data.error);
        }
    } catch (e) {
        console.error("Failed to load live price candles:", e);
    }
}

function renderLiveCandleChart(candles) {
    if (!chart) initLightweightChart();
    
    // Clear any previous Series
    if (areaSeries) { chart.removeSeries(areaSeries); areaSeries = null; }
    if (candlestickSeries) { chart.removeSeries(candlestickSeries); candlestickSeries = null; }
    
    // Create true Candlestick Series (Green/Red candles with wicks!)
    candlestickSeries = chart.addCandlestickSeries({
        upColor: '#10b981',
        downColor: '#f43f5e',
        borderUpColor: '#10b981',
        borderDownColor: '#f43f5e',
        wickUpColor: '#10b981',
        wickDownColor: '#f43f5e'
    });
    
    const formatted = candles.map(c => ({
        time: c.time, // UNIX timestamp in seconds
        open: c.open,
        high: c.high,
        low: c.low,
        close: c.close
    }));
    
    formatted.sort((a, b) => a.time - b.time);
    
    candlestickSeries.setData(formatted);
    chart.timeScale().fitContent();
    
    updateChartPriceLines();
}

function updateChartPriceLines() {
    if (currentChartMode !== 'candles' || !candlestickSeries) return;
    
    activePriceLines.forEach(line => {
        try {
            candlestickSeries.removePriceLine(line);
        } catch (e) {
            console.error("Error removing price line:", e);
        }
    });
    activePriceLines = [];
    
    // Find matching trades for selectedSymbol (stripping broker suffixes like 'm' if needed)
    const normalizedSelected = selectedSymbol.replace(/[a-z]$/, '').toUpperCase();
    
    const matched = activeTradesCache.filter(t => {
        const normTradeSym = t.symbol.replace(/[a-z]$/, '').toUpperCase();
        return normTradeSym === normalizedSelected;
    });
    
    matched.forEach(t => {
        // Entry line (Cyan)
        const openLine = candlestickSeries.createPriceLine({
            price: t.open_price,
            color: '#06b6d4',
            lineWidth: 2,
            lineStyle: 2, // Dashed
            axisLabelVisible: true,
            title: `${t.type} Entry #${t.ticket}`
        });
        activePriceLines.push(openLine);
        
        // SL line (Red)
        if (t.sl > 0) {
            const slLine = candlestickSeries.createPriceLine({
                price: t.sl,
                color: '#f43f5e',
                lineWidth: 1.5,
                lineStyle: 3, // Dotted
                axisLabelVisible: true,
                title: `SL Target`
            });
            activePriceLines.push(slLine);
        }
        
        // TP line (Green)
        if (t.tp > 0) {
            const tpLine = candlestickSeries.createPriceLine({
                price: t.tp,
                color: '#10b981',
                lineWidth: 1.5,
                lineStyle: 3, // Dotted
                axisLabelVisible: true,
                title: `TP Target`
            });
            activePriceLines.push(tpLine);
        }
    });
}

// INTERVAL TRIGGER FOR CANDLES IF SELECTED
setInterval(() => {
    if (currentChartMode === 'candles') {
        loadLiveCandles();
    }
}, 5000);

// COPY REFERRAL URL
function copyInvestorUrl() {
    const input = document.getElementById("investor-url");
    if (!input) return;
    input.select();
    input.setSelectionRange(0, 99999);
    navigator.clipboard.writeText(input.value).then(() => {
        showToast("Investor read-only dashboard link copied!", "success");
    });
}

// ----------------------------------------------------
// PORTFOLIO MANAGEMENT & LIVE PERFORMANCE
// ----------------------------------------------------

function openSymbolWindow(symbol) {
    window.open(`/symbol/${symbol}`, `_blank`, `width=1200,height=800,toolbar=no,menubar=no,scrollbars=yes,resizable=yes`);
}

async function refreshPortfolio() {
    try {
        const response = await fetch("/api/portfolio");
        const data = await response.json();
        
        if (data.success) {
            // Update Weighted Change badge
            const weightedChangeEl = document.getElementById("portfolio-weighted-change");
            const weightedChange = parseFloat(data.portfolio_change_pct);
            weightedChangeEl.textContent = `${weightedChange >= 0 ? '+' : ''}${weightedChange.toFixed(2)}%`;
            weightedChangeEl.className = `profit-text ${weightedChange >= 0 ? 'profit-positive' : 'profit-negative'}`;
            
            // Populate portfolio allocation cards grid
            const gridContainer = document.getElementById("portfolio-cards-grid");
            let html = '';
            
            data.portfolio.forEach(p => {
                const change = parseFloat(p.change_pct);
                const isUp = change >= 0;
                const changeClass = isUp ? 'profit-positive' : 'profit-negative';
                
                // Format price and highs/lows based on asset
                let formattedPrice = '';
                let formattedHigh = '';
                let formattedLow = '';
                if (p.symbol.includes('BTC')) {
                    formattedPrice = p.price.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2});
                    formattedHigh = p.high.toLocaleString(undefined, {maximumFractionDigits:0});
                    formattedLow = p.low.toLocaleString(undefined, {maximumFractionDigits:0});
                } else if (p.symbol.includes('OIL')) {
                    formattedPrice = p.price.toFixed(3);
                    formattedHigh = p.high.toFixed(2);
                    formattedLow = p.low.toFixed(2);
                } else {
                    formattedPrice = p.price.toFixed(2);
                    formattedHigh = p.high.toFixed(2);
                    formattedLow = p.low.toFixed(2);
                }
                
                html += `
                    <div class="glass-panel" onclick="openSymbolWindow('${p.symbol}')" style="padding: 1rem; cursor: pointer; transition: var(--transition-smooth); border: 1px solid var(--card-border); position: relative; border-radius: 12px; overflow: hidden;" onmouseover="this.style.borderColor='var(--accent-violet)'; this.style.transform='translateY(-2px)';" onmouseout="this.style.borderColor='var(--card-border)'; this.style.transform='translateY(0)';">
                        <!-- Top visual line accent -->
                        <div style="position: absolute; top: 0; left: 0; width: 100%; height: 3px; background: var(--accent-violet);"></div>
                        
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; margin-top: 0.25rem;">
                            <strong style="font-size: 0.85rem; font-family: var(--font-heading); color: #ffffff;">${p.symbol}</strong>
                            <span class="badge-type" style="background: rgba(139, 92, 246, 0.12); color: var(--accent-violet); font-size: 0.65rem; padding: 0.15rem 0.45rem;">${p.weight.toFixed(0)}%</span>
                        </div>
                        
                        <div style="font-size: 1.2rem; font-family: var(--font-heading); font-weight: 800; color: var(--text-primary); margin-bottom: 0.35rem; letter-spacing: -0.2px;">
                            ${formattedPrice}
                        </div>
                        
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <span class="profit-text ${changeClass}" style="font-size: 0.75rem;">
                                ${isUp ? '▲' : '▼'} ${change >= 0 ? '+' : ''}${change.toFixed(2)}%
                            </span>
                            <div style="font-size: 0.65rem; color: var(--text-secondary); text-align: right; line-height: 1.3;">
                                <div style="color: var(--accent-emerald); font-weight: 600;">H: ${formattedHigh}</div>
                                <div style="color: var(--accent-rose); font-weight: 600;">L: ${formattedLow}</div>
                            </div>
                        </div>
                    </div>
                `;
            });
            gridContainer.innerHTML = html;
            
            // Populate daily summary cockpit
            const summary = data.summary;
            
            const todayProfitEl = document.getElementById("port-today-profit");
            todayProfitEl.textContent = formatCurrency(summary.today_profit);
            todayProfitEl.className = summary.today_profit >= 0 ? 'profit-positive' : 'profit-negative';
            
            const floatingPlEl = document.getElementById("port-floating-pl");
            floatingPlEl.textContent = formatCurrency(summary.floating_pl);
            floatingPlEl.className = summary.floating_pl >= 0 ? 'profit-positive' : 'profit-negative';
            
            document.getElementById("port-today-trades").textContent = summary.today_trades;
            document.getElementById("port-today-winrate").textContent = `${summary.today_win_rate}%`;
        }
    } catch (e) {
        console.error("Portfolio refresh failed:", e);
    }
}

// FETCH AND RENDER FOREX MARKET INTELLIGENCE (ECONOMIC CALENDAR & FLASH BULLETINS)
async function refreshMarketIntelligence() {
    try {
        const response = await fetch("/api/market-intelligence");
        const data = await response.json();
        
        if (data.success) {
            // Render Economic Calendar
            const calendarTbody = document.getElementById("economic-calendar-tbody");
            if (calendarTbody && data.calendar) {
                let html = '';
                data.calendar.forEach(e => {
                    const isHigh = e.impact === 'HIGH';
                    const impactClass = isHigh ? 'badge-sell' : 'badge-buy';
                    const actualVal = parseFloat(e.actual);
                    const forecastVal = parseFloat(e.forecast);
                    let actualClass = '';
                    
                    if (!isNaN(actualVal) && !isNaN(forecastVal)) {
                        if (actualVal > forecastVal) actualClass = 'profit-positive';
                        else if (actualVal < forecastVal) actualClass = 'profit-negative';
                    }
                    
                    html += `
                        <tr>
                            <td style="font-weight: 700; color: var(--text-secondary); padding: 0.65rem 1rem;"><i class="fa-regular fa-clock" style="margin-right: 0.3rem;"></i> ${e.time}</td>
                            <td style="font-weight: 700; color: #ffffff; padding: 0.65rem 1rem;">${e.currency}</td>
                            <td style="font-weight: 600; text-overflow: ellipsis; white-space: nowrap; overflow: hidden; max-width: 170px; padding: 0.65rem 1rem;" title="${e.event}">${e.event}</td>
                            <td style="text-align: center; padding: 0.65rem 1rem;">
                                <span class="badge-type ${impactClass}" style="font-size: 0.65rem; padding: 0.15rem 0.45rem;">
                                    ${e.impact}
                                </span>
                            </td>
                            <td style="padding: 0.65rem 1rem;">${e.forecast}</td>
                            <td style="padding: 0.65rem 1rem;">${e.previous}</td>
                            <td style="padding: 0.65rem 1rem;"><strong class="profit-text ${actualClass}">${e.actual}</strong></td>
                        </tr>
                    `;
                });
                calendarTbody.innerHTML = html;
            }
            
            // Render News Wire Ticker
            const newsContainer = document.getElementById("forex-news-container");
            if (newsContainer && data.news) {
                let html = '';
                data.news.forEach(n => {
                    const isBullish = n.sentiment === 'BULLISH';
                    const isBearish = n.sentiment === 'BEARISH';
                    const sentimentClass = isBullish ? 'profit-positive' : (isBearish ? 'profit-negative' : 'text-muted');
                    const sentimentIcon = isBullish ? 'fa-arrow-trend-up' : (isBearish ? 'fa-arrow-trend-down' : 'fa-minus');
                    const sentimentLabel = isBullish ? 'Bullish Bias' : (isBearish ? 'Bearish Bias' : 'Neutral Bias');
                    
                    html += `
                        <div class="symbol-item" style="flex-direction: column; align-items: stretch; gap: 0.4rem; padding: 0.75rem 1rem; background: rgba(3, 7, 18, 0.25); border: 1px solid rgba(255,255,255,0.02); border-radius: 8px; transition: var(--transition-smooth);" onmouseover="this.style.borderColor='var(--accent-cyan)'" onmouseout="this.style.borderColor='rgba(255,255,255,0.02)'">
                            <div style="display: flex; justify-content: space-between; align-items: center; font-size: 0.7rem;">
                                <div style="display: flex; align-items: center; gap: 0.4rem;">
                                    <strong style="color: var(--accent-cyan); font-family: var(--font-heading);">${n.asset} Flash</strong>
                                    <span style="font-size: 0.65rem; color: var(--text-muted);"><i class="fa-regular fa-clock"></i> ${n.time}</span>
                                </div>
                                <span class="profit-text ${sentimentClass}" style="font-size: 0.65rem; font-weight: 700; display: flex; align-items: center; gap: 0.25rem;">
                                    <i class="fa-solid ${sentimentIcon}"></i> ${sentimentLabel}
                                </span>
                            </div>
                            <p style="font-size: 0.75rem; color: var(--text-primary); line-height: 1.45; margin-top: 0.1rem; font-weight: 500;">
                                ${n.title}
                            </p>
                        </div>
                    `;
                });
                newsContainer.innerHTML = html;
            }
        }
    } catch (e) {
        console.error("Failed to load Forex Intelligence Feed:", e);
    }
}

// DYNAMICALLY UPDATE TRADINGVIEW TECHNICAL SENTIMENT GAUGE ON SELECTION
function updateTradingViewGauge(rawSymbol) {
    const container = document.getElementById("tradingview-gauge-mount");
    if (!container) return;
    
    // Clear container
    container.innerHTML = '';
    
    // Strip broker suffixes (e.g., EURUSDm -> EURUSD)
    let symbol = rawSymbol.replace(/[a-z]$/, '').toUpperCase();
    
    // Map standard symbols to standard TradingView feed sources
    let tvSymbol = `FX:${symbol}`;
    if (symbol.includes("XAU") || symbol.includes("XAG")) {
        tvSymbol = `OANDA:${symbol}`;
    } else if (symbol.includes("BTC")) {
        tvSymbol = `BINANCE:${symbol}T`; // BTCUSDT
    } else if (symbol.includes("OIL")) {
        tvSymbol = `TVC:${symbol}`;
    }
    
    // Create new widget elements dynamically
    const widgetDiv = document.createElement("div");
    widgetDiv.className = "tradingview-widget-container__widget";
    widgetDiv.style.width = "100%";
    widgetDiv.style.height = "100%";
    container.appendChild(widgetDiv);
    
    const script = document.createElement("script");
    script.type = "text/javascript";
    script.src = "https://s3.tradingview.com/external-embedding/embed-widget-technical-analysis.js";
    script.async = true;
    
    const config = {
        "interval": "5m",
        "width": "100%",
        "isTransparent": true,
        "height": "100%",
        "symbol": tvSymbol,
        "showIntervalTabs": true,
        "displayMode": "single",
        "locale": "en",
        "colorTheme": "dark"
    };
    
    script.innerHTML = JSON.stringify(config);
    container.appendChild(script);
}
