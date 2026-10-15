package com.synthesis.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.synthesis.ui.screens.*
import com.synthesis.ui.screens.xilon.XilonDiscoveryScreen
import com.synthesis.ui.screens.terra.TerraFrontierScreen
import com.synthesis.ui.components.exoplanet.Tic167418703RechartsView
import com.synthesis.ui.components.exoplanet.Tic141913165RechartsView
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.synthesis.ui.theme.DarkBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SynthesisNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onInvestigationClick = { id -> navController.navigate("investigation/$id") },
                onNewInvestigation = { navController.navigate("new_investigation") },
                onViewExoplanets = { navController.navigate("exoplanets") },
                onViewXilonDiscovery = { navController.navigate("xilon_discovery") },
                onViewDeepHunt = { navController.navigate("deep_hunt") },
                onViewAdvancedLab = { tab ->
                    if (tab != null) {
                        navController.navigate("advanced_lab?tab=$tab")
                    } else {
                        navController.navigate("advanced_lab")
                    }
                },
                onViewTerraFrontier = { navController.navigate("terra_frontier") },
                onViewResearchArchive = { navController.navigate("research_archive") }
            )
        }
        composable("new_investigation") {
            NewInvestigationScreen(
                onNavigateBack = { navController.popBackStack() },
                onInvestigationCreated = { id ->
                    navController.navigate("investigation/$id") { popUpTo("home") }
                }
            )
        }
        composable(
            "investigation/{investigationId}",
            arguments = listOf(navArgument("investigationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val investigationId = backStackEntry.arguments?.getString("investigationId") ?: ""
            InvestigationDetailScreen(
                investigationId = investigationId,
                onNavigateBack = { navController.popBackStack() },
                onViewEvidenceGraph = { navController.navigate("evidence_graph/$investigationId") },
                onViewAnomalies = { navController.navigate("anomalies/$investigationId") },
                onViewHypotheses = { navController.navigate("hypotheses/$investigationId") },
                onViewDiscoveries = { navController.navigate("discoveries/$investigationId") }
            )
        }
        composable(
            "evidence_graph/{investigationId}",
            arguments = listOf(navArgument("investigationId") { type = NavType.StringType })
        ) { backStackEntry ->
            EvidenceGraphScreen(
                investigationId = backStackEntry.arguments?.getString("investigationId") ?: "",
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            "anomalies/{investigationId}",
            arguments = listOf(navArgument("investigationId") { type = NavType.StringType })
        ) { backStackEntry ->
            AnomaliesScreen(
                investigationId = backStackEntry.arguments?.getString("investigationId") ?: "",
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            "hypotheses/{investigationId}",
            arguments = listOf(navArgument("investigationId") { type = NavType.StringType })
        ) { backStackEntry ->
            HypothesesScreen(
                investigationId = backStackEntry.arguments?.getString("investigationId") ?: "",
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            "discoveries/{investigationId}",
            arguments = listOf(navArgument("investigationId") { type = NavType.StringType })
        ) { backStackEntry ->
            DiscoveriesScreen(
                investigationId = backStackEntry.arguments?.getString("investigationId") ?: "",
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("exoplanets") {
            ExoplanetScreen(
                onNavigateBack = { navController.popBackStack() },
                onViewDashboard = { navController.navigate("exoplanet_dashboard") },
                onViewXilonDiscovery = { tic ->
                    if (tic != null) {
                        navController.navigate("xilon_discovery?tic=$tic")
                    } else {
                        navController.navigate("xilon_discovery")
                    }
                },
                onViewDeepHunt = { navController.navigate("deep_hunt") },
                onViewAdvancedLab = { navController.navigate("advanced_lab") },
                onViewTerraFrontier = { navController.navigate("terra_frontier") }
            )
        }
        composable("exoplanet_dashboard") {
            ExoplanetDashboardScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            "xilon_discovery?tic={tic}",
            arguments = listOf(navArgument("tic") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val tic = backStackEntry.arguments?.getString("tic")
            XilonDiscoveryScreen(
                initialTicId = tic,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("xilon_discovery") {
            XilonDiscoveryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("deep_hunt") {
            DeepHuntScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            "advanced_lab?tab={tab}",
            arguments = listOf(navArgument("tab") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val tabStr = backStackEntry.arguments?.getString("tab")
            val tab = tabStr?.let {
                try {
                    com.synthesis.viewmodel.exoplanet.AdvancedTab.valueOf(it)
                } catch (e: Exception) {
                    null
                }
            }
            AdvancedDiscoveryScreen(
                onNavigateBack = { navController.popBackStack() },
                initialTab = tab
            )
        }
        composable("advanced_lab") {
            AdvancedDiscoveryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("terra_frontier") {
            TerraFrontierScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("research_archive") {
            SynthesizedResearchScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("tic_167418703") {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "TIC 167418703 • Recharts Vetting",
                                color = androidx.compose.ui.graphics.Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Înapoi",
                                    tint = androidx.compose.ui.graphics.Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = DarkBackground
                        )
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground)
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Tic167418703RechartsView(
                            onOpenInXilonScan = { tic ->
                                navController.navigate("xilon_discovery?tic=$tic")
                            }
                        )
                    }
                }
            }
        }

        composable("tic_141913165") {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("TIC 141913165 • Raport CTOI") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Înapoi")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = DarkBackground,
                            titleContentColor = androidx.compose.ui.graphics.Color.White,
                            navigationIconContentColor = androidx.compose.ui.graphics.Color.White
                        )
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground)
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Tic141913165RechartsView(
                            onOpenInXilonScan = { tic ->
                                navController.navigate("xilon_discovery?tic=$tic")
                            }
                        )
                    }
                }
            }
        }
    }
}
