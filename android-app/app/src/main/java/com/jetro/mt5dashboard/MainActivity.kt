package com.jetro.mt5dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.jetro.mt5dashboard.navigation.*
import com.jetro.mt5dashboard.ui.screens.*
import com.jetro.mt5dashboard.ui.theme.*
import com.jetro.mt5dashboard.viewmodel.MT5ViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JetroMT5Theme {
                MT5DashboardApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MT5DashboardApp() {
    val navController = rememberNavController()
    val viewModel: MT5ViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBg,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    // Logo / Brand
                    Row(
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(listOf(AccentCyan, AccentViolet))
                                )
                        )
                        Column {
                            Text(
                                "JETRO AI",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                "MT5 LIVE DASHBOARD",
                                fontSize = 8.sp,
                                color = TextMuted,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Settings icon
                    IconButton(
                        onClick = {
                            navController.navigate(NavRoute.Settings.route) {
                                launchSingleTop = true
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (currentRoute == NavRoute.Settings.route) AccentCyan else TextMuted
                        )
                    }
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface)
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                AccentCyan.copy(alpha = 0.15f),
                                AccentViolet.copy(alpha = 0.1f),
                                AccentCyan.copy(alpha = 0.15f)
                            )
                        ),
                        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
                    )
                    .navigationBarsPadding()
            ) {
                NavigationBar(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = AccentCyan,
                    tonalElevation = 0.dp,
                ) {
                    bottomNavItems.forEach { navItem ->
                        val isSelected = navBackStackEntry?.destination?.hierarchy?.any {
                            it.route == navItem.route
                        } == true

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    navItem.icon,
                                    contentDescription = navItem.label,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            label = {
                                Text(
                                    navItem.label,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(navItem.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor    = AccentCyan,
                                selectedTextColor    = AccentCyan,
                                unselectedIconColor  = TextMuted,
                                unselectedTextColor  = TextMuted,
                                indicatorColor       = AccentCyan.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.Dashboard.route,
            modifier = Modifier.padding(paddingValues),
            enterTransition = {
                fadeIn(animationSpec = androidx.compose.animation.core.tween(200))
            },
            exitTransition = {
                fadeOut(animationSpec = androidx.compose.animation.core.tween(150))
            }
        ) {
            composable(NavRoute.Dashboard.route)  { DashboardScreen(viewModel) }
            composable(NavRoute.OpenTrades.route) { OpenTradesScreen(viewModel) }
            composable(NavRoute.History.route)    { HistoryScreen(viewModel) }
            composable(NavRoute.Analytics.route)  { AnalyticsScreen(viewModel) }
            composable(NavRoute.Advisory.route)   { AdvisoryScreen(viewModel) }
            composable(NavRoute.Settings.route)   { SettingsScreen(viewModel) }
        }
    }
}
