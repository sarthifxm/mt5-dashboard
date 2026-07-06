package com.jetro.mt5dashboard.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetro.mt5dashboard.api.Advisory
import com.jetro.mt5dashboard.ui.theme.*
import com.jetro.mt5dashboard.viewmodel.MT5ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AdvisoryScreen(viewModel: MT5ViewModel) {
    val advisories  by viewModel.advisories.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {

        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(ObsidianSurface, ObsidianBg)))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(AccentAmber, RoundedCornerShape(4.dp))
                    )
                    Text(
                        "LIVE ADVISORY FEED",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = AccentAmber
                    )
                }
                Text(
                    "${advisories.size} Signals & Broadcasts",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    "LAKSHMIFX & SARTHIFXM Capital — Live Signals",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }

        if (advisories.isEmpty()) {
            EmptyState(
                message = if (isConnected) "No advisory signals yet" else "Connecting...",
                isLoading = !isConnected
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(advisories, key = { it.id }) { advisory ->
                    AdvisoryCard(advisory = advisory)
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun AdvisoryCard(advisory: Advisory) {
    val isSignal = advisory.is_signal
    val accentColor = when {
        isSignal && advisory.signal_type.uppercase() == "BUY"  -> BuyColor
        isSignal && advisory.signal_type.uppercase() == "SELL" -> SellColor
        else -> AccentAmber
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(ObsidianCard.copy(alpha = 0.95f), ObsidianSurface.copy(alpha = 0.8f))
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(listOf(accentColor.copy(alpha = 0.35f), ObsidianBorder.copy(alpha = 0.4f))),
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

            // Top row: badge + timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSignal) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.18f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                "${advisory.signal_type.uppercase()} ${advisory.signal_symbol}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            "BROADCAST",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentAmber,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
                Text(
                    advisory.timestamp.take(16),
                    fontSize = 9.sp,
                    color = TextMuted
                )
            }

            // Title
            Text(
                text = advisory.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                lineHeight = 18.sp
            )

            // Body text
            if (advisory.text.isNotBlank()) {
                Text(
                    text = advisory.text,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }

            // Signal details (SL, TP, Volume)
            if (isSignal && (advisory.signal_sl > 0 || advisory.signal_tp > 0)) {
                Divider(color = ObsidianBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (advisory.signal_sl > 0) {
                        SignalTag("SL", "${"%.5f".format(advisory.signal_sl)}", LossRed)
                    }
                    if (advisory.signal_tp > 0) {
                        SignalTag("TP", "${"%.5f".format(advisory.signal_tp)}", ProfitGreen)
                    }
                    if (advisory.signal_volume > 0) {
                        SignalTag("Vol", "${advisory.signal_volume}", AccentAmber)
                    }
                }
            }
        }
    }
}

@Composable
fun SignalTag(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 9.sp, color = color.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
        Text(value, fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}
