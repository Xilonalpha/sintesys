package com.synthesis.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.synthesis.data.local.entity.SynthesizedResearchEntity
import com.synthesis.ui.theme.*
import com.synthesis.viewmodel.SynthesizedResearchViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SynthesizedResearchScreen(
    onNavigateBack: () -> Unit,
    viewModel: SynthesizedResearchViewModel = hiltViewModel()
) {
    val researchList by viewModel.filteredResearch.collectAsState()
    val allResearch by viewModel.allResearch.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val bookmarkedOnly by viewModel.bookmarkedOnly.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<SynthesizedResearchEntity?>(null) }
    var selectedEntityDetails by remember { mutableStateOf<SynthesizedResearchEntity?>(null) }
    val context = LocalContext.current

    val categories = listOf("EXOPLANET", "SPECTROSCOPY", "MICROLENSING", "ASTEROSEISMOLOGY", "TIDAL_OCEAN")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "ARHIVĂ CERCETĂRI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = AccentCyan.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "ROOM DB",
                                    color = AccentCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "${allResearch.size} rapoarte sintetizate offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_research_button")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Adaugă sinteză", tint = AccentGreen)
                    }
                    IconButton(
                        onClick = {
                            viewModel.seedInitialResearch()
                            Toast.makeText(context, "Sinteze demonstrative regenerate!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("refresh_seed_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Regenerează demo", tint = AccentCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = AccentCyan,
                contentColor = Color.Black,
                icon = { Icon(Icons.Default.NoteAdd, contentDescription = null) },
                text = { Text("SINTEZĂ NOUĂ", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_add_research")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(padding)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Caută după țintă, titlu sau rezumat...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AccentCyan) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_field")
            )

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bookmarked only chip
                FilterChip(
                    selected = bookmarkedOnly,
                    onClick = { viewModel.onToggleBookmarkFilter() },
                    label = { Text("Favorite ⭐") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentAmber.copy(alpha = 0.25f),
                        selectedLabelColor = AccentAmber,
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    ),
                    border = BorderStroke(1.dp, if (bookmarkedOnly) AccentAmber else DarkBorder)
                )

                // All chip
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { viewModel.onCategorySelected(null) },
                    label = { Text("Toate") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentCyan.copy(alpha = 0.25f),
                        selectedLabelColor = AccentCyan,
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    ),
                    border = BorderStroke(1.dp, if (selectedCategory == null) AccentCyan else DarkBorder)
                )

                categories.forEach { category ->
                    val isSelected = selectedCategory.equals(category, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onCategorySelected(category) },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentCyan.copy(alpha = 0.25f),
                            selectedLabelColor = AccentCyan,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        ),
                        border = BorderStroke(1.dp, if (isSelected) AccentCyan else DarkBorder)
                    )
                }
            }

            // Results count and Database status bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${researchList.size} rezultate găsite",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AccentGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Persistență Locală Activă",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentGreen
                    )
                }
            }

            // Content List
            if (researchList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Nicio cercetare salvată conform filtrelor",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Poți reseta filtrele sau poți adăuga o nouă sinteză astronomică.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                viewModel.onSearchQueryChanged("")
                                viewModel.onCategorySelected(null)
                                if (bookmarkedOnly) viewModel.onToggleBookmarkFilter()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)
                        ) {
                            Text("Resetează Filtrele", color = AccentCyan)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    items(researchList, key = { it.id }) { item ->
                        SynthesizedResearchCard(
                            entity = item,
                            onToggleBookmark = { viewModel.toggleBookmark(item) },
                            onDelete = { itemToDelete = item },
                            onClick = { selectedEntityDetails = item }
                        )
                    }
                }
            }
        }
    }

    // Detail Modal Dialog
    selectedEntityDetails?.let { entity ->
        SynthesizedDetailDialog(
            entity = entity,
            onDismiss = { selectedEntityDetails = null }
        )
    }

    // Add New Synthesis Dialog
    if (showAddDialog) {
        AddSynthesizedResearchDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, target, category, summary, dataJson, conf, sources, notes ->
                viewModel.saveSynthesizedResearch(
                    title = title,
                    targetName = target,
                    researchCategory = category,
                    summary = summary,
                    synthesizedDataJson = dataJson,
                    confidenceScore = conf,
                    sourceCount = sources,
                    observerNotes = notes
                )
                showAddDialog = false
                Toast.makeText(context, "Sinteză salvată în Room DB!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { entity ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            containerColor = DarkSurface,
            title = { Text("Șterge Raport", color = TextPrimary) },
            text = { Text("Sigur doriți să ștergeți sinteza '${entity.title}' din baza de date?", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteResearch(entity.id)
                        itemToDelete = null
                        Toast.makeText(context, "Sinteză ștearsă din Room", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Șterge", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Anulează", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun SynthesizedResearchCard(
    entity: SynthesizedResearchEntity,
    onToggleBookmark: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    val formattedDate = remember(entity.timestamp) { dateFormat.format(Date(entity.timestamp)) }
    var expandedJson by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    val categoryColor = when (entity.researchCategory.uppercase()) {
        "SPECTROSCOPY" -> AccentMagenta
        "EXOPLANET" -> AccentCyan
        "MICROLENSING" -> AccentAmber
        "ASTEROSEISMOLOGY" -> AccentGreen
        "TIDAL_OCEAN" -> Color(0xFF38BDF8)
        else -> AccentPurple
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("research_card_${entity.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (entity.isBookmarked) AccentAmber.copy(alpha = 0.5f) else DarkBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Category Badge + Target + Bookmark
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = categoryColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = entity.researchCategory,
                            color = categoryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = entity.targetName,
                        color = AccentCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleBookmark, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (entity.isBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Bookmark",
                            tint = if (entity.isBookmarked) AccentAmber else TextSecondary
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = TextSecondary.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = entity.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Summary
            Text(
                text = entity.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 20.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Metrics Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ÎNCREDERE", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${(entity.confidenceScore * 100).toInt()}%",
                        color = if (entity.confidenceScore >= 0.9f) AccentGreen else AccentAmber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text("SURSE CORELATE", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${entity.sourceCount} Telescoape/Baze",
                        color = AccentCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("SALVAT LA", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text(
                        text = formattedDate,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Notes if any
            if (entity.observerNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AccentCyan.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = entity.observerNotes,
                        fontSize = 11.sp,
                        color = AccentCyan,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Actions: Expand JSON or Copy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { expandedJson = !expandedJson },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (expandedJson) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (expandedJson) "Ascunde date JSON" else "Examină date JSON", fontSize = 12.sp, color = AccentCyan)
                }

                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString("${entity.title} (${entity.targetName})\n\n${entity.summary}"))
                        Toast.makeText(context, "Rezumat copiat în clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copiază", tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }

            AnimatedVisibility(visible = expandedJson) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = entity.synthesizedDataJson,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = AccentGreen,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SynthesizedDetailDialog(
    entity: SynthesizedResearchEntity,
    onDismiss: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Column {
                Text(
                    text = entity.targetName,
                    color = AccentCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = entity.title,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = entity.summary,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                if (entity.observerNotes.isNotBlank()) {
                    Surface(
                        color = AccentAmber.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Notă observator: ${entity.observerNotes}",
                            color = AccentAmber,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Text("Date Tehnice Sintetizate:", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = entity.synthesizedDataJson,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = AccentGreen
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    clipboard.setText(AnnotatedString("SINTEZĂ CERCETARE SYNTHESIS ASTRONOMY\nȚintă: ${entity.targetName}\nTitlu: ${entity.title}\nCategorie: ${entity.researchCategory}\nÎncredere: ${(entity.confidenceScore * 100).toInt()}%\nRezumat: ${entity.summary}\nDate: ${entity.synthesizedDataJson}"))
                    Toast.makeText(context, "Raport complet copiat!", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("Copiază Raport", color = AccentCyan)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Închide", color = TextSecondary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSynthesizedResearchDialog(
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        target: String,
        category: String,
        summary: String,
        dataJson: String,
        confidence: Float,
        sourceCount: Int,
        notes: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("EXOPLANET") }
    var summary by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var confidence by remember { mutableStateOf(0.92f) }

    val categories = listOf("EXOPLANET", "SPECTROSCOPY", "MICROLENSING", "ASTEROSEISMOLOGY", "TIDAL_OCEAN")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("Adaugă Sinteză în Room DB", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Nume Țintă (ex. TOI-1233 b)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titlu Raport") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selection row
                Text("Categorie:", fontSize = 12.sp, color = TextSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Rezumat Sinteză Științifică") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note Observator (opțional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Scor Încredere: ${(confidence * 100).toInt()}%", fontSize = 12.sp, color = AccentCyan)
                Slider(
                    value = confidence,
                    onValueChange = { confidence = it },
                    valueRange = 0.5f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentCyan)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && target.isNotBlank()) {
                        val mockJson = """{"target": "$target", "category": "$category", "confidence": $confidence, "sources_count": 3, "status": "SYNTHESIZED_LOCAL"}"""
                        onSave(title, target, category, summary, mockJson, confidence, 3, notes)
                    }
                },
                enabled = title.isNotBlank() && target.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Color.Black)
            ) {
                Text("Salvează în Room", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anulează", color = TextSecondary)
            }
        }
    )
}
