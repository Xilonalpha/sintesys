package com.synthesis.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

data class ArExoplanetTarget(
    val id: String,
    val name: String,
    val hostStar: String,
    val constellation: String,
    val raDeg: Double,
    val decDeg: Double,
    val distanceLightYears: Double,
    val planetType: String,
    val isHabitableZone: Boolean,
    val transitDurationHours: Double,
    val discoveryMethod: String,
    // Dynamically calculated horizontal coords
    val altitudeDeg: Double = 0.0,
    val azimuthDeg: Double = 0.0,
    val isVisibleAboveHorizon: Boolean = false,
    val screenXFraction: Float = 0.5f,
    val screenYFraction: Float = 0.5f,
    val isInsideCameraFov: Boolean = false,
    val deltaAzDeg: Double = 0.0,
    val deltaAltDeg: Double = 0.0
)

data class DeviceSkyOrientation(
    val compassAzimuthDeg: Double = 0.0,
    val altitudePitchDeg: Double = 30.0,
    val rollDeg: Double = 0.0,
    val sensorActive: Boolean = false,
    val observerLatitude: Double = 44.4268, // Default Bucharest / Mid-latitude
    val observerLongitude: Double = 26.1025
)

@Singleton
class ArSkyTrackerService @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)

    private val _deviceOrientation = MutableStateFlow(DeviceSkyOrientation())
    val deviceOrientation: StateFlow<DeviceSkyOrientation> = _deviceOrientation.asStateFlow()

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    fun startTracking() {
        rotationSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopTracking() {
        sensorManager?.unregisterListener(this)
    }

    fun setManualOrientation(azimuth: Double, altitude: Double) {
        _deviceOrientation.value = _deviceOrientation.value.copy(
            compassAzimuthDeg = (azimuth + 360.0) % 360.0,
            altitudePitchDeg = altitude.coerceIn(-90.0, 90.0),
            sensorActive = false
        )
    }

    fun setObserverCoordinates(lat: Double, lon: Double) {
        _deviceOrientation.value = _deviceOrientation.value.copy(
            observerLatitude = lat,
            observerLongitude = lon
        )
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            val azimuthRad = orientationAngles[0]
            val pitchRad = orientationAngles[1]
            val rollRad = orientationAngles[2]

            var azimuthDeg = Math.toDegrees(azimuthRad.toDouble())
            if (azimuthDeg < 0) azimuthDeg += 360.0

            // Pitch is tilt towards sky (negative pitch in Android = pointing up)
            val altitudePitchDeg = -Math.toDegrees(pitchRad.toDouble())
            val rollDeg = Math.toDegrees(rollRad.toDouble())

            _deviceOrientation.value = _deviceOrientation.value.copy(
                compassAzimuthDeg = azimuthDeg,
                altitudePitchDeg = altitudePitchDeg,
                rollDeg = rollDeg,
                sensorActive = true
            )
        } else if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
            val az = event.values[0].toDouble()
            val pitch = -event.values[1].toDouble()
            _deviceOrientation.value = _deviceOrientation.value.copy(
                compassAzimuthDeg = az,
                altitudePitchDeg = pitch,
                sensorActive = true
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun getFamousTargets(): List<ArExoplanetTarget> = Companion.getFamousTargets()

    fun computeHorizontalCoordinates(
        raDeg: Double,
        decDeg: Double,
        latDeg: Double,
        lonDeg: Double,
        epochMillis: Long = System.currentTimeMillis()
    ): Pair<Double, Double> = Companion.computeHorizontalCoordinates(raDeg, decDeg, latDeg, lonDeg, epochMillis)

    fun computeProjectedTargets(
        device: DeviceSkyOrientation,
        cameraFovHorizontalDeg: Double = 60.0,
        cameraFovVerticalDeg: Double = 45.0
    ): List<ArExoplanetTarget> = Companion.computeProjectedTargets(device, cameraFovHorizontalDeg, cameraFovVerticalDeg)

    companion object {
        fun getFamousTargets(): List<ArExoplanetTarget> {
            return listOf(
                ArExoplanetTarget(
                    id = "trappist1",
                    name = "TRAPPIST-1 (e, f, g)",
                    hostStar = "2MASS J23062928-0502285",
                    constellation = "Aquarius",
                    raDeg = 346.622,
                    decDeg = -5.041,
                    distanceLightYears = 40.7,
                    planetType = "7 Terrestrial Worlds (3 Habitable)",
                    isHabitableZone = true,
                    transitDurationHours = 0.68,
                    discoveryMethod = "Transit Photometry (Spitzer / TRAPPIST)"
                ),
                ArExoplanetTarget(
                    id = "proxima_b",
                    name = "Proxima Centauri b",
                    hostStar = "Alpha Centauri C",
                    constellation = "Centaurus",
                    raDeg = 217.429,
                    decDeg = -62.679,
                    distanceLightYears = 4.24,
                    planetType = "Terrestrial (Closest Exoplanet)",
                    isHabitableZone = true,
                    transitDurationHours = 1.15,
                    discoveryMethod = "Radial Velocity (ESO HARPS)"
                ),
                ArExoplanetTarget(
                    id = "kepler452b",
                    name = "Kepler-452b",
                    hostStar = "Kepler-452 (G2V)",
                    constellation = "Cygnus",
                    raDeg = 296.007,
                    decDeg = 44.277,
                    distanceLightYears = 1402.0,
                    planetType = "Super-Earth (Earth's Cousin)",
                    isHabitableZone = true,
                    transitDurationHours = 10.5,
                    discoveryMethod = "Transit Photometry (Kepler)"
                ),
                ArExoplanetTarget(
                    id = "dimidium",
                    name = "51 Pegasi b (Dimidium)",
                    hostStar = "Helvetios (51 Peg)",
                    constellation = "Pegasus",
                    raDeg = 344.367,
                    decDeg = 20.769,
                    distanceLightYears = 50.45,
                    planetType = "Hot Jupiter (First Exoplanet Around Sun-like Star)",
                    isHabitableZone = false,
                    transitDurationHours = 2.4,
                    discoveryMethod = "Radial Velocity (Mayor & Queloz 1995)"
                ),
                ArExoplanetTarget(
                    id = "lhs1140b",
                    name = "LHS 1140 b",
                    hostStar = "LHS 1140 (M4.5V)",
                    constellation = "Cetus",
                    raDeg = 11.248,
                    decDeg = -15.272,
                    distanceLightYears = 48.8,
                    planetType = "Sub-Neptune / Water Ocean World",
                    isHabitableZone = true,
                    transitDurationHours = 2.1,
                    discoveryMethod = "Transit & JWST Transmission"
                ),
                ArExoplanetTarget(
                    id = "toi700d",
                    name = "TOI-700 d & e",
                    hostStar = "TOI-700 (M2V)",
                    constellation = "Dorado",
                    raDeg = 97.098,
                    decDeg = -65.578,
                    distanceLightYears = 101.4,
                    planetType = "Earth-size Habitable Planet",
                    isHabitableZone = true,
                    transitDurationHours = 2.8,
                    discoveryMethod = "Transit Photometry (NASA TESS)"
                ),
                ArExoplanetTarget(
                    id = "wasp12b",
                    name = "WASP-12 b",
                    hostStar = "WASP-12",
                    constellation = "Auriga",
                    raDeg = 97.636,
                    decDeg = 29.672,
                    distanceLightYears = 1410.0,
                    planetType = "Ultra-Hot Tidal Deformed Jupiter",
                    isHabitableZone = false,
                    transitDurationHours = 2.9,
                    discoveryMethod = "Transit (WASP Survey)"
                ),
                ArExoplanetTarget(
                    id = "k2_18b",
                    name = "K2-18 b",
                    hostStar = "K2-18 (M2.5V)",
                    constellation = "Leo",
                    raDeg = 172.564,
                    decDeg = 7.588,
                    distanceLightYears = 124.3,
                    planetType = "Hycean Ocean World Candidate",
                    isHabitableZone = true,
                    transitDurationHours = 2.6,
                    discoveryMethod = "Transit & JWST NIRISS (CH4+CO2)"
                ),
                ArExoplanetTarget(
                    id = "ogle_rogue",
                    name = "OGLE-2016-BLG-1928",
                    hostStar = "Free-Floating (No Host Star)",
                    constellation = "Sagittarius (Galactic Bulge)",
                    raDeg = 270.458,
                    decDeg = -28.125,
                    distanceLightYears = 14700.0,
                    planetType = "Terrestrial Rogue Planet (FFP)",
                    isHabitableZone = false,
                    transitDurationHours = 0.69,
                    discoveryMethod = "Gravitational Microlensing"
                )
            )
        }

        fun computeHorizontalCoordinates(
            raDeg: Double,
            decDeg: Double,
            latDeg: Double,
            lonDeg: Double,
            epochMillis: Long = System.currentTimeMillis()
        ): Pair<Double, Double> {
            val jd = (epochMillis / 86400000.0) + 2440587.5
            val d = jd - 2451545.0

            var gmst = 280.46061837 + 360.98564736629 * d
            gmst = (gmst % 360.0 + 360.0) % 360.0

            var lstDeg = gmst + lonDeg
            lstDeg = (lstDeg % 360.0 + 360.0) % 360.0

            var haDeg = lstDeg - raDeg
            haDeg = (haDeg % 360.0 + 360.0) % 360.0

            val haRad = Math.toRadians(haDeg)
            val decRad = Math.toRadians(decDeg)
            val latRad = Math.toRadians(latDeg)

            val sinAlt = sin(decRad) * sin(latRad) + cos(decRad) * cos(latRad) * cos(haRad)
            val altRad = asin(sinAlt.coerceIn(-1.0, 1.0))
            val altDeg = Math.toDegrees(altRad)

            val cosAz = (sin(decRad) - sin(latRad) * sinAlt) / (cos(latRad) * cos(altRad)).coerceAtLeast(1e-6)
            var azRad = acos(cosAz.coerceIn(-1.0, 1.0))
            var azDeg = Math.toDegrees(azRad)

            if (sin(haRad) > 0) {
                azDeg = 360.0 - azDeg
            }

            return Pair(altDeg, azDeg)
        }

        fun computeProjectedTargets(
            device: DeviceSkyOrientation,
            cameraFovHorizontalDeg: Double = 60.0,
            cameraFovVerticalDeg: Double = 45.0
        ): List<ArExoplanetTarget> {
            return getFamousTargets().map { target ->
                val (alt, az) = computeHorizontalCoordinates(
                    target.raDeg,
                    target.decDeg,
                    device.observerLatitude,
                    device.observerLongitude
                )

                var deltaAz = az - device.compassAzimuthDeg
                while (deltaAz > 180.0) deltaAz -= 360.0
                while (deltaAz < -180.0) deltaAz += 360.0

                val deltaAlt = alt - device.altitudePitchDeg

                val halfFovH = cameraFovHorizontalDeg / 2.0
                val halfFovV = cameraFovVerticalDeg / 2.0

                val screenX = 0.5f + (deltaAz / halfFovH).toFloat() * 0.5f
                val screenY = 0.5f - (deltaAlt / halfFovV).toFloat() * 0.5f

                val isInside = deltaAz in -halfFovH..halfFovH && deltaAlt in -halfFovV..halfFovV && alt > -5.0

                target.copy(
                    altitudeDeg = (alt * 10.0).roundToLong() / 10.0,
                    azimuthDeg = (az * 10.0).roundToLong() / 10.0,
                    isVisibleAboveHorizon = alt > 0.0,
                    screenXFraction = screenX.coerceIn(0.05f, 0.95f),
                    screenYFraction = screenY.coerceIn(0.05f, 0.95f),
                    isInsideCameraFov = isInside,
                    deltaAzDeg = (deltaAz * 10.0).roundToLong() / 10.0,
                    deltaAltDeg = (deltaAlt * 10.0).roundToLong() / 10.0
                )
            }
        }
    }
}
