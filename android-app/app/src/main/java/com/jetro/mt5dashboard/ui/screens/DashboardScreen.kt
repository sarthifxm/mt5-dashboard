package com.jetro.mt5dashboard.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetro.mt5dashboard.ui.components.*
import com.jetro.mt5dashboard.ui.theme.*
import com.jetro.mt5dashboard.viewmodel.MT5ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DashboardScreen(viewModel: MT5ViewModel) {
    val account        by viewModel.account.collectAsStateWithLifecycle()
    val analytics      by viewModel.analytics.collectAsStateWithLifecycle()
    val openTrades     by viewModel.openTrades.collectAsStateWithLifecycle()
    val isConnected    by viewModel.isConnected.collectAsStateWithLifecycle()
    val lastUpdated    by viewModel.lastUpdated.collectAsStateWithLifecycle()
    val errorMessage   by viewModel.errorMessage.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── Header ──────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "JETRO AI",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = account?.name?.takeIf { it.isNotBlank() }
                        ?: "Live MT5 Dashboard",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            ConnectionBadge(isConnected = isConnected, lastUpdated = lastUpdated)
        }

        // ── Error Banner ─────────────────────────────────
        if (!isConnected && errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(LossRed.copy(alpha = 0.12f))
                    .border(1.dp, LossRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = LossRed, modifier = Modifier.size(16.dp))
                    Text(text = errorMessage ?: "", fontSize = 11.sp, color = LossRed)
                }
            }
        }

        // ── Account Banner ───────────────────────────────
        account?.let { acc ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                AccentCyan.copy(alpha = 0.15f),
                                AccentViolet.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(AccentCyan.copy(alpha = 0.4f), AccentViolet.copy(alpha = 0.2f))
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("EQUITY", fontSize = 9.sp, color = AccentCyan.copy(alpha = 0.7f), letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${"%.2f".format(acc.equity)} ${acc.currency}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("BALANCE", fontSize = 9.sp, color = TextMuted, letterSpacing = 1.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${"%.2f".format(acc.balance)} ${acc.currency}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val floatColor = if (acc.profit >= 0) ProfitGreen else LossRed
                        val floatSign = if (acc.profit >= 0) "+" else ""
                        Text(
                            text = "Floating P/L: ${floatSign}${"%.2f".format(acc.profit)}",
                            fontSize = 12.sp,
                            color = floatColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Margin: ${"%.2f".format(acc.margin)}",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // ── Metric Cards Grid ────────────────────────────
        analytics?.let { a ->
            val netProfitSign = if (a.net_profit >= 0) "+" else ""
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        label = "Net Profit",
                        value = "$netProfitSign${"%.2f".format(a.net_profit)}",
                        accentColor = if (a.net_profit >= 0) AccentEmerald else LossRed,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "Win Rate",
                        value = "${"%.1f".format(a.win_rate)}%",
                        subLabel = "${a.total_trades} trades",
                        accentColor = AccentViolet,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        label = "Profit Factor",
                        value = "${"%.2f".format(a.profit_factor)}",
                        accentColor = AccentAmber,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "Max Drawdown",
                        value = "${"%.2f".format(a.max_drawdown)}%",
                        accentColor = LossRed,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        label = "Sharpe Ratio",
                        value = "${"%.2f".format(a.sharpe_ratio)}",
                        accentColor = AccentCyan,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        label = "Recovery Factor",
                        value = "${"%.2f".format(a.recovery_factor)}",
                        accentColor = AccentEmerald,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } ?: run {
            // Loading skeleton
            if (!isConnected) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(3) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            repeat(2) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(80.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(ObsidianCard.copy(alpha = 0.4f))
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Open Positions Summary ────────────────────────
        if (openTrades.isNotEmpty()) {
            SectionHeader(title = "Open Positions", count = openTrades.size, accentColor = AccentCyan)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                openTrades.take(3).forEach { trade ->
                    TradeRow(trade = trade)
                }
                if (openTrades.size > 3) {
                    Text(
                        text = "+ ${openTrades.size - 3} more positions  →",
                        fontSize = 11.sp,
                        color = AccentCyan,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // bottom nav clearance
    }
}

@Composable
fun SectionHeader(title: String, count: Int = -1, accentColor: Color = AccentCyan) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .background(accentColor, RoundedCornerShape(2.dp))
        )
        Text(
            text = title.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = accentColor,
            letterSpacing = 1.sp
        )
        if (count >= 0) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(text = "$count", fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}
