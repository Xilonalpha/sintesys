package com.synthesis.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.synthesis.domain.model.ExoplanetCandidate
import com.synthesis.ui.theme.*
import com.synthesis.viewmodel.ExoplanetUiState
import com.synthesis.viewmodel.ExoplanetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExoplanetDashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExoplanetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Recharts Dashboard, 1: Anomaly Inspector

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("POPULATION & ANOMALY DASHBOARD", color = AccentCyan, style = MaterialTheme.typography.titleMedium)
                        Text("Interactive Recharts (Period vs. Mass/Radius)", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadCandidates() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(padding)
        ) {
            when (val state = uiState) {
                is ExoplanetUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AccentCyan)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Aggregating planetary populations...", color = TextSecondary)
                        }
                    }
                }
                is ExoplanetUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = AccentRed)
                    }
                }
                is ExoplanetUiState.Success -> {
                    val allCandidates = state.displayedCandidates
                    val anomalies = remember(allCandidates) { allCandidates.filter { it.anomalyType != null } }

                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = DarkSurface,
                        contentColor = AccentCyan
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("RECHARTS SCATTER PLOT") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("ANOMALIES (${anomalies.size})") }
                        )
                    }

                    if (selectedTab == 0) {
                        // Recharts Interactive WebView Dashboard
                        RechartsWebViewDashboard(
                            candidates = allCandidates,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Anomaly Inspector List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Surface(
                                    color = AccentRed.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = AccentRed)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            "Identified astrophysical outliers: planets in the Neptunian Desert (photoevaporation anomaly), Ultra-Short Period (USP) magma worlds, or temperate long-period candidates.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }

                            items(anomalies) { candidate ->
                                AnomalyCandidateItem(candidate)
                            }

                            if (anomalies.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(160.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No population outliers detected in current sample.", color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RechartsWebViewDashboard(
    candidates: List<ExoplanetCandidate>,
    modifier: Modifier = Modifier
) {
    val htmlContent = remember(candidates) {
        RechartsDashboardHtmlBuilder.buildHtml(candidates)
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                setBackgroundColor(0xFF0A0E17.toInt())
                webViewClient = WebViewClient()
                loadDataWithBaseURL(
                    "https://exoplanetarchive.ipac.caltech.edu/",
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                "https://exoplanetarchive.ipac.caltech.edu/",
                htmlContent,
                "text/html",
                "UTF-8",
                null
            )
        }
    )
}

@Composable
fun AnomalyCandidateItem(candidate: ExoplanetCandidate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(candidate.toiId, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Text(candidate.ticId, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                }

                val anomalyLabel = candidate.anomalyType ?: "ANOMALY"
                val badgeColor = when {
                    anomalyLabel.contains("Habitable", ignoreCase = true) || anomalyLabel.contains("Temperate", ignoreCase = true) -> AccentGreen
                    anomalyLabel.contains("Desert", ignoreCase = true) || anomalyLabel.contains("Super-Puff", ignoreCase = true) -> AccentRed
                    anomalyLabel.contains("Jupiter", ignoreCase = true) || anomalyLabel.contains("USP", ignoreCase = true) || anomalyLabel.contains("Period", ignoreCase = true) -> AccentAmber
                    else -> AccentCyan
                }

                Surface(
                    color = badgeColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        anomalyLabel,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricColumn("PERIOD", "${String.format("%.2f", candidate.orbitalPeriodDays ?: 0.0)} d", AccentAmber)
                MetricColumn("MASS (est)", "${String.format("%.1f", candidate.planetMassEarth ?: 0.0)} M⊕", AccentCyan)
                MetricColumn("RADIUS", "${String.format("%.2f", candidate.planetRadiusEarth ?: 0.0)} R⊕", AccentGreen)
                MetricColumn("TEMP", "${candidate.equilibriumTempK?.toInt() ?: 0} K", TextPrimary)
            }

            if (candidate.scoreReasoning.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = TextSecondary.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(6.dp))
                candidate.scoreReasoning.take(2).forEach { reason ->
                    Text("• $reason", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
