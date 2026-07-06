package com.jetro.mt5dashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetro.mt5dashboard.api.Trade
import com.jetro.mt5dashboard.ui.theme.*
import kotlin.math.abs

@Composable
fun TradeRow(trade: Trade, modifier: Modifier = Modifier) {
    val isBuy = trade.type.uppercase() == "BUY"
    val typeColor = if (isBuy) BuyColor else SellColor
    val profitColor = if (trade.profit >= 0) ProfitGreen else LossRed
    val profitSign = if (trade.profit >= 0) "+" else ""

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianCard.copy(alpha = 0.7f))
            .border(
                width = 1.dp,
                color = typeColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            // BUY/SELL badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(typeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = trade.type.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = typeColor,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Symbol + Volume
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trade.symbol,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Vol: ${trade.volume}  |  #${trade.ticket}",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            // Price info
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${profitSign}${"%.2f".format(trade.profit)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = profitColor
                )
                Text(
                    text = "@ ${"%.5f".format(trade.open_price)}",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun HistoryTradeRow(
    symbol: String,
    type: String,
    profit: Double,
    volume: Double,
    price: Double,
    time: String,
    ticket: Long,
    modifier: Modifier = Modifier
) {
    val isBuy = type.uppercase() == "BUY"
    val typeColor = if (isBuy) BuyColor else SellColor
    val profitColor = if (profit >= 0) ProfitGreen else LossRed
    val profitSign = if (profit >= 0) "+" else ""

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianCard.copy(alpha = 0.5f))
            .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            // Type dot
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(8.dp)
                    .background(typeColor, RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = symbol,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = type.uppercase(),
                        fontSize = 9.sp,
                        color = typeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "$time  ·  Vol: $volume",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$profitSign${"%.2f".format(profit)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = profitColor
                )
                Text(
                    text = "@ ${"%.5f".format(price)}",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
fun ConnectionBadge(isConnected: Boolean, lastUpdated: String) {
    val dotColor = if (isConnected) AccentEmerald else LossRed
    val label = if (isConnected) "LIVE" else "OFFLINE"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, RoundedCornerShape(4.dp))
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = dotColor,
            letterSpacing = 0.8.sp
        )
        if (isConnected && lastUpdated.isNotBlank()) {
            Text(
                text = "· $lastUpdated",
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}
