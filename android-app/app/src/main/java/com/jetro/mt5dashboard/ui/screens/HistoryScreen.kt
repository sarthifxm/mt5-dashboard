package com.jetro.mt5dashboard.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetro.mt5dashboard.ui.components.HistoryTradeRow
import com.jetro.mt5dashboard.ui.theme.*
import com.jetro.mt5dashboard.viewmodel.MT5ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HistoryScreen(viewModel: MT5ViewModel) {
    val history     by viewModel.history.collectAsStateWithLifecycle()
    val account     by viewModel.account.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()

    var filterType by remember { mutableStateOf("ALL") }

    val filteredHistory = when (filterType) {
        "BUY"  -> history.filter { it.type.uppercase() == "BUY" }
        "SELL" -> history.filter { it.type.uppercase() == "SELL" }
        "WIN"  -> history.filter { it.profit > 0 }
        "LOSS" -> history.filter { it.profit < 0 }
        else   -> history
    }

    val totalProfit = filteredHistory.sumOf { it.profit }
    val wins = filteredHistory.count { it.profit > 0 }
    val losses = filteredHistory.count { it.profit < 0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {

        // ── Header ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(ObsidianSurface, ObsidianBg)))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "TRADE HISTORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color = AccentViolet
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        val tpColor = if (totalProfit >= 0) ProfitGreen else LossRed
                        Text(
                            "${filteredHistory.size} Trades",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${if (totalProfit >= 0) "+" else ""}${"%.2f".format(totalProfit)} ${account?.currency ?: "USD"}",
                            fontSize = 13.sp,
                            color = tpColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatPill("W", "$wins", ProfitGreen)
                        StatPill("L", "$losses", LossRed)
                    }
                }

                // ── Filter Chips ────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ALL", "BUY", "SELL", "WIN", "LOSS").forEach { f ->
                        val selected = filterType == f
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (selected) AccentViolet.copy(alpha = 0.25f)
                                    else ObsidianCard.copy(alpha = 0.5f)
                                )
                                .border(
                                    1.dp,
                                    if (selected) AccentViolet.copy(alpha = 0.6f) else ObsidianBorder,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { filterType = f }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = f,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) AccentViolet else TextMuted
                            )
                        }
                    }
                }
            }
        }

        // ── History List ─────────────────────────────────
        if (filteredHistory.isEmpty()) {
            EmptyState(
                message = if (isConnected) "No trade history found" else "Connecting...",
                isLoading = !isConnected
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredHistory, key = { it.ticket }) { trade ->
                    HistoryTradeRow(
                        symbol = trade.symbol,
                        type = trade.type,
                        profit = trade.profit,
                        volume = trade.volume,
                        price = trade.price,
                        time = trade.time,
                        ticket = trade.ticket
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun StatPill(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 9.sp, color = color.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
            Text(value, fontSize = 14.sp, color = color, fontWeight = FontWeight.ExtraBold)
        }
    }
}
