package com.synthesis.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synthesis.domain.model.Investigation
import com.synthesis.domain.model.InvestigationStatus
import com.synthesis.ui.theme.*
import com.synthesis.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onInvestigationClick: (String) -> Unit,
    onNewInvestigation: () -> Unit,
    onViewExoplanets: () -> Unit,
    onViewXilonDiscovery: () -> Unit = {},
    onViewDeepHunt: () -> Unit = {},
    onViewAdvancedLab: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val investigations by viewModel.investigations.collectAsState()
    val systemStatus by viewModel.systemStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(AccentCyan, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "SYNTHESIS",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = AccentCyan.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "v2026.2",
                                        color = AccentCyan,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                "Astrophysics & Cosmological Intelligence",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onViewAdvancedLab,
                        modifier = Modifier.testTag("top_lab_button")
                    ) {
                        Icon(Icons.Default.Biotech, contentDescription = "Discovery Lab", tint = AccentGreen)
                    }
                    IconButton(
                        onClick = onViewDeepHunt,
                        modifier = Modifier.testTag("top_deep_hunt_button")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = "Deep Hunt", tint = AccentAmber)
                    }
                    IconButton(
                        onClick = onViewXilonDiscovery,
                        modifier = Modifier.testTag("top_xilon_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Xilon Discovery", tint = AccentCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                contentColor = AccentCyan
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* already on home */ },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Acasă", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AccentCyan,
                        indicatorColor = AccentCyan.copy(alpha = 0.2f)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onViewAdvancedLab,
                    icon = { Icon(Icons.Default.Biotech, contentDescription = "Lab", tint = AccentGreen) },
                    label = { Text("Lab & Cosmo", style = MaterialTheme.typography.labelSmall, color = TextSecondary) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onViewDeepHunt,
                    icon = { Icon(Icons.Default.Bolt, contentDescription = "Deep Hunt", tint = AccentAmber) },
                    label = { Text("Deep Hunt", style = MaterialTheme.typography.labelSmall, color = TextSecondary) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onViewXilonDiscovery,
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Xilon", tint = AccentCyan) },
                    label = { Text("Xilon AI", style = MaterialTheme.typography.labelSmall, color = TextSecondary) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onViewExoplanets,
                    icon = { Icon(Icons.Default.Public, contentDescription = "Exoplanete", tint = TextSecondary) },
                    label = { Text("Exoplanete", style = MaterialTheme.typography.labelSmall, color = TextSecondary) }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewInvestigation,
                containerColor = AccentCyan,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Investigation", modifier = Modifier.size(24.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DarkBackground)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // 1. SLEEK HORIZONTAL SYSTEM STATUS ROW
            item {
                ModernSystemStatusBar(status = systemStatus)
            }

            // 2. LUXURY 2026/27 HERO CARD (Clean, balanced, modern)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onViewAdvancedLab() },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(AccentCyan, AccentMagenta, AccentAmber))),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🌌", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "LABORATORUL ASTROFIZIC",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                color = AccentCyan.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "9 MOTOARE NOI",
                                    color = AccentCyan,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Simulare Big Bang & inflație FLRW, fizica relativistă a găurilor negre Kerr, sincronizare Smart Scope și spectroscopie de transmisie JWST.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onViewAdvancedLab,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "DESCHIDE TOATE CELE 16 MODULE",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // 3. CORE MODULES 2x2 GRID
            item {
                Text(
                    "HUB-URI DE CERCETARE & INSTRUMENTE",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModuleQuickCard(
                        modifier = Modifier.weight(1f),
                        icon = "🌌",
                        title = "Big Bang & Găuri Negre",
                        subtitle = "FLRW, CMB, Sgr A* & Kerr",
                        accentColor = AccentMagenta,
                        onClick = onViewAdvancedLab
                    )
                    ModuleQuickCard(
                        modifier = Modifier.weight(1f),
                        icon = "📡",
                        title = "Smart Scope & GoTo",
                        subtitle = "Seestar, DWARF, ASCOM",
                        accentColor = AccentGreen,
                        onClick = onViewAdvancedLab
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModuleQuickCard(
                        modifier = Modifier.weight(1f),
                        icon = "⚡",
                        title = "Deep Hunt (TESS)",
                        subtitle = "Mono-tranzite & Lumi Noi",
                        accentColor = AccentAmber,
                        onClick = onViewDeepHunt
                    )
                    ModuleQuickCard(
                        modifier = Modifier.weight(1f),
                        icon = "✨",
                        title = "Xilon AI",
                        subtitle = "Descoperire Autonomă",
                        accentColor = AccentCyan,
                        onClick = onViewXilonDiscovery
                    )
                }
            }

            // 4. ACTIVE INVESTIGATIONS LIST / ELEGANT EMPTY STATE
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "INVESTIGAȚII ȘI ANALIZE ACTIVE",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    if (investigations.isNotEmpty()) {
                        Text(
                            "${investigations.size} Active",
                            color = AccentCyan,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            if (investigations.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AccentCyan.copy(alpha = 0.1f),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Explore,
                                        contentDescription = null,
                                        tint = AccentCyan,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Nicio investigație în desfășurare",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Apasă pe butonul de mai jos pentru a iniția căutarea automată de exoplanete sau simularea unei curbe fotometrice.",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = onNewInvestigation,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, AccentCyan),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Inițiază o Investigație Nouă", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            } else {
                items(investigations) { investigation ->
                    InvestigationCard(investigation = investigation, onClick = { onInvestigationClick(investigation.id) })
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun ModernSystemStatusBar(status: HomeViewModel.SystemStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPill(label = "SURSE DATE", value = status.dataSources.toString(), color = AccentCyan)
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF1E293B)))
            StatusPill(label = "CONN. LIVE", value = status.liveConnections.toString(), color = AccentGreen)
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF1E293B)))
            StatusPill(label = "ANALIZE", value = status.activeAnalyses.toString(), color = AccentAmber)
            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF1E293B)))
            StatusPill(label = "ANOMALII", value = status.anomalies.toString(), color = AccentRed)
        }
    }
}

@Composable
fun StatusPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
    }
}

@Composable
fun ModuleQuickCard(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(104.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(icon, fontSize = 20.sp)
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun InvestigationCard(investigation: Investigation, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        when (investigation.status) {
                            InvestigationStatus.ACTIVE -> AccentGreen
                            InvestigationStatus.COMPLETED -> AccentCyan
                            InvestigationStatus.FAILED -> AccentRed
                            else -> AccentAmber
                        }
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    investigation.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    investigation.query,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Surse: ${investigation.sourcesConsulted} • Anomalii: ${investigation.anomaliesFound} • Scor: ${(investigation.confidenceScore * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentCyan
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}
