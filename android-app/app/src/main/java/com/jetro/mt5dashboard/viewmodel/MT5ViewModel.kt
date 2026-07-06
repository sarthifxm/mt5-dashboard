package com.jetro.mt5dashboard.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jetro.mt5dashboard.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MT5ViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("mt5_prefs", Context.MODE_PRIVATE)

    // ─── Server URL ───────────────────────────────────────
    private val _serverUrl = MutableStateFlow(
        prefs.getString("server_url", "http://192.168.1.100:5000") ?: "http://192.168.1.100:5000"
    )
    val serverUrl: StateFlow<String> = _serverUrl

    // ─── Connection ───────────────────────────────────────
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _lastUpdated = MutableStateFlow("")
    val lastUpdated: StateFlow<String> = _lastUpdated

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // ─── Account ─────────────────────────────────────────
    private val _account = MutableStateFlow<AccountResponse?>(null)
    val account: StateFlow<AccountResponse?> = _account

    // ─── Open Trades ──────────────────────────────────────
    private val _openTrades = MutableStateFlow<List<Trade>>(emptyList())
    val openTrades: StateFlow<List<Trade>> = _openTrades

    // ─── History ──────────────────────────────────────────
    private val _history = MutableStateFlow<List<HistoryTrade>>(emptyList())
    val history: StateFlow<List<HistoryTrade>> = _history

    // ─── Analytics ────────────────────────────────────────
    private val _analytics = MutableStateFlow<AdvancedAnalyticsResponse?>(null)
    val analytics: StateFlow<AdvancedAnalyticsResponse?> = _analytics

    // ─── Advisory ────────────────────────────────────────
    private val _advisories = MutableStateFlow<List<Advisory>>(emptyList())
    val advisories: StateFlow<List<Advisory>> = _advisories

    // ─── Active refresh tab ───────────────────────────────
    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab

    private var refreshJob: Job? = null

    init {
        startAutoRefresh()
    }

    fun setActiveTab(tab: Int) {
        _activeTab.value = tab
    }

    fun updateServerUrl(url: String) {
        val cleanUrl = url.trim()
        prefs.edit().putString("server_url", cleanUrl).apply()
        _serverUrl.value = cleanUrl
        viewModelScope.launch { fetchAll() }
    }

    fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                fetchAll()
                delay(5000L)
            }
        }
    }

    fun manualRefresh() {
        viewModelScope.launch { fetchAll() }
    }

    private suspend fun fetchAll() {
        val url = _serverUrl.value
        if (url.isBlank()) return

        withContext(Dispatchers.IO) {
            // Always fetch account (lightweight heartbeat)
            try {
                val api = RetrofitClient.getService(url)
                val acc = api.getAccount()
                if (acc.success) {
                    _account.value = acc
                    _isConnected.value = true
                    _errorMessage.value = null
                    _lastUpdated.value = java.text.SimpleDateFormat(
                        "HH:mm:ss", java.util.Locale.getDefault()
                    ).format(java.util.Date())
                } else {
                    _isConnected.value = false
                    _errorMessage.value = acc.error ?: "MT5 terminal offline"
                }
            } catch (e: Exception) {
                _isConnected.value = false
                _errorMessage.value = "Cannot reach server: ${e.localizedMessage}"
                return@withContext
            }

            // Fetch open trades
            try {
                val api = RetrofitClient.getService(url)
                val res = api.getOpenTrades()
                if (res.success) _openTrades.value = res.trades
            } catch (_: Exception) {}

            // Fetch analytics (heavier - do every refresh cycle)
            try {
                val api = RetrofitClient.getService(url)
                val res = api.getAdvancedAnalytics()
                if (res.success) _analytics.value = res
            } catch (_: Exception) {}

            // Fetch advisory
            try {
                val api = RetrofitClient.getService(url)
                val res = api.getAdvisory()
                if (res.success) _advisories.value = res.advisories
            } catch (_: Exception) {}

            // Fetch history (less frequent – only refresh on demand or tab open)
            try {
                val api = RetrofitClient.getService(url)
                val res = api.getHistory()
                if (res.success) _history.value = res.history
            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}
