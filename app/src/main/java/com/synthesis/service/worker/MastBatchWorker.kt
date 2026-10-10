package com.synthesis.service.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.synthesis.MainActivity
import com.synthesis.service.MastBatchProcessingEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Enterprise WorkManager background worker running queued MAST batch dataset analysis
 * even when the app is closed or running in background.
 */
@HiltWorker
class MastBatchWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val batchEngine: MastBatchProcessingEngine
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            Log.d(TAG, "Starting MAST Batch Processing Worker in background...")
            ensureNotificationChannelExists()

            // If queue is empty, load the Habitable Zone campaign as background scout
            if (batchEngine.jobs.value.none { it.status == com.synthesis.domain.model.exoplanet.MastBatchJobStatus.QUEUED }) {
                val campaigns = batchEngine.getAvailableCampaigns()
                if (campaigns.isNotEmpty()) {
                    batchEngine.enqueueCampaign(campaigns.first())
                }
            }

            batchEngine.startProcessing()

            // Monitor execution until queue finishes or timeout
            var previousCompleted = 0
            while (batchEngine.isRunning.value) {
                delay(2000)
                val summary = batchEngine.summary.value
                if (summary.completedCount != previousCompleted) {
                    previousCompleted = summary.completedCount
                    postProgressNotification(summary.completedCount, summary.totalJobs)
                }
            }

            val finalSummary = batchEngine.summary.value
            prefs.edit()
                .putLong(KEY_LAST_RUN_TIMESTAMP, System.currentTimeMillis())
                .putString(KEY_LAST_RUN_STATUS, "SUCCESS (${finalSummary.completedCount} procesate, ${finalSummary.candidatesDiscoveredCount} candidați)")
                .putInt(KEY_DISCOVERIES_COUNT, finalSummary.candidatesDiscoveredCount)
                .apply()

            if (finalSummary.candidatesDiscoveredCount > 0) {
                postDiscoveryNotification(finalSummary.candidatesDiscoveredCount)
            }

            Result.success()
        } catch (e: CancellationException) {
            Log.d(TAG, "MAST Batch Worker cancelled.")
            batchEngine.pauseProcessing()
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "MAST Batch Worker failed: ${e.message}", e)
            prefs.edit()
                .putString(KEY_LAST_RUN_STATUS, "FAILED: ${e.localizedMessage}")
                .apply()
            Result.failure()
        }
    }

    private fun postProgressNotification(completed: Int, total: Int) {
        try {
            val intent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                appContext,
                202,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentTitle("🛰️ Procesare Batch MAST STScI")
                .setContentText("Progres: $completed / $total sisteme stelare analizate...")
                .setProgress(total, completed, false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID_PROGRESS, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun postDiscoveryNotification(count: Int) {
        try {
            val intent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                appContext,
                203,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("🪐 Descoperire Nouă Candidat Planetar!")
                .setContentText("Procesarea batch MAST a identificat $count candidați planetari de mare încredere!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID_DISCOVERY, notification)
        } catch (_: SecurityException) {
        }
    }

    private fun ensureNotificationChannelExists() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MAST Batch Processing",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificări de fundal pentru procesarea loturilor mari de date MAST"
            }
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val TAG = "MastBatchWorker"
        const val PREFS_NAME = "synthesis_mast_batch_prefs"
        const val KEY_LAST_RUN_TIMESTAMP = "last_run_timestamp"
        const val KEY_LAST_RUN_STATUS = "last_run_status"
        const val KEY_DISCOVERIES_COUNT = "discoveries_count"

        private const val CHANNEL_ID = "channel_mast_batch_processing"
        private const val NOTIFICATION_ID_PROGRESS = 5101
        private const val NOTIFICATION_ID_DISCOVERY = 5102
    }
}
