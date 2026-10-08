package com.synthesis.ui.screens.billing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import com.synthesis.data.billing.SubscriptionManager
import com.synthesis.data.billing.SubscriptionPlan
import com.synthesis.ui.theme.*

@Composable
fun LockScreenPaywall(
    subscriptionManager: SubscriptionManager,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030508)) // Pure dark black space
            .testTag("lock_screen_paywall"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = AccentAmber.copy(alpha = 0.15f),
                shape = RoundedCornerShape(100.dp),
                border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "PERIOADĂ DE TEST EXPIRATĂ (60 MIN)",
                        color = AccentAmber,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "ACCESUL GRATUIT S-A ÎNCHEIAT",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                "Ai utilizat limita de 1 oră de explorare gratuită a platformei de inteligență științifică SYNTHESIS.\n\n" +
                        "Pentru a continua să folosești motoarele Deep Hunt, de-blenderul Gaia DR3 nelimitat, " +
                        "alertele de tranzit live prin GPS și exportul pentru telescoape (N.I.N.A., Stellarium, AstroImageJ), " +
                        "te rugăm să activezi un abonament.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // PLAN ANUAL (RECOMANDAT)
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, AccentGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = AccentGreen.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "CEL MAI AVANTAJOS (-37% REDUCERE)",
                                color = AccentGreen,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text("75.00 € / an", style = MaterialTheme.typography.titleLarge, color = AccentGreen, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Abonament Anual PRO", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text("Echivalentul a doar 6.25 € / lună. Include alerte live GPS de tranzit și export nelimitat de coordonate.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            isProcessing = true
                            subscriptionManager.purchaseSubscription(SubscriptionPlan.YEARLY, activity = activity) { success, msg ->
                                isProcessing = false
                                if (success) {
                                    onUnlocked()
                                } else {
                                    statusMessage = msg
                                }
                            }
                        },
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isProcessing) "SE PROCESEAZĂ..." else "ACTIVEAZĂ PLAN ANUAL (75 €)", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PLAN LUNAR
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Abonament Lunar", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("9.99 € / lună", style = MaterialTheme.typography.titleMedium, color = AccentCyan, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Acces complet lună de lună, fără angajament pe termen lung. Poți anula oricând.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            isProcessing = true
                            subscriptionManager.purchaseSubscription(SubscriptionPlan.MONTHLY, activity = activity) { success, msg ->
                                isProcessing = false
                                if (success) {
                                    onUnlocked()
                                } else {
                                    statusMessage = msg
                                }
                            }
                        },
                        enabled = !isProcessing,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCyan),
                        border = BorderStroke(1.dp, AccentCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(if (isProcessing) "SE PROCESEAZĂ..." else "ACTIVEAZĂ PLAN LUNAR (9.99 €)", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(
                    onClick = {
                        isProcessing = true
                        subscriptionManager.restorePurchases { success, msg ->
                            isProcessing = false
                            if (success) {
                                onUnlocked()
                            } else {
                                statusMessage = msg ?: "Nu s-au găsit abonamente active anterioare."
                            }
                        }
                    },
                    enabled = !isProcessing
                ) {
                    Text("Restaurează Achizițiile", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }

                TextButton(
                    onClick = {
                        subscriptionManager.resetTrialForTesting()
                        onUnlocked()
                    }
                ) {
                    Text("Demo Test: Resetează 60 Min", color = AccentAmber.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                }
            }

            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(statusMessage!!, color = AccentAmber, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
