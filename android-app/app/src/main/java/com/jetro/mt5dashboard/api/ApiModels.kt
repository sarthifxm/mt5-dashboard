package com.jetro.mt5dashboard.api

import com.google.gson.annotations.SerializedName

// ─── Account ─────────────────────────────────────────────
data class AccountResponse(
    val success: Boolean = false,
    val balance: Double = 0.0,
    val equity: Double = 0.0,
    val profit: Double = 0.0,
    val margin: Double = 0.0,
    val name: String = "",
    val server: String = "",
    val login: Long = 0,
    val currency: String = "USD",
    val error: String? = null
)

// ─── Open Trades ─────────────────────────────────────────
data class Trade(
    val ticket: Long = 0,
    val symbol: String = "",
    val type: String = "",
    val volume: Double = 0.0,
    val profit: Double = 0.0,
    val open_price: Double = 0.0,
    val current_price: Double = 0.0,
    val sl: Double = 0.0,
    val tp: Double = 0.0,
    val open_time: String = ""
)

data class OpenTradesResponse(
    val success: Boolean = false,
    val trades: List<Trade> = emptyList(),
    val error: String? = null
)

// ─── History ─────────────────────────────────────────────
data class HistoryTrade(
    val ticket: Long = 0,
    val position_id: Long = 0,
    val symbol: String = "",
    val type: String = "",
    val volume: Double = 0.0,
    val profit: Double = 0.0,
    val price: Double = 0.0,
    val time: String = ""
)

data class HistoryResponse(
    val success: Boolean = false,
    val history: List<HistoryTrade> = emptyList(),
    val error: String? = null
)

// ─── Advanced Analytics ──────────────────────────────────
data class EquityPoint(
    val time: String = "",
    val balance: Double = 0.0
)

data class SymbolDist(
    val symbol: String = "",
    val trades: Int = 0
)

data class DailyProfit(
    val date: String = "",
    val profit: Double = 0.0
)

data class MonthlyProfit(
    val month: String = "",
    val profit: Double = 0.0
)

data class AdvancedAnalyticsResponse(
    val success: Boolean = false,
    val balance: Double = 0.0,
    val equity: Double = 0.0,
    val net_profit: Double = 0.0,
    val total_trades: Int = 0,
    val win_rate: Double = 0.0,
    val profit_factor: Double = 0.0,
    val max_drawdown: Double = 0.0,
    val sharpe_ratio: Double = 0.0,
    val sortino_ratio: Double = 0.0,
    val recovery_factor: Double = 0.0,
    val equity_curve: List<EquityPoint> = emptyList(),
    val symbol_distribution: List<SymbolDist> = emptyList(),
    val daily_profit: List<DailyProfit> = emptyList(),
    val monthly_profit: List<MonthlyProfit> = emptyList(),
    val total_deposits: Double = 0.0,
    val total_withdrawals: Double = 0.0,
    val error: String? = null
)

// ─── Advisory ────────────────────────────────────────────
data class Advisory(
    val id: Long = 0,
    val timestamp: String = "",
    val title: String = "",
    val text: String = "",
    val audio_url: String? = null,
    val is_signal: Boolean = false,
    val signal_symbol: String = "",
    val signal_type: String = "",
    val signal_volume: Double = 0.0,
    val signal_sl: Double = 0.0,
    val signal_tp: Double = 0.0
)

data class AdvisoryResponse(
    val success: Boolean = false,
    val advisories: List<Advisory> = emptyList(),
    val error: String? = null
)

// ─── Candles ─────────────────────────────────────────────
data class Candle(
    val time: Long = 0,
    val open: Double = 0.0,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val close: Double = 0.0,
    val volume: Int = 0
)

data class CandlesResponse(
    val success: Boolean = false,
    val symbol: String = "",
    val timeframe: String = "",
    val candles: List<Candle> = emptyList(),
    val error: String? = null
)

// ─── Symbols ─────────────────────────────────────────────
data class SymbolsResponse(
    val success: Boolean = false,
    val symbols: List<String> = emptyList()
)
