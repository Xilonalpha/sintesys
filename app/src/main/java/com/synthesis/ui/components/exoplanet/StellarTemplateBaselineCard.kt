package com.synthesis.ui.components.exoplanet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synthesis.domain.model.exoplanet.StellarLightCurveTemplate
import com.synthesis.domain.model.exoplanet.StellarTemplateCategory
import com.synthesis.domain.model.exoplanet.TemplateMatchScore
import com.synthesis.ui.theme.*

private val DarkSurfaceVariant = Color(0xFF161F30)

/**
 * Professional astronomical Jetpack Compose card implementing the Library of Pre-Defined
 * Stellar Light Curve Templates (planetary transits, eclipsing binaries, stellar flares, etc.).
 * Provides visual baselines and statistical Bayesian / Chi-squared fits for space agency validation.
 */
@Composable
fun StellarTemplateBaselineCard(
    templates: List<StellarLightCurveTemplate>,
    selectedTemplate: StellarLightCurveTemplate?,
    matchScores: List<TemplateMatchScore>,
    isOverlayActive: Boolean,
    onSelectTemplate: (String) -> Unit,
    onToggleOverlay: () -> Unit,
    onRunTemplateFit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFullEvaluation by remember { mutableStateOf(true) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 1. Header with Title & Overlay Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = AccentCyan,
                            modifier = Modifier.size(8.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "BIBLIOTECĂ ȘABLOANE ASTROFIZICE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )
                    }
                    Text(
                        "Linii de bază teoretice (NASA / ESA) pentru validarea tranzitelor FITS",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }

                // Overlay Active Toggle Pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isOverlayActive) AccentCyan.copy(alpha = 0.15f) else DarkBorder.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, if (isOverlayActive) AccentCyan else DarkBorder),
                    modifier = Modifier.clickable { onToggleOverlay() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isOverlayActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = if (isOverlayActive) AccentCyan else TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isOverlayActive) "Overlay Activ" else "Overlay Oprit",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverlayActive) AccentCyan else TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Horizontal Scrollable Template Selector Chips
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                templates.forEach { tmpl ->
                    val isSelected = selectedTemplate?.id == tmpl.id
                    val chipColor = parseHexColor(tmpl.badgeColorHex)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) chipColor.copy(alpha = 0.2f) else DarkSurfaceVariant,
                        border = BorderStroke(1.dp, if (isSelected) chipColor else DarkBorder),
                        modifier = Modifier.clickable { onSelectTemplate(tmpl.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = getTemplateIcon(tmpl.id),
                                fontSize = 12.sp
                            )
                            Column {
                                Text(
                                    text = tmpl.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) chipColor else TextPrimary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = tmpl.astrophysicalClass,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) chipColor.copy(alpha = 0.8f) else TextSecondary,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Selected Template Details & Space Agency Verification Criteria
            if (selectedTemplate != null) {
                val tmpl = selectedTemplate
                val tmplColor = parseHexColor(tmpl.badgeColorHex)

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceVariant,
                    border = BorderStroke(1.dp, tmplColor.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    getTemplateIcon(tmpl.id),
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    tmpl.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = tmplColor
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = tmplColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    tmpl.category.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tmplColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 9.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            tmpl.physicsDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Astrophysical Parameters Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TemplateParamItem(
                                label = "Perioadă Tipică",
                                value = tmpl.typicalPeriod,
                                modifier = Modifier.weight(1f)
                            )
                            TemplateParamItem(
                                label = "Adâncime / Flux",
                                value = tmpl.typicalDepthPpm,
                                modifier = Modifier.weight(1f)
                            )
                            TemplateParamItem(
                                label = "Formulă Model",
                                value = tmpl.formulaDescription,
                                modifier = Modifier.weight(1f),
                                isMonospace = true
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Space Agency Diagnostic Checklist
                        Text(
                            "CRITERII DE VALIDARE MANUALĂ (NASA TFOP / ESA):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        tmpl.diagnosticVerificationFeatures.forEach { feature ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AccentGreen,
                                    modifier = Modifier
                                        .size(12.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = feature,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // False Positive Warning Box
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkBackground,
                            border = BorderStroke(1.dp, if (tmpl.category == StellarTemplateCategory.PLANETARY_TRANSIT) DarkBorder else AccentAmber.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    if (tmpl.category == StellarTemplateCategory.PLANETARY_TRANSIT) Icons.Default.Info else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (tmpl.category == StellarTemplateCategory.PLANETARY_TRANSIT) AccentCyan else AccentAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        "Discriminator Fals-Pozitiv:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (tmpl.category == StellarTemplateCategory.PLANETARY_TRANSIT) AccentCyan else AccentAmber,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        tmpl.falsePositiveRisk,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Action Bar: Automated Template Fit Button
            Button(
                onClick = onRunTemplateFit,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.QueryStats, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Rulează Potrivire Statistică Bayesiană (Fit χ²)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // 5. Template Fit Ranking Breakdown (when computed)
            if (matchScores.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "CLASAMENT POTRIVIRE STATISTICĂ (BAYESIAN & REDUCED χ²):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                    Text(
                        "${matchScores.size} Scenarii Evaluate",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 9.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                matchScores.take(4).forEachIndexed { idx, score ->
                    val tmpl = score.template
                    val tmplColor = parseHexColor(tmpl.badgeColorHex)
                    val isTopMatch = idx == 0

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isTopMatch) tmplColor.copy(alpha = 0.12f) else DarkSurfaceVariant,
                        border = BorderStroke(1.dp, if (isTopMatch) tmplColor else DarkBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { onSelectTemplate(tmpl.id) }
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        if (isTopMatch) "★ #${idx + 1}" else "#${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTopMatch) tmplColor else TextSecondary,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        tmpl.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${String.format(java.util.Locale.US, "%.1f", score.confidencePercent)}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (score.confidencePercent > 60.0) AccentGreen else AccentCyan,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "(χ²_red = ${String.format(java.util.Locale.US, "%.2f", score.reducedChiSquared)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Confidence progress bar
                            LinearProgressIndicator(
                                progress = { (score.confidencePercent / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = if (isTopMatch) tmplColor else AccentCyan,
                                trackColor = DarkBorder
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Verdict: ${score.morphologyVerdict} — ${score.diagnosticNotes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 9.sp,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateParamItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isMonospace: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = DarkBackground,
        border = BorderStroke(1.dp, DarkBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontSize = 8.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 9.sp,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                maxLines = 1
            )
        }
    }
}

private fun getTemplateIcon(templateId: String): String {
    return when (templateId) {
        "planetary_transit_mandel_agol" -> "🪐"
        "eclipsing_binary_detached_ea" -> "⚯"
        "eclipsing_binary_contact_ew" -> "☍"
        "grazing_eclipsing_binary_v_shape" -> "⋁"
        "stellar_flare_davenport" -> "⚡"
        "starspot_rotational_bydra" -> "☀️"
        "pulsating_rr_lyrae" -> "〰️"
        "gravitational_microlensing_paczynski" -> "🔍"
        else -> "✨"
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val longVal = clean.toLong(16)
        if (clean.length == 6) Color(0xFF000000 or longVal) else Color(longVal)
    } catch (e: Exception) {
        AccentCyan
    }
}
