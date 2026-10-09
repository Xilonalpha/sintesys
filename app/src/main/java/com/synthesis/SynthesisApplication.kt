package com.synthesis

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.synthesis.service.worker.AstronomicalSyncManager
import com.synthesis.service.worker.TessSurveyWorkManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SynthesisApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var astronomicalSyncManager: AstronomicalSyncManager

    @Inject
    lateinit var tessSurveyWorkManager: TessSurveyWorkManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Automatically schedule periodic background celestial and exoplanet sync
        astronomicalSyncManager.schedulePeriodicCelestialSync(this)
        // Automatically schedule periodic background TESS archive transit survey scout (every 6 hours)
        tessSurveyWorkManager.schedulePeriodicSurvey(this, repeatHours = 6)
    }
}
