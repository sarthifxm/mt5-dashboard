package com.jetro.mt5dashboard.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetro.mt5dashboard.ui.components.TradeRow
import com.jetro.mt5dashboard.ui.theme.*
import com.jetro.mt5dashboard.viewmodel.MT5ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OpenTradesScreen(viewModel: MT5ViewModel) {
    val openTrades  by viewModel.openTrades.collectAsStateWithLifecycle()
    val account     by viewModel.account.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()

    val totalPL = openTrades.sumOf { it.profit }
    val plColor = if (totalPL >= 0) ProfitGreen else LossRed
    val plSign = if (totalPL >= 0) "+" else ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {

        // ── Top Summary Bar ──────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(ObsidianSurface, ObsidianBg)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OPEN POSITIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = AccentCyan
                    )
                    IconButton(
                        onClick = { viewModel.manualRefresh() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "${openTrades.size} Active Trades",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Floating P/L: ${plSign}${"%.2f".format(totalPL)} ${account?.currency ?: "USD"}",
                            fontSize = 12.sp,
                            color = plColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // ── Trade List ───────────────────────────────────
        if (openTrades.isEmpty()) {
            EmptyState(
                message = if (isConnected) "No open positions" else "Connecting to server...",
                isLoading = !isConnected
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(openTrades, key = { it.ticket }) { trade ->
                    TradeRow(trade = trade)

                    // Expanded details below each trade
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                            .background(ObsidianSurface.copy(alpha = 0.5f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LabelValue(
                                label = "Current",
                                value = "${"%.5f".format(trade.current_price)}"
                            )
                            LabelValue(
                                label = "SL",
                                value = if (trade.sl > 0) "${"%.5f".format(trade.sl)}" else "—"
                            )
                            LabelValue(
                                label = "TP",
                                value = if (trade.tp > 0) "${"%.5f".format(trade.tp)}" else "—"
                            )
                            LabelValue(label = "Since", value = trade.open_time.take(10))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun LabelValue(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Text(text = value, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyState(message: String, isLoading: Boolean = false) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = AccentCyan,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(text = message, fontSize = 13.sp, color = TextMuted)
        }
    }
}
