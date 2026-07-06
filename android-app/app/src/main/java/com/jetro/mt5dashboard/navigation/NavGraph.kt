package com.jetro.mt5dashboard.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavRoute(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Dashboard  : NavRoute("dashboard",   "Dashboard", Icons.Default.Dashboard)
    object OpenTrades : NavRoute("open_trades", "Positions", Icons.Default.AccountBalance)
    object History    : NavRoute("history",     "History",   Icons.Default.History)
    object Analytics  : NavRoute("analytics",   "Analytics", Icons.Default.BarChart)
    object Advisory   : NavRoute("advisory",    "Advisory",  Icons.Default.Campaign)
    object Settings   : NavRoute("settings",    "Settings",  Icons.Default.Settings)
}

val bottomNavItems = listOf(
    NavRoute.Dashboard,
    NavRoute.OpenTrades,
    NavRoute.History,
    NavRoute.Analytics,
    NavRoute.Advisory
)
