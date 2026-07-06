package com.jetro.mt5dashboard.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetro.mt5dashboard.ui.components.*
import com.jetro.mt5dashboard.ui.theme.*
import com.jetro.mt5dashboard.viewmodel.MT5ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AnalyticsScreen(viewModel: MT5ViewModel) {
    val analytics   by viewModel.analytics.collectAsStateWithLifecycle()
    val account     by viewModel.account.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Header
        Text(
            "ANALYTICS",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp,
            color = AccentEmerald
        )

        if (analytics == null) {
            EmptyState(
                message = if (isConnected) "Loading analytics..." else "Connecting...",
                isLoading = true
            )
            return@Column
        }

        val a = analytics!!

        // ── Equity Curve ─────────────────────────────────
        GlassCard(accentColor = AccentCyan) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Equity Growth Curve",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        "${a.equity_curve.size} points",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
                EquityLineChart(
                    points = a.equity_curve,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                // Min / Max labels
                if (a.equity_curve.isNotEmpty()) {
                    val minB = a.equity_curve.minOf { it.balance }
                    val maxB = a.equity_curve.maxOf { it.balance }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Low: ${"%.2f".format(minB)}", fontSize = 10.sp, color = LossRed)
                        Text("High: ${"%.2f".format(maxB)}", fontSize = 10.sp, color = ProfitGreen)
                    }
                }
            }
        }

        // ── Key Metrics Grid ─────────────────────────────
        GlassCard(accentColor = AccentViolet) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Performance Metrics", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnalyticStat("Win Rate", "${"%.1f".format(a.win_rate)}%", AccentViolet, Modifier.weight(1f))
                    AnalyticStat("Profit Factor", "${"%.2f".format(a.profit_factor)}", AccentAmber, Modifier.weight(1f))
                    AnalyticStat("Sharpe", "${"%.2f".format(a.sharpe_ratio)}", AccentCyan, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnalyticStat("Sortino", "${"%.2f".format(a.sortino_ratio)}", AccentEmerald, Modifier.weight(1f))
                    AnalyticStat("Max DD", "${"%.2f".format(a.max_drawdown)}%", LossRed, Modifier.weight(1f))
                    AnalyticStat("Recovery", "${"%.2f".format(a.recovery_factor)}", AccentBlue, Modifier.weight(1f))
                }
            }
        }

        // ── Daily P&L Chart ──────────────────────────────
        if (a.daily_profit.isNotEmpty()) {
            GlassCard(accentColor = AccentAmber) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val recentDaily = a.daily_profit.takeLast(30)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Daily P&L (Last 30 days)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        val netDaily = recentDaily.sumOf { it.profit }
                        Text(
                            "${if (netDaily >= 0) "+" else ""}${"%.2f".format(netDaily)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (netDaily >= 0) ProfitGreen else LossRed
                        )
                    }
                    DailyBarChart(
                        labels = recentDaily.map { it.date.takeLast(5) },
                        values = recentDaily.map { it.profit },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }

        // ── Symbol Distribution ──────────────────────────
        if (a.symbol_distribution.isNotEmpty()) {
            GlassCard(accentColor = AccentEmerald) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Symbol Distribution", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    val total = a.symbol_distribution.sumOf { it.trades }.toFloat()
                    val colors = listOf(AccentCyan, AccentViolet, AccentEmerald, AccentAmber, AccentRose, AccentBlue)
                    a.symbol_distribution
                        .sortedByDescending { it.trades }
                        .take(8)
                        .forEachIndexed { idx, sym ->
                            val pct = if (total > 0) sym.trades / total else 0f
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(sym.symbol, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                                    Text("${sym.trades} trades (${(pct * 100).toInt()}%)", fontSize = 10.sp, color = TextMuted)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(ObsidianBorder)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(pct)
                                            .fillMaxHeight()
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        colors[idx % colors.size],
                                                        colors[idx % colors.size].copy(alpha = 0.5f)
                                                    )
                                                )
                                            )
                                    )
                                }
                            }
                        }
                }
            }
        }

        // ── Account Summary ───────────────────────────────
        GlassCard(accentColor = AccentCyan) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Account Summary", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                SummaryRow("Total Deposits", "+${"%.2f".format(a.total_deposits)}", ProfitGreen)
                SummaryRow("Total Withdrawals", "-${"%.2f".format(a.total_withdrawals)}", LossRed)
                SummaryRow("Net Profit", "${if (a.net_profit >= 0) "+" else ""}${"%.2f".format(a.net_profit)}", if (a.net_profit >= 0) ProfitGreen else LossRed)
                Divider(color = ObsidianBorder, thickness = 0.5.dp)
                SummaryRow("Total Trades", "${a.total_trades}", AccentCyan)
                account?.let { acc ->
                    SummaryRow("Account", "#${acc.login} — ${acc.name}", TextSecondary)
                    SummaryRow("Server", acc.server, TextMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun GlassCard(
    accentColor: androidx.compose.ui.graphics.Color = AccentCyan,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianCard.copy(alpha = 0.8f))
            .border(
                1.dp,
                Brush.linearGradient(listOf(accentColor.copy(alpha = 0.3f), ObsidianBorder.copy(alpha = 0.5f))),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun AnalyticStat(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, fontSize = 9.sp, color = TextMuted, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = TextMuted)
        Text(value, fontSize = 12.sp, color = valueColor, fontWeight = FontWeight.SemiBold)
    }
}
