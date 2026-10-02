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
                onViewAdvancedLab = { navController.navigate("advanced_lab") }
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
                onViewXilonDiscovery = { navController.navigate("xilon_discovery") },
                onViewDeepHunt = { navController.navigate("deep_hunt") },
                onViewAdvancedLab = { navController.navigate("advanced_lab") }
            )
        }
        composable("exoplanet_dashboard") {
            ExoplanetDashboardScreen(
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
        composable("advanced_lab") {
            AdvancedDiscoveryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
