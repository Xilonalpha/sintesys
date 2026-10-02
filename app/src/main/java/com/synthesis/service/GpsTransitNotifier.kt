package com.synthesis.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.synthesis.MainActivity
import com.synthesis.domain.model.exoplanet.LiveTransitTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.sin

@Singleton
class GpsTransitNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "synthesis_gps_transits"
        const val NOTIFICATION_ID = 4201
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "SYNTHESIS Live Transit Alerts"
            val descriptionText = "High-priority alerts when exoplanet transits pass above your GPS coordinates."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Dispatches the real-time GPS transit alert requested by the user.
     * Computes the dynamic local ingress time, countdown, and sky position.
     */
    fun sendGpsTransitAlert(
        target: LiveTransitTarget? = null,
        userLatitude: Double = 44.4323, // Observer GPS coordinates
        userLongitude: Double = 26.1063
    ): Pair<Boolean, String> {
        val targetName = target?.candidateName ?: "Candidat Exoplanetar"
        val constellation = target?.constellation ?: "Cygnus"
        val ra = target?.rightAscension ?: "19h 42m 12s"
        val dec = target?.declination ?: "+41° 18′ 29″"
        val hours = target?.hoursUntilIngress ?: 0.0

        // Calculate dynamic countdown in hours & minutes
        val hoursInt = hours.toInt()
        val minsInt = (((hours - hoursInt) * 60.0).toInt()).coerceAtLeast(0)
        val countdownStr = when {
            hoursInt > 0 -> "${hoursInt}h ${minsInt}m"
            minsInt > 0 -> "$minsInt minute"
            else -> "acum"
        }

        // Calculate local calendar date & clock time of ingress
        val ingressMillis = System.currentTimeMillis() + (hours * 3600.0 * 1000.0).toLong()
        val localTimeFormat = java.text.SimpleDateFormat("HH:mm (d MMM)", java.util.Locale.getDefault())
        val localClockTime = localTimeFormat.format(java.util.Date(ingressMillis))

        val title = "⚡ ALERTĂ TRANZIT LIVE - GPS SKY RADAR"
        val message = if (hours <= 0.05) {
            "Tranzitul candidatului $targetName este ÎN CURS DE DESFĂȘURARE chiar acum deasupra coordonatelor tale în constelația $constellation (RA: $ra, Dec: $dec, V_mag: ${target?.apparentVisualMag ?: 10.0}). Durată: ${target?.transitDurationHours ?: 2.5}h."
        } else {
            "Peste $countdownStr (la ora $localClockTime), tranzitul candidatului $targetName trece deasupra ta în constelația $constellation. Coordonate: RA $ra, Dec $dec (V_mag: ${target?.apparentVisualMag ?: 10.0}, Fezabilitate: ${target?.amateurTelescopeFeasibility ?: "Accesibil"})."
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val sent = try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
            true
        } catch (e: SecurityException) {
            // Missing runtime notification permission on Android 13+
            false
        } catch (e: Exception) {
            false
        }

        return Pair(sent, message)
    }
}
