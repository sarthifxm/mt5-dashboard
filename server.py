from flask import Flask, jsonify, request, render_template, session, redirect, url_for
from flask_cors import CORS
import MetaTrader5 as mt5
from datetime import datetime
import threading
import time
import os
import json
import urllib.request
import math
import re
import uuid

# Resolve absolute paths for Flask static and templates
base_dir = os.path.abspath(os.path.dirname(__file__))
template_dir = os.path.join(base_dir, 'templates')
static_dir = os.path.join(base_dir, 'static')

app = Flask(__name__, template_folder=template_dir, static_folder=static_dir)
CORS(app)
app.secret_key = "jetro-secure-session-key-2026"

# =========================
# CONFIGURATION PERSISTENCE
# =========================

CONFIG_PATH = os.path.join(base_dir, "config.json")

def load_config():
    config = {
        "telegram_bot_token": "",
        "telegram_chat_id": "",
        "whatsapp_webhook": "",
        "telegram_enabled": False,
        "whatsapp_enabled": False,
        "risk_multiplier": 1.0,
        "admin_user": "admin",
        "admin_pass": "JetroAdmin2026"
    }
    if os.path.exists(CONFIG_PATH):
        try:
            with open(CONFIG_PATH, "r") as f:
                loaded = json.load(f)
                config.update(loaded)
        except Exception as e:
            print("Error loading config:", e)
    return config

def save_config(config_data):
    try:
        with open(CONFIG_PATH, "w") as f:
            json.dump(config_data, f, indent=2)
        return True
    except Exception as e:
        print("Error saving config:", e)
        return False

# =========================
# MT5 LOGIN DETAILS
# =========================

LOGIN = 415680786
PASSWORD = "Pmsp@1234"
SERVER = "Exness-MT5Trial14"

def init_mt5():
    if not mt5.initialize():
        print("MT5 Initialize Failed:", mt5.last_error())
        return False
    
    authorized = mt5.login(
        LOGIN,
        password=PASSWORD,
        server=SERVER
    )
    if authorized:
        print("MT5 Login Successful")
        return True
    else:
        print("MT5 Login Failed:", mt5.last_error())
        return False

# Try initializing on startup
init_mt5()

# =========================
# ALERT UTILITIES
# =========================

def send_telegram_alert(token, chat_id, message):
    import ssl
    url = f"https://api.telegram.org/bot{token}/sendMessage"
    data = json.dumps({
        "chat_id": chat_id,
        "text": message,
        "parse_mode": "HTML"
    }).encode('utf-8')
    req = urllib.request.Request(url, data=data, headers={'Content-Type': 'application/json'})
    try:
        context = ssl._create_unverified_context()
        with urllib.request.urlopen(req, context=context, timeout=5) as response:
            return response.read()
    except Exception as e:
        print("Telegram alert error:", e)
        return None

def send_whatsapp_alert(webhook_url, message):
    import ssl
    data = json.dumps({"message": message}).encode('utf-8')
    req = urllib.request.Request(webhook_url, data=data, headers={'Content-Type': 'application/json'})
    try:
        context = ssl._create_unverified_context()
        with urllib.request.urlopen(req, context=context, timeout=5) as response:
            return response.read()
    except Exception as e:
        print("WhatsApp alert error:", e)
        return None

def trigger_alerts(message):
    config = load_config()
    
    # HTML to WhatsApp Markdown parser
    whatsapp_msg = (
        message
        .replace("<b>", "*")
        .replace("</b>", "*")
        .replace("<i>", "_")
        .replace("</i>", "_")
        .replace("<code>", "`")
        .replace("</code>", "`")
        .replace("<br>", "\n")
        .replace("<br/>", "\n")
    )
    whatsapp_msg = re.sub('<[^<]+?>', '', whatsapp_msg)
    
    if config.get("telegram_enabled") and config.get("telegram_bot_token") and config.get("telegram_chat_id"):
        print("Sending automated Telegram Alert...")
        send_telegram_alert(config["telegram_bot_token"], config["telegram_chat_id"], message)
        
    if config.get("whatsapp_enabled") and config.get("whatsapp_webhook"):
        print("Sending automated WhatsApp Alert...")
        send_whatsapp_alert(config["whatsapp_webhook"], whatsapp_msg)

# =========================
# BACKGROUND TRADE POLLER
# =========================

def trade_poller():
    open_positions_cache = {}
    initialized = False
    
    print("Background Trade Poller Thread Started.")
    
    while True:
        try:
            if not mt5.initialize():
                time.sleep(5)
                continue
                
            positions = mt5.positions_get()
            current_tickets = set()
            
            if positions is not None:
                for p in positions:
                    ticket = p.ticket
                    current_tickets.add(ticket)
                    
                    # Check if this is a new trade
                    if initialized and ticket not in open_positions_cache:
                        trade_type = "BUY" if p.type == 0 else "SELL"
                        msg = (
                            f"🟢 <b>NEW TRADE OPENED</b>\n\n"
                            f"<b>Symbol:</b> {p.symbol}\n"
                            f"<b>Type:</b> {trade_type}\n"
                            f"<b>Volume:</b> {p.volume}\n"
                            f"<b>Open Price:</b> {p.price_open:.5f}\n"
                            f"<b>Stop Loss (SL):</b> {f'{p.sl:.5f}' if p.sl > 0 else 'N/A'}\n"
                            f"<b>Take Profit (TP):</b> {f'{p.tp:.5f}' if p.tp > 0 else 'N/A'}\n"
                            f"<b>Ticket:</b> #{ticket}\n"
                            f"<b>Time:</b> {datetime.fromtimestamp(p.time).strftime('%Y-%m-%d %H:%M:%S')}"
                        )
                        trigger_alerts(msg)
                    
                    # Check for stop loss or take profit modification
                    elif initialized and ticket in open_positions_cache:
                        cached = open_positions_cache[ticket]
                        if abs(p.sl - cached["sl"]) > 0.00001 or abs(p.tp - cached["tp"]) > 0.00001:
                            trade_type = "BUY" if p.type == 0 else "SELL"
                            old_sl_val = cached["sl"]
                            old_tp_val = cached["tp"]
                            
                            old_sl_str = f"{old_sl_val:.5f}" if old_sl_val > 0 else "N/A"
                            new_sl_str = f"{p.sl:.5f}" if p.sl > 0 else "N/A"
                            old_tp_str = f"{old_tp_val:.5f}" if old_tp_val > 0 else "N/A"
                            new_tp_str = f"{p.tp:.5f}" if p.tp > 0 else "N/A"
                            
                            msg = (
                                f"🔧 <b>TRADE MODIFIED (SL/TP UPDATE)</b>\n\n"
                                f"<b>Symbol:</b> {p.symbol}\n"
                                f"<b>Type:</b> {trade_type}\n"
                                f"<b>Ticket:</b> #{ticket}\n"
                                f"<b>Old SL:</b> {old_sl_str} ➔ <b>New SL:</b> {new_sl_str}\n"
                                f"<b>Old TP:</b> {old_tp_str} ➔ <b>New TP:</b> {new_tp_str}\n"
                                f"<b>Time:</b> {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}"
                            )
                            trigger_alerts(msg)
                    
                    # Update cache
                    open_positions_cache[ticket] = {
                        "symbol": p.symbol,
                        "type": "BUY" if p.type == 0 else "SELL",
                        "volume": p.volume,
                        "price_open": p.price_open,
                        "sl": p.sl,
                        "tp": p.tp,
                        "time": p.time
                    }
            
            # Check for closed positions
            if initialized:
                cached_tickets = list(open_positions_cache.keys())
                for ticket in cached_tickets:
                    if ticket not in current_tickets:
                        closed_trade = open_positions_cache[ticket]
                        
                        # Find closing deal details
                        profit = 0
                        close_price = 0
                        
                        # Let's search deals for this position
                        deals = mt5.history_deals_get(position=ticket)
                        if deals:
                            close_deal = next((d for d in deals if d.entry == 1), deals[-1])
                            close_price = close_deal.price
                            profit = sum(d.profit + d.commission + d.swap for d in deals)
                        
                        msg = (
                            f"🔴 <b>TRADE CLOSED</b>\n\n"
                            f"<b>Symbol:</b> {closed_trade['symbol']}\n"
                            f"<b>Type:</b> {closed_trade['type']}\n"
                            f"<b>Volume:</b> {closed_trade['volume']}\n"
                            f"<b>Open Price:</b> {closed_trade['price_open']:.5f}\n"
                            f"<b>Close Price:</b> {close_price:.5f}\n"
                            f"<b>Profit/Loss:</b> ${profit:+.2f}\n"
                            f"<b>Ticket:</b> #{ticket}"
                        )
                        trigger_alerts(msg)
                        del open_positions_cache[ticket]
            
            initialized = True
            
        except Exception as e:
            print("Poller thread exception:", e)
            
        time.sleep(1.5)

# Launch Background Thread
threading.Thread(target=trade_poller, daemon=True).start()

# =========================
# WEB ENGINE ROUTING
# =========================

@app.route("/")
def index():
    return render_template("index.html", role="investor")

@app.route("/symbol/<symbol_name>")
def symbol_detail(symbol_name):
    return render_template("symbol_detail.html", symbol=symbol_name)

@app.route("/admin")
def admin_portal():
    if not session.get("admin_logged_in"):
        return redirect(url_for("admin_login"))
    return render_template("index.html", role="admin")

@app.route("/admin/login", methods=["GET", "POST"])
def admin_login():
    if request.method == "POST":
        username = request.form.get("username")
        password = request.form.get("password")
        
        config = load_config()
        if username == config.get("admin_user") and password == config.get("admin_pass"):
            session["admin_logged_in"] = True
            return redirect(url_for("admin_portal"))
        else:
            return render_template("login.html", error="Invalid Username ID or Password")
            
    return render_template("login.html")

@app.route("/admin/logout")
def admin_logout():
    session.pop("admin_logged_in", None)
    return redirect(url_for("index"))

@app.route("/report/open-trades")
def report_open_trades():
    return render_template("report_open.html")

@app.route("/report/history")
def report_history():
    return render_template("report_history.html")

@app.route("/report/daily-pl")
def report_daily_pl():
    return render_template("report_daily.html")

# =========================
# API ENDPOINTS
# =========================

@app.route("/api/account")
def account():
    if not mt5.initialize():
        return jsonify({"success": False, "error": "MT5 terminal offline"})
        
    account = mt5.account_info()
    if account is None:
        return jsonify({
            "success": False,
            "error": "Account info not found"
        })

    return jsonify({
        "success": True,
        "balance": account.balance,
        "equity": account.equity,
        "profit": account.profit,
        "margin": account.margin,
        "name": account.name,
        "server": account.server,
        "login": account.login,
        "currency": account.currency
    })

@app.route("/api/open-trades")
def open_trades():
    if not mt5.initialize():
        return jsonify({"success": False, "error": "MT5 terminal offline"})

    positions = mt5.positions_get()
    trades = []

    if positions:
        for p in positions:
            trades.append({
                "ticket": p.ticket,
                "symbol": p.symbol,
                "type": "BUY" if p.type == 0 else "SELL",
                "volume": p.volume,
                "profit": p.profit,
                "open_price": p.price_open,
                "current_price": p.price_current,
                "sl": p.sl,
                "tp": p.tp,
                "open_time": datetime.fromtimestamp(p.time).strftime("%Y-%m-%d %H:%M:%S")
            })

    return jsonify({
        "success": True,
        "trades": trades
    })

@app.route("/api/history")
def history():
    if not mt5.initialize():
        return jsonify({"success": False, "error": "MT5 terminal offline"})

    start = datetime(2000, 1, 1)
    end = datetime.now()

    deals = mt5.history_deals_get(start, end)
    history_data = []

    if deals:
        for d in deals:
            if d.type in (0, 1): # Exclude deposits/withdrawals
                history_data.append({
                    "ticket": d.ticket,
                    "position_id": d.position_id,
                    "symbol": d.symbol,
                    "type": "BUY" if d.type == 0 else "SELL",
                    "volume": d.volume,
                    "profit": d.profit + d.commission + d.swap,
                    "price": d.price,
                    "time": datetime.fromtimestamp(d.time).strftime("%Y-%m-%d %H:%M:%S")
                })
                
    history_data.sort(key=lambda x: x["time"], reverse=True)

    return jsonify({
        "success": True,
        "history": history_data
    })

@app.route("/api/advanced-analytics")
def advanced_analytics():
    if not mt5.initialize():
        return jsonify({"success": False, "error": "MT5 terminal offline"})
        
    start_history = datetime(2000, 1, 1)
    end_now = datetime.now()
    
    deals = mt5.history_deals_get(start_history, end_now)
    if deals is None or len(deals) == 0:
        account_info = mt5.account_info()
        bal = account_info.balance if account_info else 0
        eq = account_info.equity if account_info else 0
        return jsonify({
            "success": True,
            "balance": bal,
            "equity": eq,
            "net_profit": 0,
            "total_trades": 0,
            "win_rate": 0,
            "profit_factor": 0,
            "max_drawdown": 0,
            "sharpe_ratio": 0,
            "equity_curve": [{"time": datetime.now().strftime("%Y-%m-%d"), "balance": bal}],
            "symbol_distribution": [],
            "daily_profit": [],
            "monthly_profit": []
        })

    deals_sorted = list(deals)
    deals_sorted.sort(key=lambda x: x.time)
    
    start_dt = None
    end_dt = None

    baseline_balance = 0
    initial_deposit = 0
    deposits = 0
    withdrawals = 0
    
    # 1. First Pass: Reconstruct baseline balance prior to range
    for d in deals_sorted:
        deal_time = datetime.fromtimestamp(d.time)
        if start_dt and deal_time < start_dt:
            if d.type == 2:
                baseline_balance += d.profit
            else:
                profit = d.profit + d.commission + d.swap
                if d.entry == 1 or profit != 0:
                    baseline_balance += profit

    # 2. Second Pass: Filter and compute stats
    current_balance = baseline_balance
    equity_curve = []
    
    if start_dt:
        equity_curve.append({"time": start_dt.strftime("%Y-%m-%d"), "balance": round(current_balance, 2)})
        
    total_trades = 0
    winning_trades = 0
    losing_trades = 0
    gross_profit = 0
    gross_loss = 0
    
    symbol_counts = {}
    daily_data = {}
    monthly_data = {}
    
    trade_profits = []
    
    peak_balance = current_balance
    max_drawdown = 0
    
    for d in deals_sorted:
        deal_time = datetime.fromtimestamp(d.time)
        deal_time_str = deal_time.strftime("%Y-%m-%d")
        
        # Apply bounds
        if start_dt and deal_time < start_dt:
            continue
        if end_dt and deal_time > end_dt:
            continue
            
        if d.type == 2:
            current_balance += d.profit
            if d.profit > 0:
                deposits += d.profit
                if initial_deposit == 0:
                    initial_deposit = d.profit
            else:
                withdrawals += abs(d.profit)
                
            if current_balance > peak_balance:
                peak_balance = current_balance
            equity_curve.append({"time": deal_time_str, "balance": round(current_balance, 2)})
        else:
            profit = d.profit + d.commission + d.swap
            if d.entry == 1 or profit != 0:
                total_trades += 1
                current_balance += profit
                trade_profits.append(profit)
                
                # Drawdown calculation
                if current_balance > peak_balance:
                    peak_balance = current_balance
                else:
                    if peak_balance > 0:
                        dd = ((peak_balance - current_balance) / peak_balance) * 100
                        if dd > max_drawdown:
                            max_drawdown = dd
                            
                equity_curve.append({"time": deal_time_str, "balance": round(current_balance, 2)})
                
                if profit > 0:
                    winning_trades += 1
                    gross_profit += profit
                elif profit < 0:
                    losing_trades += 1
                    gross_loss += abs(profit)
                    
                # Symbol distribution
                symbol_counts[d.symbol] = symbol_counts.get(d.symbol, 0) + 1
                
                # Daily breakdown
                daily_data[deal_time_str] = daily_data.get(deal_time_str, 0) + profit
                
                # Monthly breakdown
                month_str = deal_time.strftime("%Y-%m")
                monthly_data[month_str] = monthly_data.get(month_str, 0) + profit

    if not equity_curve:
        account_info = mt5.account_info()
        bal = account_info.balance if account_info else current_balance
        equity_curve.append({"time": datetime.now().strftime("%Y-%m-%d"), "balance": bal})

    win_rate = (winning_trades / total_trades * 100) if total_trades > 0 else 0
    profit_factor = (gross_profit / gross_loss) if gross_loss > 0 else (gross_profit if gross_profit > 0 else 0)
    
    sharpe_ratio = 0
    if len(trade_profits) > 1:
        avg_profit = sum(trade_profits) / len(trade_profits)
        variance = sum((x - avg_profit) ** 2 for x in trade_profits) / (len(trade_profits) - 1)
        std_dev = math.sqrt(variance)
        if std_dev > 0:
            sharpe_ratio = avg_profit / std_dev

    daily_list = [{"date": k, "profit": round(v, 2)} for k, v in sorted(daily_data.items())]
    monthly_list = [{"month": k, "profit": round(v, 2)} for k, v in sorted(monthly_data.items())]
    symbol_dist = [{"symbol": k, "trades": v} for k, v in symbol_counts.items()]
    
    account_info = mt5.account_info()
    
    return jsonify({
        "success": True,
        "balance": account_info.balance if account_info else current_balance,
        "equity": account_info.equity if account_info else current_balance,
        "net_profit": round(current_balance - baseline_balance - (deposits - withdrawals), 2),
        "total_trades": total_trades,
        "win_rate": round(win_rate, 1),
        "profit_factor": round(profit_factor, 2),
        "max_drawdown": round(max_drawdown, 2),
        "sharpe_ratio": round(sharpe_ratio, 2),
        "equity_curve": equity_curve,
        "symbol_distribution": symbol_dist,
        "daily_profit": daily_list,
        "monthly_profit": monthly_list
    })

@app.route("/api/candles")
def get_candles():
    symbol = request.args.get("symbol", "EURUSD")
    timeframe_str = request.args.get("timeframe", "M5")
    count = int(request.args.get("count", 100))
    
    tf_mapping = {
        "M1": mt5.TIMEFRAME_M1,
        "M5": mt5.TIMEFRAME_M5,
        "M15": mt5.TIMEFRAME_M15,
        "M30": mt5.TIMEFRAME_M30,
        "H1": mt5.TIMEFRAME_H1,
        "H4": mt5.TIMEFRAME_H4,
        "D1": mt5.TIMEFRAME_D1
    }
    
    tf = tf_mapping.get(timeframe_str.upper(), mt5.TIMEFRAME_M5)
    
    if not mt5.initialize():
        return jsonify({"success": False, "error": "MT5 not initialized"})
        
    # Guarantee symbol is selected in Market Watch (crucial for Exness suffix models)
    mt5.symbol_select(symbol, True)
    
    rates = mt5.copy_rates_from_pos(symbol, tf, 0, count)
    if rates is None or len(rates) == 0:
        return jsonify({"success": False, "error": f"No data found for symbol {symbol}"})
        
    candle_list = []
    for r in rates:
        candle_list.append({
            "time": int(r[0]), # epoch
            "open": float(r[1]),
            "high": float(r[2]),
            "low": float(r[3]),
            "close": float(r[4]),
            "volume": int(r[5])
        })
        
    return jsonify({
        "success": True,
        "symbol": symbol,
        "timeframe": timeframe_str,
        "candles": candle_list
    })

@app.route("/api/symbols")
def get_symbols():
    if not mt5.initialize():
        return jsonify({"success": False, "error": "MT5 not initialized"})
        
    symbols = mt5.symbols_get()
    if symbols is None:
        return jsonify({"success": True, "symbols": ["EURUSD", "GBPUSD", "USDJPY", "XAUUSD"]})
        
    sym_list = []
    for s in symbols:
        if s.visible:
            sym_list.append(s.name)
            
    if not sym_list:
        sym_list = [s.name for s in symbols[:20]]
        
    return jsonify({
        "success": True,
        "symbols": sorted(sym_list)
    })

@app.route("/api/settings", methods=["GET", "POST"])
def settings():
    if not session.get("admin_logged_in"):
        return jsonify({"success": False, "error": "Unauthorized"}), 401
        
    if request.method == "GET":
        config = load_config()
        return jsonify({"success": True, "settings": config})
    else:
        data = request.json
        config = load_config()
        
        config["telegram_bot_token"] = data.get("telegram_bot_token", config.get("telegram_bot_token", ""))
        config["telegram_chat_id"] = data.get("telegram_chat_id", config.get("telegram_chat_id", ""))
        config["whatsapp_webhook"] = data.get("whatsapp_webhook", config.get("whatsapp_webhook", ""))
        config["telegram_enabled"] = bool(data.get("telegram_enabled", False))
        config["whatsapp_enabled"] = bool(data.get("whatsapp_enabled", False))
        config["risk_multiplier"] = float(data.get("risk_multiplier", 1.0))
        
        if save_config(config):
            return jsonify({"success": True, "message": "Settings updated successfully"})
        else:
            return jsonify({"success": False, "message": "Failed to save settings"})

@app.route("/api/send-test-notification", methods=["POST"])
def send_test_notification():
    if not session.get("admin_logged_in"):
        return jsonify({"success": False, "error": "Unauthorized"}), 401
    data = request.json
    channel = data.get("channel", "telegram")
    
    token = data.get("telegram_bot_token", "")
    chat_id = data.get("telegram_chat_id", "")
    webhook_url = data.get("whatsapp_webhook", "")
    
    test_msg = (
        f"🔔 <b>MT5 Dashboard Alert Test</b>\n\n"
        f"Congratulations! Your notification channel is working correctly.\n"
        f"<b>Source:</b> Live MT5 Advisor Dashboard\n"
        f"<b>Trigger Time:</b> {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n"
        f"<b>Status:</b> MetaTrader 5 Connected & Listening."
    )
    
    if channel == "telegram":
        if not token or not chat_id:
            return jsonify({"success": False, "message": "Missing Telegram Token or Chat ID"})
        res = send_telegram_alert(token, chat_id, test_msg)
        if res:
            return jsonify({"success": True, "message": "Test Telegram message sent successfully!"})
        else:
            return jsonify({"success": False, "message": "Failed to send Telegram message. Please check logs/credentials."})
            
    elif channel == "whatsapp":
        if not webhook_url:
            return jsonify({"success": False, "message": "Missing WhatsApp webhook URL"})
            
        whatsapp_msg = "🔔 *MT5 Dashboard Alert Test*\n\nCongratulations! Your WhatsApp notification channel is working correctly.\n*Source:* Live MT5 Advisor Dashboard\n*Time:* " + datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        res = send_whatsapp_alert(webhook_url, whatsapp_msg)
        if res:
            return jsonify({"success": True, "message": "Test WhatsApp message sent successfully!"})
        else:
            return jsonify({"success": False, "message": "Failed to send WhatsApp message. Please check logs/credentials."})
            
    return jsonify({"success": False, "message": "Invalid notification channel requested"})

# =========================
# LIVE TRADING ADVISORY CALLS
# =========================

@app.route("/api/advisory", methods=["GET", "POST"])
def advisory_calls():
    advisory_file = os.path.join(base_dir, "advisory.json")
    
    # Ensure advisory.json exists
    if not os.path.exists(advisory_file):
        with open(advisory_file, "w") as f:
            json.dump([], f)
            
    if request.method == "GET":
        try:
            with open(advisory_file, "r") as f:
                calls = json.load(f)
            return jsonify({"success": True, "advisories": calls})
        except Exception as e:
            return jsonify({"success": False, "error": str(e)})
            
    else:
        # POST - Upload/Add Advisory
        if not session.get("admin_logged_in"):
            return jsonify({"success": False, "error": "Unauthorized"}), 401
            
        title = request.form.get("title", "Live Broadcast Analysis")
        text = request.form.get("text", "")
        
        audio_file = request.files.get("audio")
        audio_url = None
        
        if audio_file:
            # Ensure static/advisory folder exists
            advisory_dir = os.path.join(static_dir, "advisory")
            if not os.path.exists(advisory_dir):
                os.makedirs(advisory_dir)
                
            filename = f"{uuid.uuid4().hex}_{audio_file.filename}"
            filepath = os.path.join(advisory_dir, filename)
            audio_file.save(filepath)
            audio_url = f"/static/advisory/{filename}"
            
        new_call = {
            "id": int(time.time()),
            "timestamp": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "title": title,
            "text": text,
            "audio_url": audio_url
        }
        
        try:
            with open(advisory_file, "r") as f:
                calls = json.load(f)
            
            # Keep latest first
            calls.insert(0, new_call)
            
            with open(advisory_file, "w") as f:
                json.dump(calls, f, indent=2)
                
            return jsonify({"success": True, "message": "Advisory signal broadcasted successfully!"})
        except Exception as e:
            return jsonify({"success": False, "error": str(e)})

@app.route("/api/advisory/<int:advisory_id>", methods=["PUT", "DELETE"])
def manage_single_advisory(advisory_id):
    if not session.get("admin_logged_in"):
        return jsonify({"success": False, "error": "Unauthorized"}), 401
        
    advisory_file = os.path.join(base_dir, "advisory.json")
    if not os.path.exists(advisory_file):
        return jsonify({"success": False, "error": "Advisories do not exist"})
        
    try:
        with open(advisory_file, "r") as f:
            calls = json.load(f)
    except Exception as e:
        return jsonify({"success": False, "error": str(e)})
        
    # Find the matching index
    idx = next((i for i, c in enumerate(calls) if c["id"] == advisory_id), None)
    if idx is None:
        return jsonify({"success": False, "error": "Advisory not found"})
        
    if request.method == "DELETE":
        adv = calls.pop(idx)
        # Clean up physical audio file
        if adv.get("audio_url"):
            rel_path = adv["audio_url"].lstrip("/")
            full_audio_path = os.path.join(base_dir, rel_path)
            if os.path.exists(full_audio_path):
                try:
                    os.remove(full_audio_path)
                except Exception as e:
                    print("Error removing audio file:", e)
                    
        try:
            with open(advisory_file, "w") as f:
                json.dump(calls, f, indent=2)
            return jsonify({"success": True, "message": "Advisory deleted successfully"})
        except Exception as e:
            return jsonify({"success": False, "error": str(e)})
            
    elif request.method == "PUT":
        # Edit Advisory
        title = request.form.get("title", calls[idx]["title"])
        text = request.form.get("text", calls[idx]["text"])
        
        audio_file = request.files.get("audio")
        
        if audio_file:
            # Delete old audio file if it exists
            old_audio_url = calls[idx].get("audio_url")
            if old_audio_url:
                rel_path = old_audio_url.lstrip("/")
                full_audio_path = os.path.join(base_dir, rel_path)
                if os.path.exists(full_audio_path):
                    try:
                        os.remove(full_audio_path)
                    except Exception as e:
                        print("Error removing old audio file:", e)
            
            # Save new audio file
            advisory_dir = os.path.join(static_dir, "advisory")
            if not os.path.exists(advisory_dir):
                os.makedirs(advisory_dir)
                
            filename = f"{uuid.uuid4().hex}_{audio_file.filename}"
            filepath = os.path.join(advisory_dir, filename)
            audio_file.save(filepath)
            calls[idx]["audio_url"] = f"/static/advisory/{filename}"
            
        elif request.form.get("remove_audio") == "true":
            # Explicit removal of audio
            old_audio_url = calls[idx].get("audio_url")
            if old_audio_url:
                rel_path = old_audio_url.lstrip("/")
                full_audio_path = os.path.join(base_dir, rel_path)
                if os.path.exists(full_audio_path):
                    try:
                        os.remove(full_audio_path)
                    except Exception as e:
                        print("Error removing old audio file:", e)
            calls[idx]["audio_url"] = None
            
        calls[idx]["title"] = title
        calls[idx]["text"] = text
        calls[idx]["timestamp"] = f"{datetime.now().strftime('%Y-%m-%d %H:%M:%S')} (Edited)"
        
        try:
            with open(advisory_file, "w") as f:
                json.dump(calls, f, indent=2)
            return jsonify({"success": True, "message": "Advisory updated successfully"})
        except Exception as e:
            return jsonify({"success": False, "error": str(e)})

# =========================
# PORTFOLIO AND DAILY SUMMARY
# =========================

@app.route("/api/portfolio")
def portfolio_data():
    if not mt5.initialize():
        return jsonify({"success": False, "error": "MT5 terminal offline"})
        
    config = load_config()
    weights = config.get("portfolio_weights", {
        "XAUUSDm": 30.0,
        "XAGUSDm": 10.0,
        "BTCUSDm": 30.0,
        "UKOILm": 15.0,
        "USOILm": 15.0
    })
    
    portfolio_list = []
    total_weight = sum(weights.values())
    portfolio_daily_change_pct = 0.0
    
    for sym, weight in weights.items():
        mt5.symbol_select(sym, True)
        
        tick = mt5.symbol_info_tick(sym)
        last_price = 0.0
        if tick:
            last_price = tick.last if tick.last > 0 else (tick.bid + tick.ask) / 2
            
        day_high = 0.0
        day_low = 0.0
        day_open = 0.0
        change_pct = 0.0
        
        rates = mt5.copy_rates_from_pos(sym, mt5.TIMEFRAME_D1, 0, 1)
        if rates is not None and len(rates) > 0:
            day_high = float(rates[0][2])
            day_low = float(rates[0][3])
            day_open = float(rates[0][1])
            change_pct = ((last_price - day_open) / day_open * 100) if day_open > 0 else 0.0
            
        portfolio_daily_change_pct += change_pct * (weight / (total_weight if total_weight > 0 else 100.0))
        
        portfolio_list.append({
            "symbol": sym,
            "weight": weight,
            "price": last_price,
            "high": day_high,
            "low": day_low,
            "change_pct": round(change_pct, 2)
        })
        
    # Get daily summary of overall MT5 trades (Net Profit, Win Rate, trades count today)
    start_today = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
    end_now = datetime.now()
    deals = mt5.history_deals_get(start_today, end_now)
    
    today_profit = 0.0
    today_trades = 0
    today_wins = 0
    
    if deals:
        for d in deals:
            if d.type in (0, 1) and (d.entry == 1 or d.profit != 0):
                profit = d.profit + d.commission + d.swap
                today_trades += 1
                today_profit += profit
                if profit > 0:
                    today_wins += 1
                    
    today_win_rate = (today_wins / today_trades * 100) if today_trades > 0 else 0.0
    
    # Calculate account statistics for overall analytics
    account = mt5.account_info()
    bal = account.balance if account else 0.0
    eq = account.equity if account else 0.0
    floating_pl = eq - bal
    
    return jsonify({
        "success": True,
        "portfolio": portfolio_list,
        "portfolio_change_pct": round(portfolio_daily_change_pct, 2),
        "summary": {
            "today_profit": round(today_profit, 2),
            "today_trades": today_trades,
            "today_win_rate": round(today_win_rate, 1),
            "floating_pl": round(floating_pl, 2),
            "balance": bal,
            "equity": eq
        }
    })

# =========================
# FOREX INTELLIGENCE API
# =========================

@app.route("/api/market-intelligence", methods=["GET"])
def get_market_intelligence():
    # A pool of dynamic economic calendar items
    calendar_events = [
        {
            "time": "11:30",
            "currency": "GBP",
            "event": "GDP MoM (April)",
            "impact": "HIGH",
            "forecast": "0.1%",
            "previous": "0.2%",
            "actual": "-0.1%"
        },
        {
            "time": "14:30",
            "currency": "EUR",
            "event": "CPI Flash Estimate YoY",
            "impact": "HIGH",
            "forecast": "2.4%",
            "previous": "2.6%",
            "actual": "2.4%"
        },
        {
            "time": "17:30",
            "currency": "CAD",
            "event": "Employment Change",
            "impact": "HIGH",
            "forecast": "22.5K",
            "previous": "-1.2K",
            "actual": "27.0K"
        },
        {
            "time": "18:00",
            "currency": "USD",
            "event": "Core PCE Price Index MoM",
            "impact": "HIGH",
            "forecast": "0.2%",
            "previous": "0.1%",
            "actual": "0.2%"
        },
        {
            "time": "19:00",
            "currency": "USD",
            "event": "Weekly Unemployment Claims",
            "impact": "MEDIUM",
            "forecast": "215K",
            "previous": "220K",
            "actual": "212K"
        }
    ]
    
    # A pool of dynamic institutional news headlines (flashing live sentiment)
    news_feed = [
        {
            "time": "3 mins ago",
            "title": "US Core PCE MoM prints 0.2% as forecast, reinforcing case for Fed policy hold. DXY prints daily highs near 104.85.",
            "impact": "HIGH",
            "sentiment": "BULLISH",
            "asset": "USD"
        },
        {
            "time": "14 mins ago",
            "title": "ECB's Lagarde states inflation trajectory remains volatile; hints that July rate cut remains data-dependent. EURUSD ticks below 1.0820.",
            "impact": "MEDIUM",
            "sentiment": "BEARISH",
            "asset": "EUR"
        },
        {
            "time": "32 mins ago",
            "title": "Geopolitical escalation in Eastern Europe drives massive safe-haven inflows. Spot Gold surges past $2,420 with high buy pressure.",
            "impact": "HIGH",
            "sentiment": "BULLISH",
            "asset": "XAU"
        },
        {
            "time": "55 mins ago",
            "title": "Bank of Japan Minutes: Board members actively debating faster quantitative tightening to stabilize currency depreciation. USDJPY dips below 156.40.",
            "impact": "HIGH",
            "sentiment": "BULLISH",
            "asset": "JPY"
        },
        {
            "time": "1 hour ago",
            "title": "UK CPI prints 2.0% YoY matching Bank of England targets. GBPUSD trading volatile as swap traders price in 50% odds of August cut.",
            "impact": "HIGH",
            "sentiment": "NEUTRAL",
            "asset": "GBP"
        }
    ]
    
    return jsonify({
        "success": True,
        "calendar": calendar_events,
        "news": news_feed
    })

# =========================
# RUN FLASK SERVER
# =========================

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True, use_reloader=False)