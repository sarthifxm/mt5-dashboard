package com.jetro.mt5dashboard.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetro.mt5dashboard.ui.theme.*
import com.jetro.mt5dashboard.viewmodel.MT5ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(viewModel: MT5ViewModel) {
    val serverUrl   by viewModel.serverUrl.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val errorMsg    by viewModel.errorMessage.collectAsStateWithLifecycle()
    val account     by viewModel.account.collectAsStateWithLifecycle()

    var urlInput by remember(serverUrl) { mutableStateOf(serverUrl) }
    var saved by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

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
            "SETTINGS",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp,
            color = TextSecondary
        )

        // ── Connection Status Card ────────────────────────
        GlassCard(accentColor = if (isConnected) AccentEmerald else LossRed) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (isConnected) AccentEmerald else LossRed,
                                RoundedCornerShape(5.dp)
                            )
                    )
                    Text(
                        if (isConnected) "CONNECTED" else "OFFLINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isConnected) AccentEmerald else LossRed,
                        letterSpacing = 0.8.sp
                    )
                }
                account?.let { acc ->
                    SummaryRow("Account", "#${acc.login}", AccentCyan)
                    SummaryRow("Name", acc.name, TextSecondary)
                    SummaryRow("Server", acc.server, TextMuted)
                    SummaryRow("Currency", acc.currency, TextMuted)
                }
                if (!isConnected && errorMsg != null) {
                    Text(
                        errorMsg!!,
                        fontSize = 11.sp,
                        color = LossRed.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // ── Server URL Card ──────────────────────────────
        GlassCard(accentColor = AccentCyan) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Server Connection", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(
                    "Enter the IP address and port of your PC running the MT5 server. Both devices must be on the same WiFi network.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it; saved = false },
                    label = { Text("Server URL", fontSize = 12.sp) },
                    placeholder = { Text("http://192.168.1.100:5000", fontSize = 12.sp, color = TextMuted) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.updateServerUrl(urlInput)
                            saved = true
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedLabelColor = AccentCyan,
                        unfocusedLabelColor = TextMuted,
                        cursorColor = AccentCyan,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.updateServerUrl(urlInput)
                        saved = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Connect", fontWeight = FontWeight.Bold, color = ObsidianBg)
                }

                if (saved && isConnected) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(14.dp))
                        Text("Connected successfully!", fontSize = 11.sp, color = AccentEmerald)
                    }
                }
            }
        }

        // ── Quick Setup Guide ─────────────────────────────
        GlassCard(accentColor = AccentAmber) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                    Text("Quick Setup Guide", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                StepItem("1", "Start server.py on your Windows PC")
                StepItem("2", "Find your PC's local IP: open CMD → type 'ipconfig'")
                StepItem("3", "Enter: http://YOUR_IP:5000 above")
                StepItem("4", "Make sure your phone is on the same WiFi")
                Divider(color = ObsidianBorder, thickness = 0.5.dp)
                Text(
                    "📡 For remote access over internet, use a tunnel like ngrok or Cloudflare Tunnel and enter the HTTPS URL instead.",
                    fontSize = 10.sp,
                    color = TextMuted,
                    lineHeight = 15.sp
                )
            }
        }

        // ── Refresh Button ────────────────────────────────
        OutlinedButton(
            onClick = { viewModel.manualRefresh() },
            border = BorderStroke(1.dp, AccentViolet.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = AccentViolet, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Force Refresh Now", color = AccentViolet, fontWeight = FontWeight.SemiBold)
        }

        // ── App Info ─────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text("JETRO AI · MT5 Dashboard", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                Text("Version 1.0.0 · LAKSHMIFX & SARTHIFXM Capital", fontSize = 9.sp, color = ObsidianDivider)
                Text("Auto-refresh: every 5 seconds", fontSize = 9.sp, color = ObsidianDivider)
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun StepItem(number: String, text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(20.dp)
                .background(AccentAmber.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
        ) {
            Text(number, fontSize = 10.sp, color = AccentAmber, fontWeight = FontWeight.ExtraBold)
        }
        Text(text, fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp, modifier = Modifier.padding(top = 2.dp))
    }
}
